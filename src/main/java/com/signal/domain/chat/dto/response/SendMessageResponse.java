package com.signal.domain.chat.dto.response;

import com.signal.domain.chat.engine.ChatEngineResponse;
import com.signal.domain.chat.engine.SituationType;
import com.signal.domain.chat.service.SendMessageResult;
import java.util.List;

public record SendMessageResponse(
        Long messageId,
        String reply,
        SituationType situationType,
        List<String> suggestedActions,
        boolean crisisDetected,
        List<String> recommendedAgencies
) {

    public static SendMessageResponse from(SendMessageResult result) {
        ChatEngineResponse engineResponse = result.engineResponse();
        return new SendMessageResponse(
                result.botMessage().getId(),
                engineResponse.reply(),
                engineResponse.situationType(),
                engineResponse.suggestedActions(),
                engineResponse.crisisDetected(),
                engineResponse.recommendedAgencies());
    }
}
