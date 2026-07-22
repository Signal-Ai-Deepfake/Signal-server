package com.signal.domain.chat.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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
import com.signal.domain.report.repository.ReportRepository;
import com.signal.domain.riskassessment.entity.RiskLevel;
import com.signal.global.exception.ErrorCode;
import com.signal.global.exception.SignalException;
import java.util.List;
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

    private ChatService chatService;

    @BeforeEach
    void setUp() {
        chatService = new ChatService(chatSessionRepository, chatMessageRepository, reportRepository, chatEngine);
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
        when(chatEngine.respond("안녕")).thenReturn(engineResponse);
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
        // 위기 신호가 한 번도 없었으면 "정서 안정" 단계는 자동 충족으로 간주 (16%)
        assertThat(summary.progressPercent()).isEqualTo(16);
        assertThat(summary.recommendedSteps()).containsExactly("증거 보존", "플랫폼 신고", "전문 기관 상담");
    }

    @Test
    void 여섯_체크리스트가_모두_충족되면_봇이_종료를_제안하고_진행률은_96퍼센트다() {
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
        when(chatEngine.respond("고마워요")).thenReturn(engineResponse);
        when(chatMessageRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        SendMessageResult result = chatService.sendMessage("session-1", 1L, null, "고마워요");

        assertThat(session.isAwaitingEndConfirmation()).isTrue();
        assertThat(result.sessionEnded()).isFalse();
        assertThat(result.engineResponse().reply()).contains("마무리해도 괜찮을까요");

        ChatSummaryResponse summary = chatService.getSummary("session-1", 1L, null);
        assertThat(summary.progressPercent()).isEqualTo(96);
    }

    @Test
    void 종료_제안에_긍정하면_세션이_종료되고_진행률이_100퍼센트다() {
        ChatSession session = ChatSession.builder().sessionId("session-1").userId(1L).build();
        session.recordEngineResult(SituationType.IMAGE_ABUSE, false, true);
        session.markEvidenceUrlMentioned();
        session.markAwaitingEndConfirmation();
        when(chatSessionRepository.findBySessionId("session-1")).thenReturn(Optional.of(session));
        when(reportRepository.findByUserIdOrderByCreatedAtDesc(1L)).thenReturn(List.of());
        when(chatMessageRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        SendMessageResult result = chatService.sendMessage("session-1", 1L, null, "네 좋아요");

        assertThat(result.sessionEnded()).isTrue();
        assertThat(session.isAwaitingEndConfirmation()).isFalse();

        ChatSummaryResponse summary = chatService.getSummary("session-1", 1L, null);
        assertThat(summary.progressPercent()).isEqualTo(100);
    }

    @Test
    void 종료_제안에_더_얘기하고_싶다고_하면_96퍼센트를_유지하고_대화가_계속된다() {
        ChatSession session = ChatSession.builder().sessionId("session-1").userId(1L).build();
        session.recordEngineResult(SituationType.IMAGE_ABUSE, false, true);
        session.markEvidenceUrlMentioned();
        session.markAwaitingEndConfirmation();
        when(chatSessionRepository.findBySessionId("session-1")).thenReturn(Optional.of(session));
        when(reportRepository.findByUserIdOrderByCreatedAtDesc(1L)).thenReturn(List.of());
        when(chatMessageRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        ChatEngineResponse engineResponse = new ChatEngineResponse(
                "더 말씀해주세요", SituationType.IMAGE_ABUSE, List.of(), false, List.of());
        when(chatEngine.respond("더 얘기하고 싶어요")).thenReturn(engineResponse);

        SendMessageResult result = chatService.sendMessage("session-1", 1L, null, "더 얘기하고 싶어요");

        assertThat(result.sessionEnded()).isFalse();
        assertThat(session.isAwaitingEndConfirmation()).isFalse();
        assertThat(result.engineResponse().reply()).isEqualTo("더 말씀해주세요");
    }
}
