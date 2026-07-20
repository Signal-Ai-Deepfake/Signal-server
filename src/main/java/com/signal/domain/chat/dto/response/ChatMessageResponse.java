package com.signal.domain.chat.dto.response;

import com.signal.domain.chat.entity.ChatMessage;
import com.signal.domain.chat.entity.ChatRole;
import java.time.LocalDateTime;

public record ChatMessageResponse(ChatRole role, String content, LocalDateTime createdAt) {

    public static ChatMessageResponse from(ChatMessage message) {
        return new ChatMessageResponse(message.getRole(), message.getContent(), message.getCreatedAt());
    }
}
