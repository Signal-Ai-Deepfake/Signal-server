package com.signal.domain.chat.service;

import com.signal.domain.chat.dto.response.ChatSummaryResponse;
import com.signal.domain.chat.dto.response.ConsultationProgressResponse;
import com.signal.domain.chat.engine.ChatEngine;
import com.signal.domain.chat.engine.ChatEngineResponse;
import com.signal.domain.chat.engine.ChatSpeaker;
import com.signal.domain.chat.engine.ChatTurn;
import com.signal.domain.chat.engine.SituationType;
import com.signal.domain.chat.guidance.AgencyCatalog;
import com.signal.domain.chat.guidance.GuidancePlanner;
import com.signal.domain.chat.guidance.GuidanceText;
import com.signal.domain.chat.entity.ChatMessage;
import com.signal.domain.chat.entity.ChatRole;
import com.signal.domain.chat.entity.ChatSession;
import com.signal.domain.chat.repository.ChatMessageRepository;
import com.signal.domain.chat.progress.ConsultationProgress;
import com.signal.domain.chat.progress.ConsultationProgressStatus;
import com.signal.domain.chat.progress.ConsultationProgressTracker;
import com.signal.domain.chat.progress.ConsultationStage;
import com.signal.domain.chat.progress.StageFacts;
import com.signal.domain.chat.progress.StageRules;
import com.signal.domain.chat.progress.StageSnapshot;
import com.signal.domain.chat.repository.ChatSessionRepository;
import com.signal.domain.report.entity.Report;
import com.signal.domain.report.entity.ReportStatus;
import com.signal.domain.report.repository.ReportRepository;
import com.signal.domain.riskassessment.entity.RiskLevel;
import com.signal.global.exception.ErrorCode;
import com.signal.global.exception.SignalException;
import com.signal.global.i18n.LanguageContext;
import com.signal.global.i18n.ResponseLanguage;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
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
    private static final int MAX_HISTORY_MESSAGES = 10;   // LLM에 넘길 이전 대화 최대 개수 (토큰 사용량 상한)
    private static final Pattern URL_PATTERN = Pattern.compile("https?://\\S+", Pattern.CASE_INSENSITIVE);

    private final ChatSessionRepository chatSessionRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final ReportRepository reportRepository;
    private final ChatEngine chatEngine;
    private final ConsultationProgressTracker progressTracker;

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
        List<ChatTurn> history = buildHistory(session.getId());

        session.recordUserMessage(content);
        if (URL_PATTERN.matcher(content).find()) {
            session.markEvidenceUrlMentioned();
        }

        chatMessageRepository.save(ChatMessage.builder()
                .chatSessionId(session.getId())
                .role(ChatRole.USER)
                .content(content)
                .build());

        ChatEngineResponse engineResponse = resolveEngineResponse(session, userId, content, history);

        ChatMessage botMessage = chatMessageRepository.save(ChatMessage.builder()
                .chatSessionId(session.getId())
                .role(ChatRole.BOT)
                .content(engineResponse.reply())
                .build());

        return new SendMessageResult(
                botMessage, engineResponse, session.isSessionEnded(),
                ConsultationProgress.from(session.getStageSnapshot()));
    }

    private List<ChatTurn> buildHistory(Long chatSessionId) {
        List<ChatMessage> messages = chatMessageRepository.findByChatSessionIdOrderByCreatedAtAsc(chatSessionId);
        int fromIndex = Math.max(0, messages.size() - MAX_HISTORY_MESSAGES);
        return messages.subList(fromIndex, messages.size()).stream()
                .map(message -> new ChatTurn(
                        message.getRole() == ChatRole.USER ? ChatSpeaker.USER : ChatSpeaker.BOT,
                        message.getContent()))
                .toList();
    }

    private ChatEngineResponse resolveEngineResponse(
            ChatSession session, Long userId, String content, List<ChatTurn> history) {
        ResponseLanguage language = LanguageContext.current();
        if (session.isAwaitingEndConfirmation()) {
            boolean confirmed = GuidanceText.isEndConfirmation(content);
            session.resolveEndConfirmation(confirmed);
            if (confirmed) {
                return new ChatEngineResponse(
                        GuidanceText.endClosing(language), session.getLastSituationType(), List.of(), false, List.of());
            }
        }

        ChatEngineResponse engineResponse = chatEngine.respond(content, history);
        boolean agenciesGiven = !engineResponse.recommendedAgencies().isEmpty();
        session.recordEngineResult(engineResponse.situationType(), engineResponse.crisisDetected(), agenciesGiven);

        ConsultationProgressStatus statusBefore = ConsultationProgress.from(session.getStageSnapshot()).status();

        List<Report> reports = myReports(userId);
        List<ChatTurn> turns = new ArrayList<>(history);
        turns.add(new ChatTurn(ChatSpeaker.USER, content));
        turns.add(new ChatTurn(ChatSpeaker.BOT, engineResponse.reply()));
        int userTurns = (int) turns.stream().filter(turn -> turn.speaker() == ChatSpeaker.USER).count();
        StageSnapshot snapshot = progressTracker.update(
                session.getStageSnapshot(), engineResponse.situationType(), turns,
                new StageFacts(
                        engineResponse.crisisDetected(),
                        session.isEverCrisisDetected(),
                        session.isEvidenceUrlMentioned(),
                        !reports.isEmpty(),
                        hasFinalizedReport(reports),
                        userTurns));

        // 서버가 이번 턴에 붙일 안내 블록을 고른다(한 턴에 하나). 블록을 내보낸 단계만 서버가 완료로 기록한다.
        Optional<ConsultationStage> delivered = GuidancePlanner.next(
                snapshot.type(), snapshot.stages(), userTurns, engineResponse.crisisDetected());
        String reply = engineResponse.reply();
        List<String> agencies = engineResponse.recommendedAgencies();
        if (delivered.isPresent()) {
            ConsultationStage stage = delivered.get();
            snapshot = StageRules.markDelivered(snapshot, stage);
            reply = reply + "\n\n" + GuidanceText.guidanceBlock(stage, snapshot.type(), language);
            if (stage == ConsultationStage.AGENCY_GUIDANCE) {
                session.markAgenciesRecommended();
                agencies = snapshot.type() == SituationType.IMAGE_ABUSE
                        ? AgencyCatalog.imageAbuse(language)
                        : AgencyCatalog.generalSupport(language);
            }
        }
        session.updateStageSnapshot(snapshot);

        // 종료 제안은 "방금 모든 단계가 끝난" 턴에 한 번만, 안내 블록 뒤에 덧붙인다(위기 메시지에는 하지 않는다).
        // 사용자가 계속하겠다고 답한 뒤에는 이미 완료 상태라 다시 묻지 않는다.
        boolean justCompleted = statusBefore != ConsultationProgressStatus.COMPLETED
                && ConsultationProgress.from(snapshot).status() == ConsultationProgressStatus.COMPLETED;
        if (justCompleted && !engineResponse.crisisDetected()
                && !session.isSessionEnded() && !session.isAwaitingEndConfirmation()) {
            session.markAwaitingEndConfirmation();
            reply = reply + "\n\n" + GuidanceText.endPrompt(language);
        }

        if (reply.equals(engineResponse.reply()) && agencies == engineResponse.recommendedAgencies()) {
            return engineResponse;
        }
        return new ChatEngineResponse(
                reply, engineResponse.situationType(), engineResponse.suggestedActions(),
                engineResponse.crisisDetected(), agencies);
    }

    public List<ChatMessage> getMessages(String sessionId, Long userId, String anonymousId) {
        ChatSession session = getOwnedSession(sessionId, userId, anonymousId);
        return chatMessageRepository.findByChatSessionIdOrderByCreatedAtAsc(session.getId());
    }

    public List<ChatSession> getMySessions(Long userId) {
        // userId가 null이면 파생 쿼리가 user_id IS NULL로 바뀌어 모든 익명 세션이 조회된다.
        if (userId == null) {
            throw new SignalException(ErrorCode.UNAUTHORIZED);
        }
        return chatSessionRepository.findByUserIdOrderByCreatedAtDesc(userId);
    }

    public ChatSession getSession(String sessionId, Long userId, String anonymousId) {
        return getOwnedSession(sessionId, userId, anonymousId);
    }

    public ChatSummaryResponse getSummary(String sessionId, Long userId, String anonymousId) {
        ChatSession session = getOwnedSession(sessionId, userId, anonymousId);
        List<Report> reports = myReports(userId);
        boolean reportFinalized = hasFinalizedReport(reports);

        ConsultationProgress progress = ConsultationProgress.from(session.getStageSnapshot());

        return new ChatSummaryResponse(
                describeSituation(session),
                recommendedSteps(session, reportFinalized),
                riskLevel(session),
                riskDescription(session),
                progress.progressPercent(),
                ConsultationProgressResponse.from(progress));
    }

    private List<Report> myReports(Long userId) {
        return userId == null ? List.of() : reportRepository.findByUserIdOrderByCreatedAtDesc(userId);
    }

    private boolean hasFinalizedReport(List<Report> reports) {
        return reports.stream().anyMatch(report -> report.getStatus() == ReportStatus.FINALIZED);
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
