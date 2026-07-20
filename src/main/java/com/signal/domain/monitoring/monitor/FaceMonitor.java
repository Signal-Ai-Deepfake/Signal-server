package com.signal.domain.monitoring.monitor;

public interface FaceMonitor {

    /**
     * 기준 이미지를 바탕으로 얼굴 도용 모니터링을 시작한다. 실제 AI 서버 연동 전까지는
     * 스텁 구현체가 초기 탐지 결과를 비동기로 시뮬레이션해 채워 넣는다.
     */
    void startMonitoring(Long monitoringId, String referenceImageUrl);
}
