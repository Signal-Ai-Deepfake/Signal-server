package com.signal.domain.chat.service;

import com.signal.domain.chat.dto.response.ChatSummaryResponse;
import com.signal.domain.chat.engine.ChatEngine;
import com.signal.domain.chat.engine.ChatEngineResponse;
import com.signal.domain.chat.engine.SituationType;
import com.signal.domain.chat.entity.ChatMessage;
import com.signal.domain.chat.entity.ChatRole;
import com.signal.domain.chat.entity.ChatSession;
import com.signal.domain.chat.repository.ChatMessageRepository;
import com.signal.domain.chat.repository.ChatSessionRepository;
import com.signal.domain.report.entity.Report;
import com.signal.domain.report.entity.ReportStatus;
import com.signal.domain.report.repository.ReportRepository;
import com.signal.domain.riskassessment.entity.RiskLevel;
import com.signal.global.exception.ErrorCode;
import com.signal.global.exception.SignalException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.regex.Pattern;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class ChatService {

    private static final long ANONYMOUS_CHAT_SESSION_LIMIT = 5;
    private static final int PROGRESS_STAGE_COUNT = 6;
    private static final int PROGRESS_STAGE_WEIGHT = 16;
    private static final Pattern URL_PATTERN = Pattern.compile("https?://\\S+", Pattern.CASE_INSENSITIVE);
    private static final List<String> END_CONFIRM_KEYWORDS = List.of(
            "네", "응", "좋아", "그만할래", "종료할래", "끝낼래", "마무리", "그래");
    private static final String END_PROMPT =
            "지금까지 상황을 잘 정리해주셨어요. 상담을 마무리해도 괜찮을까요? 더 이야기하고 싶으시면 편하게 말씀해주세요.";
    private static final String END_CLOSING_MESSAGE = "상담을 마무리할게요. 언제든 다시 찾아주세요.";

    private final ChatSessionRepository chatSessionRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final ReportRepository reportRepository;
    private final ChatEngine chatEngine;

    @Transactional
    public ChatSession createSession(Long userId, String anonymousId, boolean saveConsent) {
        if (userId == null) {
            validateAnonymousUsage(anonymousId);
        }

        ChatSession session = ChatSession.builder()
                .sessionId(UUID.randomUUID().toString())
                .userId(userId)
                .anonymousId(userId == null ? anonymousId : null)
                .saveConsent(saveConsent)
                .build();
        return chatSessionRepository.save(session);
    }

    @Transactional
    public SendMessageResult sendMessage(String sessionId, Long userId, String anonymousId, String content) {
        ChatSession session = getOwnedSession(sessionId, userId, anonymousId);
        session.recordUserMessage(content);
        if (URL_PATTERN.matcher(content).find()) {
            session.markEvidenceUrlMentioned();
        }

        chatMessageRepository.save(ChatMessage.builder()
                .chatSessionId(session.getId())
                .role(ChatRole.USER)
                .content(content)
                .build());

        ChatEngineResponse engineResponse = resolveEngineResponse(session, userId, content);

        ChatMessage botMessage = chatMessageRepository.save(ChatMessage.builder()
                .chatSessionId(session.getId())
                .role(ChatRole.BOT)
                .content(engineResponse.reply())
                .build());

        return new SendMessageResult(botMessage, engineResponse, session.isSessionEnded());
    }

    private ChatEngineResponse resolveEngineResponse(ChatSession session, Long userId, String content) {
        if (session.isAwaitingEndConfirmation()) {
            boolean confirmed = END_CONFIRM_KEYWORDS.stream().anyMatch(content::contains);
            session.resolveEndConfirmation(confirmed);
            if (confirmed) {
                return new ChatEngineResponse(
                        END_CLOSING_MESSAGE, session.getLastSituationType(), List.of(), false, List.of());
            }
        }

        ChatEngineResponse engineResponse = chatEngine.respond(content);
        boolean agenciesGiven = !engineResponse.recommendedAgencies().isEmpty();
        session.recordEngineResult(engineResponse.situationType(), engineResponse.crisisDetected(), agenciesGiven);

        List<Report> reports = myReports(userId);
        boolean progressComplete = calculateProgressPercent(
                session, !reports.isEmpty(), hasFinalizedReport(reports)) >= PROGRESS_STAGE_COUNT * PROGRESS_STAGE_WEIGHT;
        if (!session.isSessionEnded() && !session.isAwaitingEndConfirmation() && progressComplete) {
            session.markAwaitingEndConfirmation();
            return new ChatEngineResponse(
                    END_PROMPT, engineResponse.situationType(), engineResponse.suggestedActions(),
                    engineResponse.crisisDetected(), engineResponse.recommendedAgencies());
        }

        return engineResponse;
    }

    public List<ChatMessage> getMessages(String sessionId, Long userId, String anonymousId) {
        ChatSession session = getOwnedSession(sessionId, userId, anonymousId);
        return chatMessageRepository.findByChatSessionIdOrderByCreatedAtAsc(session.getId());
    }

    public List<ChatSession> getMySessions(Long userId) {
        return chatSessionRepository.findByUserIdOrderByCreatedAtDesc(userId);
    }

    public ChatSession getSession(String sessionId, Long userId, String anonymousId) {
        return getOwnedSession(sessionId, userId, anonymousId);
    }

    public ChatSummaryResponse getSummary(String sessionId, Long userId, String anonymousId) {
        ChatSession session = getOwnedSession(sessionId, userId, anonymousId);
        List<Report> reports = myReports(userId);
        boolean reportFinalized = hasFinalizedReport(reports);

        int progressPercent = session.isSessionEnded()
                ? 100
                : calculateProgressPercent(session, !reports.isEmpty(), reportFinalized);

        return new ChatSummaryResponse(
                describeSituation(session),
                recommendedSteps(session, reportFinalized),
                riskLevel(session),
                riskDescription(session),
                progressPercent);
    }

    private List<Report> myReports(Long userId) {
        return userId == null ? List.of() : reportRepository.findByUserIdOrderByCreatedAtDesc(userId);
    }

    private boolean hasFinalizedReport(List<Report> reports) {
        return reports.stream().anyMatch(report -> report.getStatus() == ReportStatus.FINALIZED);
    }

    private int calculateProgressPercent(ChatSession session, boolean reportStarted, boolean reportFinalized) {
        int completed = 0;
        if (session.getLastSituationType() != null) {
            completed++;
        }
        if (session.isEmotionallyStabilized()) {
            completed++;
        }
        if (session.isEvidenceUrlMentioned()) {
            completed++;
        }
        if (reportStarted) {
            completed++;
        }
        if (reportFinalized) {
            completed++;
        }
        if (session.isAgenciesRecommended()) {
            completed++;
        }

        return Math.min(completed, PROGRESS_STAGE_COUNT) * PROGRESS_STAGE_WEIGHT;
    }

    private String describeSituation(ChatSession session) {
        SituationType situationType = session.getLastSituationType();
        if (situationType == null) {
            return "상담 시작 전";
        }
        if (situationType == SituationType.IMAGE_ABUSE) {
            return session.isLastCrisisDetected() ? "이미지 도용·유포로 인한 위기 상황" : "이미지 도용·유포 피해 상담";
        }
        return session.isLastCrisisDetected() ? "정서적 위기 상황" : "일반 상담";
    }

    private List<String> recommendedSteps(ChatSession session, boolean reportFinalized) {
        List<String> steps = new ArrayList<>();
        if (!session.isEvidenceUrlMentioned()) {
            steps.add("증거 보존");
        }
        if (!reportFinalized) {
            steps.add("플랫폼 신고");
        }
        if (!session.isAgenciesRecommended()) {
            steps.add("전문 기관 상담");
        }
        return steps;
    }

    private RiskLevel riskLevel(ChatSession session) {
        if (session.isLastCrisisDetected()) {
            return RiskLevel.HIGH;
        }
        if (session.getLastSituationType() == SituationType.IMAGE_ABUSE) {
            return RiskLevel.MEDIUM;
        }
        return RiskLevel.LOW;
    }

    private String riskDescription(ChatSession session) {
        return switch (riskLevel(session)) {
            case HIGH -> "위험 신호가 감지되어 긴급한 조치가 필요해요.";
            case MEDIUM -> "확산 여부를 확인하고 있어요.";
            case LOW -> "특별한 위험 신호는 없어요.";
        };
    }

    @Transactional
    public void deleteSession(String sessionId, Long userId, String anonymousId) {
        ChatSession session = getOwnedSession(sessionId, userId, anonymousId);
        chatMessageRepository.deleteByChatSessionId(session.getId());
        chatSessionRepository.delete(session);
    }

    private ChatSession getOwnedSession(String sessionId, Long userId, String anonymousId) {
        ChatSession session = chatSessionRepository.findBySessionId(sessionId)
                .orElseThrow(() -> new SignalException(ErrorCode.NOT_FOUND));

        if (!session.isOwnedBy(userId, anonymousId)) {
            throw new SignalException(ErrorCode.FORBIDDEN);
        }

        return session;
    }

    private void validateAnonymousUsage(String anonymousId) {
        if (!StringUtils.hasText(anonymousId)) {
            throw new SignalException(ErrorCode.ANONYMOUS_ID_REQUIRED);
        }
        if (chatSessionRepository.countByAnonymousId(anonymousId) >= ANONYMOUS_CHAT_SESSION_LIMIT) {
            throw new SignalException(ErrorCode.ANONYMOUS_CHAT_LIMIT_EXCEEDED);
        }
    }
}
