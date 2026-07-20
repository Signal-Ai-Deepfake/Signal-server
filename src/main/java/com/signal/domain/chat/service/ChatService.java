package com.signal.domain.chat.service;

import com.signal.domain.chat.engine.ChatEngine;
import com.signal.domain.chat.engine.ChatEngineResponse;
import com.signal.domain.chat.entity.ChatMessage;
import com.signal.domain.chat.entity.ChatRole;
import com.signal.domain.chat.entity.ChatSession;
import com.signal.domain.chat.repository.ChatMessageRepository;
import com.signal.domain.chat.repository.ChatSessionRepository;
import com.signal.global.exception.ErrorCode;
import com.signal.global.exception.SignalException;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ChatService {

    private final ChatSessionRepository chatSessionRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final ChatEngine chatEngine;

    @Transactional
    public ChatSession createSession() {
        ChatSession session = ChatSession.builder()
                .sessionId(UUID.randomUUID().toString())
                .build();
        return chatSessionRepository.save(session);
    }

    @Transactional
    public SendMessageResult sendMessage(String sessionId, String content) {
        ChatSession session = getSession(sessionId);

        chatMessageRepository.save(ChatMessage.builder()
                .chatSessionId(session.getId())
                .role(ChatRole.USER)
                .content(content)
                .build());

        ChatEngineResponse engineResponse = chatEngine.respond(content);

        ChatMessage botMessage = chatMessageRepository.save(ChatMessage.builder()
                .chatSessionId(session.getId())
                .role(ChatRole.BOT)
                .content(engineResponse.reply())
                .build());

        return new SendMessageResult(botMessage, engineResponse);
    }

    public List<ChatMessage> getMessages(String sessionId) {
        ChatSession session = getSession(sessionId);
        return chatMessageRepository.findByChatSessionIdOrderByCreatedAtAsc(session.getId());
    }

    private ChatSession getSession(String sessionId) {
        return chatSessionRepository.findBySessionId(sessionId)
                .orElseThrow(() -> new SignalException(ErrorCode.NOT_FOUND));
    }
}
