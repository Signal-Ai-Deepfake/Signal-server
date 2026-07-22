package com.signal.domain.chat.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.signal.domain.chat.engine.ChatEngine;
import com.signal.domain.chat.engine.ChatEngineResponse;
import com.signal.domain.chat.engine.SituationType;
import com.signal.domain.chat.entity.ChatMessage;
import com.signal.domain.chat.entity.ChatRole;
import com.signal.domain.chat.entity.ChatSession;
import com.signal.domain.chat.repository.ChatMessageRepository;
import com.signal.domain.chat.repository.ChatSessionRepository;
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
    private ChatEngine chatEngine;

    private ChatService chatService;

    @BeforeEach
    void setUp() {
        chatService = new ChatService(chatSessionRepository, chatMessageRepository, chatEngine);
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
}
