package com.signal.domain.chat.progress;

/**
 * 대화 분석과 무관하게 서버가 확실히 아는 사실. LLM 판단이 틀려도 이 사실은 뒤집지 못한다.
 *
 * @param crisisDetected       이번 메시지에서 위기 신호가 감지됐는가
 * @param everCrisisDetected   이 세션에서 위기 신호가 한 번이라도 감지됐는가(위기 상담 기관을 안내한 적이 있다는 뜻)
 * @param evidenceUrlMentioned 사용자가 증거로 볼 수 있는 URL을 대화에 남겼는가
 * @param reportStarted        신고 문서를 작성하기 시작했는가
 * @param reportFinalized      신고 문서를 확정했는가
 * @param userTurnCount        지금까지 사용자가 보낸 메시지 수(이번 메시지 포함)
 */
public record StageFacts(
        boolean crisisDetected,
        boolean everCrisisDetected,
        boolean evidenceUrlMentioned,
        boolean reportStarted,
        boolean reportFinalized,
        int userTurnCount
) {
}
