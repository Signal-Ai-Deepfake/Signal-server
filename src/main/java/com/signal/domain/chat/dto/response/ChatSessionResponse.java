package com.signal.domain.chat.dto.response;

import com.signal.domain.chat.entity.ChatSession;
import java.time.LocalDateTime;

public record ChatSessionResponse(String sessionId, boolean saveConsent, LocalDateTime createdAt) {

    public static ChatSessionResponse from(ChatSession session) {
        return new ChatSessionResponse(session.getSessionId(), session.isSaveConsent(), session.getCreatedAt());
    }
}
