package com.signal.domain.chat.service;

import com.signal.domain.chat.engine.ChatEngineResponse;
import com.signal.domain.chat.entity.ChatMessage;

public record SendMessageResult(ChatMessage botMessage, ChatEngineResponse engineResponse) {
}
