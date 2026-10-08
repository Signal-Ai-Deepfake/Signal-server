package com.signal.domain.chat.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.signal.domain.chat.dto.response.ChatSummaryResponse;
import com.signal.domain.chat.engine.ChatEngine;
import com.signal.domain.chat.engine.ChatEngineResponse;
import com.signal.domain.chat.engine.ChatSpeaker;
import com.signal.domain.chat.engine.ChatTurn;
import com.signal.domain.chat.engine.SituationType;
import com.signal.domain.chat.entity.ChatMessage;
import com.signal.domain.chat.entity.ChatRole;
import com.signal.domain.chat.entity.ChatSession;
import com.signal.domain.chat.progress.ConsultationProgressStatus;
import com.signal.domain.chat.progress.ConsultationProgressTracker;
import com.signal.domain.chat.progress.ConsultationStage;
import com.signal.domain.chat.progress.StageAnalyzer;
import com.signal.domain.chat.progress.StageStatus;
import com.signal.domain.chat.repository.ChatMessageRepository;
import com.signal.domain.chat.repository.ChatSessionRepository;
import com.signal.domain.report.entity.Report;
import com.signal.domain.report.repository.ReportRepository;
import com.signal.domain.riskassessment.entity.RiskLevel;
import com.signal.global.exception.ErrorCode;
import com.signal.global.exception.SignalException;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ChatServiceTest {

    @Mock
    private ChatSessionRepository chatSessionRepository;

    @Mock
    private ChatMessageRepository chatMessageRepository;

    @Mock
    private ReportRepository reportRepository;

    @Mock
    private ChatEngine chatEngine;

    @Mock
    private StageAnalyzer stageAnalyzer;

    private ChatService chatService;

    @BeforeEach
    void setUp() {
        chatService = new ChatService(chatSessionRepository, chatMessageRepository, reportRepository, chatEngine,
                new ConsultationProgressTracker(stageAnalyzer));
    }

    @Test
    void 비로그인으로_내_세션_목록을_조회하면_UNAUTHORIZED이고_저장소를_조회하지_않는다() {
        assertThatThrownBy(() -> chatService.getMySessions(null))
                .isInstanceOfSatisfying(SignalException.class,
                        e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.UNAUTHORIZED));

        verifyNoInteractions(chatSessionRepository);
    }

    @Test
    void 세션을_생성하면_UUID_sessionId가_발급된다() {
        when(chatSessionRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        ChatSession session = chatService.createSession(1L, null, false);

        assertThat(session.getSessionId()).isNotBlank();
        assertThat(session.getCreatedAt()).isNotNull();
    }

    @Test
    void 메시지를_보내면_사용자_메시지와_봇_응답이_저장되고_엔진_결과가_반환된다() {
        ChatSession session = ChatSession.builder().sessionId("session-1").userId(1L).build();
        when(chatSessionRepository.findBySessionId("session-1")).thenReturn(Optional.of(session));
        ChatEngineResponse engineResponse = new ChatEngineResponse(
                "안녕하세요", SituationType.GENERAL, List.of("조금 더 알려주세요"), false, List.of());
        when(chatEngine.respond(eq("안녕"), any())).thenReturn(engineResponse);
        when(chatMessageRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(reportRepository.findByUserIdOrderByCreatedAtDesc(1L)).thenReturn(List.of());

        SendMessageResult result = chatService.sendMessage("session-1", 1L, null, "안녕");

        assertThat(result.botMessage().getRole()).isEqualTo(ChatRole.BOT);
        assertThat(result.botMessage().getContent()).isEqualTo("안녕하세요");
        assertThat(result.engineResponse()).isEqualTo(engineResponse);

        ArgumentCaptor<ChatMessage> captor = ArgumentCaptor.forClass(ChatMessage.class);
        verify(chatMessageRepository, times(2)).save(captor.capture());
        List<ChatMessage> saved = captor.getAllValues();
        assertThat(saved.get(0).getRole()).isEqualTo(ChatRole.USER);
        assertThat(saved.get(0).getContent()).isEqualTo("안녕");
        assertThat(saved.get(1).getRole()).isEqualTo(ChatRole.BOT);
    }

    @Test
    void 이전_메시지가_있으면_history로_변환해_엔진에_전달한다() {
        ChatSession session = ChatSession.builder().sessionId("session-1").userId(1L).build();
        when(chatSessionRepository.findBySessionId("session-1")).thenReturn(Optional.of(session));

        ChatMessage prevUser = ChatMessage.builder()
                .chatSessionId(session.getId()).role(ChatRole.USER).content("전에 한 말").build();
        ChatMessage prevBot = ChatMessage.builder()
                .chatSessionId(session.getId()).role(ChatRole.BOT).content("전에 답한 말").build();
        when(chatMessageRepository.findByChatSessionIdOrderByCreatedAtAsc(session.getId()))
                .thenReturn(List.of(prevUser, prevBot));

        ChatEngineResponse engineResponse = new ChatEngineResponse(
                "답변", SituationType.GENERAL, List.of(), false, List.of());
        when(chatEngine.respond(eq("이어서 말할게요"), any())).thenReturn(engineResponse);
        when(chatMessageRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(reportRepository.findByUserIdOrderByCreatedAtDesc(1L)).thenReturn(List.of());

        chatService.sendMessage("session-1", 1L, null, "이어서 말할게요");

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<ChatTurn>> historyCaptor = ArgumentCaptor.forClass(List.class);
        verify(chatEngine).respond(eq("이어서 말할게요"), historyCaptor.capture());
        assertThat(historyCaptor.getValue()).containsExactly(
                new ChatTurn(ChatSpeaker.USER, "전에 한 말"),
                new ChatTurn(ChatSpeaker.BOT, "전에 답한 말"));
    }

    @Test
    void 존재하지_않는_세션에_메시지를_보내면_예외가_발생한다() {
        when(chatSessionRepository.findBySessionId("unknown")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> chatService.sendMessage("unknown", 1L, null, "안녕"))
                .isInstanceOf(SignalException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.NOT_FOUND);
    }

    @Test
    void 존재하는_세션의_메시지목록을_시간순으로_반환한다() {
        ChatSession session = ChatSession.builder().sessionId("session-1").userId(1L).build();
        when(chatSessionRepository.findBySessionId("session-1")).thenReturn(Optional.of(session));
        ChatMessage message = ChatMessage.builder()
                .chatSessionId(session.getId()).role(ChatRole.USER).content("안녕").build();
        when(chatMessageRepository.findByChatSessionIdOrderByCreatedAtAsc(session.getId()))
                .thenReturn(List.of(message));

        List<ChatMessage> messages = chatService.getMessages("session-1", 1L, null);

        assertThat(messages).containsExactly(message);
    }

    @Test
    void 존재하지_않는_세션의_메시지를_조회하면_예외가_발생한다() {
        when(chatSessionRepository.findBySessionId("unknown")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> chatService.getMessages("unknown", 1L, null))
                .isInstanceOf(SignalException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.NOT_FOUND);
    }

    @Test
    void 메시지가_없는_세션의_요약은_기본값을_반환한다() {
        ChatSession session = ChatSession.builder().sessionId("session-1").userId(1L).build();
        when(chatSessionRepository.findBySessionId("session-1")).thenReturn(Optional.of(session));
        when(reportRepository.findByUserIdOrderByCreatedAtDesc(1L)).thenReturn(List.of());

        ChatSummaryResponse summary = chatService.getSummary("session-1", 1L, null);

        assertThat(summary.situation()).isEqualTo("상담 시작 전");
        assertThat(summary.riskLevel()).isEqualTo(RiskLevel.LOW);
        // 새 상담은 NOT_STARTED, 진행률 0
        assertThat(summary.progressPercent()).isZero();
        assertThat(summary.progress().status()).isEqualTo(ConsultationProgressStatus.NOT_STARTED);
        assertThat(summary.progress().currentStage()).isNull();
        assertThat(summary.recommendedSteps()).containsExactly("증거 보존", "플랫폼 신고", "전문 기관 상담");
    }

    @Test
    void 모든_단계가_완료되면_봇이_종료를_제안하고_진행률은_100퍼센트다() {
        ChatSession session = ChatSession.builder().sessionId("session-1").userId(1L).build();
        session.recordEngineResult(SituationType.IMAGE_ABUSE, false, true);
        session.markEvidenceUrlMentioned();
        when(chatSessionRepository.findBySessionId("session-1")).thenReturn(Optional.of(session));

        Report finalizedReport = Report.builder()
                .userId(1L).description("설명").sourceUrls(List.of("http://a.com")).build();
        finalizedReport.markFinalized("doc-url");
        when(reportRepository.findByUserIdOrderByCreatedAtDesc(1L)).thenReturn(List.of(finalizedReport));

        ChatEngineResponse engineResponse = new ChatEngineResponse(
                "평범한 답변", SituationType.IMAGE_ABUSE, List.of(), false, List.of());
        when(chatEngine.respond(eq("고마워요"), any())).thenReturn(engineResponse);
        when(chatMessageRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        SendMessageResult result = chatService.sendMessage("session-1", 1L, null, "고마워요");

        assertThat(session.isAwaitingEndConfirmation()).isTrue();
        assertThat(result.sessionEnded()).isFalse();
        assertThat(result.engineResponse().reply()).contains("마무리해도 괜찮을까요");

        ChatSummaryResponse summary = chatService.getSummary("session-1", 1L, null);
        assertThat(summary.progressPercent()).isEqualTo(100);
        assertThat(result.progress().status()).isEqualTo(ConsultationProgressStatus.COMPLETED);
    }

    @Test
    void 메시지_응답에_현재_단계와_완료_단계와_진행률이_포함된다() {
        ChatSession session = ChatSession.builder().sessionId("session-1").userId(1L).build();
        when(chatSessionRepository.findBySessionId("session-1")).thenReturn(Optional.of(session));
        when(reportRepository.findByUserIdOrderByCreatedAtDesc(1L)).thenReturn(List.of());
        when(chatMessageRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(chatEngine.respond(eq("제 사진이 유포됐어요"), any())).thenReturn(new ChatEngineResponse(
                "신고 방법을 알려드릴게요", SituationType.IMAGE_ABUSE, List.of(), false, List.of()));
        when(stageAnalyzer.analyze(any(), any(), any())).thenReturn(Map.of(
                ConsultationStage.EVIDENCE_PRESERVATION, StageStatus.IN_PROGRESS,
                ConsultationStage.PLATFORM_REPORT, StageStatus.IN_PROGRESS));

        SendMessageResult result = chatService.sendMessage("session-1", 1L, null, "제 사진이 유포됐어요");

        assertThat(result.progress().status()).isEqualTo(ConsultationProgressStatus.IN_PROGRESS);
        assertThat(result.progress().completedStages())
                .containsExactly(ConsultationStage.SITUATION_CHECK, ConsultationStage.EMOTIONAL_SUPPORT);
        assertThat(result.progress().currentStage()).isEqualTo(ConsultationStage.EVIDENCE_PRESERVATION);
        assertThat(result.progress().progressPercent()).isEqualTo(40);
    }

    @Test
    void 일반_상담은_해당없는_단계를_제외하고_진행률을_계산한다() {
        ChatSession session = ChatSession.builder().sessionId("session-1").userId(1L).build();
        when(chatSessionRepository.findBySessionId("session-1")).thenReturn(Optional.of(session));
        when(reportRepository.findByUserIdOrderByCreatedAtDesc(1L)).thenReturn(List.of());
        when(chatMessageRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(chatEngine.respond(eq("안녕하세요"), any())).thenReturn(new ChatEngineResponse(
                "안녕하세요", SituationType.GENERAL, List.of(), false, List.of()));
        when(stageAnalyzer.analyze(any(), any(), any())).thenThrow(new IllegalStateException("LLM 실패"));

        SendMessageResult result = chatService.sendMessage("session-1", 1L, null, "안녕하세요");

        assertThat(result.progress().stages().get(ConsultationStage.EVIDENCE_PRESERVATION))
                .isEqualTo(StageStatus.NOT_APPLICABLE);
        // 룰 기반 폴백: 정서 안정만 완료, 3개 단계 중 1개 -> 33%
        assertThat(result.progress().progressPercent()).isEqualTo(33);
    }

    @Test
    void 종료_제안에_긍정하면_세션이_종료된다() {
        ChatSession session = ChatSession.builder().sessionId("session-1").userId(1L).build();
        session.recordEngineResult(SituationType.IMAGE_ABUSE, false, true);
        session.markEvidenceUrlMentioned();
        session.markAwaitingEndConfirmation();
        when(chatSessionRepository.findBySessionId("session-1")).thenReturn(Optional.of(session));
        when(chatMessageRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        SendMessageResult result = chatService.sendMessage("session-1", 1L, null, "네 좋아요");

        assertThat(result.sessionEnded()).isTrue();
        assertThat(session.isAwaitingEndConfirmation()).isFalse();
    }

    @Test
    void 종료_제안에_더_얘기하고_싶다고_하면_대화가_계속된다() {
        ChatSession session = ChatSession.builder().sessionId("session-1").userId(1L).build();
        session.recordEngineResult(SituationType.IMAGE_ABUSE, false, true);
        session.markEvidenceUrlMentioned();
        session.markAwaitingEndConfirmation();
        when(chatSessionRepository.findBySessionId("session-1")).thenReturn(Optional.of(session));
        when(reportRepository.findByUserIdOrderByCreatedAtDesc(1L)).thenReturn(List.of());
        when(chatMessageRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        ChatEngineResponse engineResponse = new ChatEngineResponse(
                "더 말씀해주세요", SituationType.IMAGE_ABUSE, List.of(), false, List.of());
        when(chatEngine.respond(eq("더 얘기하고 싶어요"), any())).thenReturn(engineResponse);

        SendMessageResult result = chatService.sendMessage("session-1", 1L, null, "더 얘기하고 싶어요");

        assertThat(result.sessionEnded()).isFalse();
        assertThat(session.isAwaitingEndConfirmation()).isFalse();
        assertThat(result.engineResponse().reply()).isEqualTo("더 말씀해주세요");
    }
}
