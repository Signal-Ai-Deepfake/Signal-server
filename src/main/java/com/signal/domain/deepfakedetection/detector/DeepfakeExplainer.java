package com.signal.domain.deepfakedetection.detector;

import com.signal.domain.deepfakedetection.entity.DetectionBand;
import com.signal.domain.deepfakedetection.entity.Evidence;
import java.util.List;

/**
 * 이미 정해진 합성 가능성 등급을 사용자에게 풀어 쓰는 설명기.
 *
 * 설명기는 등급·점수·verdict를 입력으로만 받고 결과로는 {@link Evidence}(설명 문장과 위치)만 돌려준다.
 * 그래서 어떤 구현이든 등급을 바꿀 수 없고, 설명이 실패해도 탐지 결과에는 영향이 없다(빈 목록).
 */
public interface DeepfakeExplainer {

    /**
     * @param content  이미지 원본 바이트
     * @param mimeType 이미지 MIME 타입
     * @param band     이미 정해진 등급
     * @param width    이미지 너비(px), 모르면 0
     * @param height   이미지 높이(px), 모르면 0
     * @return 설명 근거. 설명할 것이 없거나 실패하면 빈 목록(예외를 던지지 않는다)
     */
    List<Evidence> explain(byte[] content, String mimeType, DetectionBand band, int width, int height);
}
