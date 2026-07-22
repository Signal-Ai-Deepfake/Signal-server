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
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class ChatService {

    private static final long ANONYMOUS_CHAT_SESSION_LIMIT = 5;

    private final ChatSessionRepository chatSessionRepository;
    private final ChatMessageRepository chatMessageRepository;
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

        chatMessageRepository.save(ChatMessage.builder()
                .chatSessionId(session.getId())
                .role(ChatRole.USER)
                .content(content)
                .build());

        ChatEngineResponse engineResponse = chatEngine.respond(content);
        session.recordEngineResult(engineResponse.situationType(), engineResponse.crisisDetected());

        ChatMessage botMessage = chatMessageRepository.save(ChatMessage.builder()
                .chatSessionId(session.getId())
                .role(ChatRole.BOT)
                .content(engineResponse.reply())
                .build());

        return new SendMessageResult(botMessage, engineResponse);
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
