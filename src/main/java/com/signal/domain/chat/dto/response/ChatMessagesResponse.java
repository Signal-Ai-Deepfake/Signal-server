package com.signal.domain.chat.dto.response;

import com.signal.domain.chat.entity.ChatMessage;
import java.util.List;

public record ChatMessagesResponse(List<ChatMessageResponse> messages) {

    public static ChatMessagesResponse from(List<ChatMessage> messages) {
        return new ChatMessagesResponse(messages.stream().map(ChatMessageResponse::from).toList());
    }
}
