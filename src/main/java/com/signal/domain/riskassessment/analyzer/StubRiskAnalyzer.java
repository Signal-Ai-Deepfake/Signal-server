package com.signal.domain.riskassessment.analyzer;

import com.signal.domain.riskassessment.entity.BoundingBox;
import com.signal.domain.riskassessment.entity.DetectedFace;
import com.signal.domain.riskassessment.entity.RiskFactor;
import com.signal.domain.riskassessment.entity.RiskLevel;
import com.signal.global.exception.ErrorCode;
import com.signal.global.exception.SignalException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

/**
 * 실제 AI 서버 연동 전까지 사용하는 스텁 구현체. 이미지 바이트 해시로 점수를 결정론적으로 산출해
 * 동일 이미지에 대해 항상 같은 결과를 반환한다. 실서버 연동 시 이 클래스를 교체한다.
 */
@Component
public class StubRiskAnalyzer implements RiskAnalyzer {

    private static final List<String> FACTOR_TYPES = List.of(
            "IMAGE_QUALITY", "FACE_EXPOSURE", "METADATA_LEAK", "REVERSE_SEARCH_RISK");

    @Override
    public RiskAnalysisResult analyze(MultipartFile image) {
        int seed = Math.abs(contentHash(image));

        if (seed % 10 == 0) {
            return RiskAnalysisResult.faceNotDetected();
        }

        int overallScore = seed % 101;
        RiskLevel overallRiskLevel = RiskLevel.fromScore(overallScore);

        return new RiskAnalysisResult(
                true,
                overallRiskLevel,
                overallScore,
                buildFactors(seed),
                buildRecommendations(overallRiskLevel),
                List.of(buildFace(seed)));
    }

    private List<RiskFactor> buildFactors(int seed) {
        List<RiskFactor> factors = new ArrayList<>();
        for (int i = 0; i < FACTOR_TYPES.size(); i++) {
            String type = FACTOR_TYPES.get(i);
            int score = (seed + i * 17) % 101;
            factors.add(RiskFactor.builder()
                    .type(type)
                    .label(toLabel(type))
                    .score(score)
                    .description(toDescription(type, score))
                    .build());
        }
        return factors;
    }

    private DetectedFace buildFace(int seed) {
        BoundingBox boundingBox = BoundingBox.builder()
                .x(seed % 100)
                .y(seed % 80)
                .width(200 + seed % 50)
                .height(200 + seed % 50)
                .build();
        return DetectedFace.builder()
                .faceIndex(0)
                .boundingBox(boundingBox)
                .build();
    }

    private List<String> buildRecommendations(RiskLevel riskLevel) {
        return switch (riskLevel) {
            case HIGH -> List.of(
                    "해당 이미지의 공개 게시를 자제하세요.",
                    "이미지 보호 처리(노이즈 삽입) 기능을 이용해 도용 위험을 낮추세요.",
                    "정기적으로 얼굴 모니터링을 신청해 무단 사용 여부를 추적하세요.");
            case MEDIUM -> List.of(
                    "이미지 공개 범위를 지인으로 한정하는 것을 권장합니다.",
                    "이미지 보호 처리 기능을 검토해보세요.");
            case LOW -> List.of("특별한 조치 없이도 안전한 수준이지만, 정기적인 점검을 권장합니다.");
        };
    }

    private String toLabel(String type) {
        return switch (type) {
            case "IMAGE_QUALITY" -> "이미지 품질";
            case "FACE_EXPOSURE" -> "얼굴 노출도";
            case "METADATA_LEAK" -> "메타데이터 노출";
            case "REVERSE_SEARCH_RISK" -> "역검색 노출 위험";
            default -> type;
        };
    }

    private String toDescription(String type, int score) {
        return switch (type) {
            case "IMAGE_QUALITY" -> "이미지 해상도와 선명도가 딥페이크 합성에 활용될 수 있는 정도 (점수: " + score + ")";
            case "FACE_EXPOSURE" -> "얼굴이 정면으로 명확히 노출된 정도 (점수: " + score + ")";
            case "METADATA_LEAK" -> "촬영 위치·기기 등 메타데이터 노출 위험 (점수: " + score + ")";
            case "REVERSE_SEARCH_RISK" -> "역이미지 검색으로 원본을 추적당할 위험 (점수: " + score + ")";
            default -> "점수: " + score;
        };
    }

    private int contentHash(MultipartFile image) {
        try {
            return Arrays.hashCode(image.getBytes());
        } catch (IOException e) {
            throw new SignalException(ErrorCode.INTERNAL_SERVER_ERROR);
        }
    }
}
