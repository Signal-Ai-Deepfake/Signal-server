package com.signal.domain.riskassessment.analyzer;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

class StubRiskAnalyzerTest {

    private final StubRiskAnalyzer stubRiskAnalyzer = new StubRiskAnalyzer();

    @Test
    void 동일한_이미지는_항상_동일한_결과를_반환한다() {
        MockMultipartFile image = new MockMultipartFile("image", "photo.png", "image/png", new byte[]{1, 2, 3, 4, 5});

        RiskAnalysisResult first = stubRiskAnalyzer.analyze(image);
        RiskAnalysisResult second = stubRiskAnalyzer.analyze(image);

        assertThat(first).isEqualTo(second);
    }

    @Test
    void 어떤_이미지든_얼굴없음으로_단정하지_않고_폴백_결과를_반환한다() {
        for (int i = 0; i < 200; i++) {
            MockMultipartFile image = new MockMultipartFile(
                    "image", "photo.png", "image/png", new byte[]{(byte) i, 2, 3, (byte) (i * 7), 5});

            RiskAnalysisResult result = stubRiskAnalyzer.analyze(image);

            assertThat(result.faceDetected()).isTrue();
            assertThat(result.fallbackUsed()).isTrue();
            assertThat(result.factors()).isNotEmpty();
            assertThat(result.recommendations()).isNotEmpty();
            assertThat(result.faces()).isNotEmpty();
        }
    }
}
