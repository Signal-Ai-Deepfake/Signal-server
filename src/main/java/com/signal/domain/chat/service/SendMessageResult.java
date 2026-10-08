package com.signal.domain.chat.service;

import com.signal.domain.chat.engine.ChatEngineResponse;
import com.signal.domain.chat.entity.ChatMessage;
import com.signal.domain.chat.progress.ConsultationProgress;

public record SendMessageResult(
        ChatMessage botMessage,
        ChatEngineResponse engineResponse,
        boolean sessionEnded,
        ConsultationProgress progress) {
}
