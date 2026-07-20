package com.signal.domain.chat.engine;

import java.util.List;

public record ChatEngineResponse(
        String reply,
        SituationType situationType,
        List<String> suggestedActions,
        boolean crisisDetected,
        List<String> recommendedAgencies
) {
}
