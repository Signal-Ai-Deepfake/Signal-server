package com.signal.domain.deepfakedetection.detector;

public interface DeepfakeDetector {

    /**
     * 업로드된 이미지/영상의 딥페이크 여부를 비동기로 분석하고, 완료/실패 결과를 해당
     * DeepfakeDetection에 반영한다. 실제 AI 서버 연동 전까지는 스텁 구현체가 대신한다.
     */
    void detect(Long detectionId, String fileUrl, boolean isVideo);
}
