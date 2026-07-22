package com.signal.domain.chat.dto.response;

import com.signal.domain.riskassessment.entity.RiskLevel;
import java.util.List;

public record ChatSummaryResponse(
        String situation,
        List<String> recommendedSteps,
        RiskLevel riskLevel,
        String riskDescription,
        int progressPercent
) {
}
