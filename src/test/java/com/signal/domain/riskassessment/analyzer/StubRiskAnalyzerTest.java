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
    void 얼굴이_검출되면_요인과_추천사항과_얼굴목록을_포함한다() {
        MockMultipartFile image = new MockMultipartFile("image", "photo.png", "image/png", new byte[]{1, 2, 3, 4, 5});

        RiskAnalysisResult result = stubRiskAnalyzer.analyze(image);

        if (result.faceDetected()) {
            assertThat(result.overallRiskLevel()).isNotNull();
            assertThat(result.factors()).isNotEmpty();
            assertThat(result.recommendations()).isNotEmpty();
            assertThat(result.faces()).isNotEmpty();
        } else {
            assertThat(result.factors()).isEmpty();
            assertThat(result.faces()).isEmpty();
        }
    }
}
