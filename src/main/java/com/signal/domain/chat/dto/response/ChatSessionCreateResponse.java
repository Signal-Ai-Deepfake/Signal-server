package com.signal.domain.chat.dto.response;

import com.signal.domain.chat.entity.ChatSession;
import java.time.LocalDateTime;

public record ChatSessionCreateResponse(String sessionId, LocalDateTime createdAt) {

    public static ChatSessionCreateResponse from(ChatSession session) {
        return new ChatSessionCreateResponse(session.getSessionId(), session.getCreatedAt());
    }
}
