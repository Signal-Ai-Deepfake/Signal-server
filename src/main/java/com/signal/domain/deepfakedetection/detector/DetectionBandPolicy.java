package com.signal.domain.deepfakedetection.detector;

import com.signal.domain.deepfakedetection.entity.AssessmentBasis;
import com.signal.domain.deepfakedetection.entity.DetectionBand;
import com.signal.domain.deepfakedetection.entity.ProvenanceSignal;
import com.signal.domain.deepfakedetection.entity.UndeterminedReason;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * 모델 점수(0~100)와 입력 품질, 출처 신호로 등급({@link DetectionBand})을 정한다. LLM은 관여하지 않는다.
 *
 * 순서:
 * 1. 강한 출처 신호(생성 도구의 흔적)가 있으면 점수와 무관하게 HIGH (근거: PROVENANCE)
 * 2. 짧은 변이 min-short-edge-px보다 작으면 UNDETERMINED (모델은 짧은 변을 440으로 키우므로 작은 이미지는 점수를 믿기 어렵다)
 * 3. 점수가 low-below 미만이면 LOW, high-above 초과이면 HIGH, 그 사이는 UNCERTAIN
 *
 * 임계값의 기본값(30, 70)은 기존 verdict 구간을 그대로 옮긴 잠정값이다. 평가 세트(#35)로 측정해서 정해야 하며,
 * 그때까지는 이 값을 근거로 정확도를 말하지 않는다.
 */
@Component
public class DetectionBandPolicy {

    public record Assessment(DetectionBand band, AssessmentBasis basis, UndeterminedReason undeterminedReason) {
    }

    private final int lowBelow;
    private final int highAbove;
    private final int minShortEdgePx;

    public DetectionBandPolicy(@Value("${deepfake-detection.bands.low-below:30}") int lowBelow,
                               @Value("${deepfake-detection.bands.high-above:70}") int highAbove,
                               @Value("${deepfake-detection.bands.min-short-edge-px:256}") int minShortEdgePx) {
        if (lowBelow < 0 || highAbove > 100 || lowBelow > highAbove) {
            throw new IllegalArgumentException("등급 임계값이 올바르지 않습니다: low-below=" + lowBelow + ", high-above=" + highAbove);
        }
        this.lowBelow = lowBelow;
        this.highAbove = highAbove;
        this.minShortEdgePx = Math.max(0, minShortEdgePx);
    }

    /**
     * @param riskScore 모델 점수를 0~100으로 옮긴 값
     * @param width     이미지 너비(px). 모르면 0
     * @param height    이미지 높이(px). 모르면 0
     * @param signals   파일에서 찾은 출처 신호
     */
    public Assessment assess(int riskScore, int width, int height, List<ProvenanceSignal> signals) {
        if (ProvenanceSignalExtractor.hasStrongSignal(signals)) {
            return new Assessment(DetectionBand.HIGH, AssessmentBasis.PROVENANCE, null);
        }
        if (width > 0 && height > 0 && Math.min(width, height) < minShortEdgePx) {
            return new Assessment(DetectionBand.UNDETERMINED, AssessmentBasis.MODEL, UndeterminedReason.LOW_RESOLUTION);
        }
        if (riskScore < lowBelow) {
            return new Assessment(DetectionBand.LOW, AssessmentBasis.MODEL, null);
        }
        if (riskScore > highAbove) {
            return new Assessment(DetectionBand.HIGH, AssessmentBasis.MODEL, null);
        }
        return new Assessment(DetectionBand.UNCERTAIN, AssessmentBasis.MODEL, null);
    }
}
