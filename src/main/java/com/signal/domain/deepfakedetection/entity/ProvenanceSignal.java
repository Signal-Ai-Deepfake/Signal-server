package com.signal.domain.deepfakedetection.entity;

import java.util.Optional;

/**
 * 파일 자체에서 읽을 수 있는 출처(provenance) 신호. 모델 점수와 별개로 결정론적으로 얻는다.
 *
 * 발견되면 강한 근거가 되지만, 없다고 해서 진짜라는 뜻은 아니다(메타데이터는 캡처·재저장·메신저 전송에서 사라진다).
 */
public enum ProvenanceSignal {
    /** PNG 텍스트 청크나 메타데이터에 AI 생성 도구(Stable Diffusion, ComfyUI, Midjourney 등)의 기록이 있다. */
    GENERATOR_METADATA(true, "이미지 파일의 메타데이터에 AI 생성 도구의 기록이 남아 있습니다."),
    /** IPTC digitalSourceType이 AI 생성(trainedAlgorithmicMedia)임을 표시한다. */
    AI_SOURCE_DECLARED(true, "이미지 파일의 메타데이터가 AI로 생성된 콘텐츠임을 표시하고 있습니다."),
    /**
     * AI로 일부를 편집·합성했다는 표시(IPTC compositeWithTrainedAlgorithmicMedia, compositeSynthetic, Adobe Firefly 등).
     * 실제 촬영한 사진에 생성형 채우기를 쓴 경우에도 남으므로 이미지 전체가 합성이라는 근거가 아니다.
     * 등급을 HIGH로 올리지 않고, 모델 점수가 낮을 때 LOW로 두지 않는 정도로만 쓴다.
     */
    AI_EDIT_DECLARED(false, "이미지 파일의 메타데이터에 AI로 편집하거나 합성한 흔적이 있습니다."),
    /** C2PA 콘텐츠 자격 증명이 포함돼 있다. 서명 검증은 하지 않으므로 참고용이고 등급에는 쓰지 않는다. */
    C2PA_MANIFEST(false, "콘텐츠 자격 증명(C2PA) 정보가 포함돼 있습니다. 서명은 검증하지 않았습니다.");

    private final boolean strong;
    private final String message;

    ProvenanceSignal(boolean strong, String message) {
        this.strong = strong;
        this.message = message;
    }

    /** true면 등급을 {@link DetectionBand#HIGH}로 올리는 근거로 쓴다. */
    public boolean isStrong() {
        return strong;
    }

    public String getMessage() {
        return message;
    }

    public static Optional<ProvenanceSignal> fromCode(String code) {
        if (code == null) {
            return Optional.empty();
        }
        try {
            return Optional.of(valueOf(code));
        } catch (IllegalArgumentException e) {
            return Optional.empty();
        }
    }
}
