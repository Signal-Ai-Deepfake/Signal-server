package com.signal.domain.chat.progress;

/** 대화 분석과 무관하게 서버가 확실히 아는 사실. LLM 판단이 틀려도 이 사실은 뒤집지 못한다. */
public record StageFacts(
        boolean crisisDetected,
        boolean emotionallyStabilized,
        boolean evidenceUrlMentioned,
        boolean reportStarted,
        boolean reportFinalized,
        boolean agenciesRecommended
) {
}
