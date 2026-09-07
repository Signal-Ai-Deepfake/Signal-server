package com.signal.domain.riskassessment.analyzer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.signal.global.ai.VisionCompletionClient;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

@ExtendWith(MockitoExtension.class)
class LlmRiskAnalyzerTest {

    @Mock
    private VisionCompletionClient visionCompletionClient;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private MockMultipartFile samplePngImage() throws IOException {
        BufferedImage image = new BufferedImage(400, 300, BufferedImage.TYPE_INT_RGB);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ImageIO.write(image, "png", out);
        return new MockMultipartFile("image", "photo.png", "image/png", out.toByteArray());
    }

    @Test
    void LLM이_얼굴을_검출하면_요인_4개와_얼굴_정보를_포함한_결과를_반환한다() throws IOException {
        LlmRiskAnalyzer analyzer = new LlmRiskAnalyzer(visionCompletionClient, objectMapper, true);
        when(visionCompletionClient.complete(any(byte[].class), anyString(), anyString(), anyString()))
                .thenReturn("""
                        {
                          "faceDetected": true,
                          "faceBox": {"x":0.2,"y":0.1,"width":0.4,"height":0.5},
                          "imageQualityScore": 80,
                          "imageQualityNote": "해상도가 높습니다",
                          "faceExposureScore": 90,
                          "faceExposureNote": "정면으로 노출되어 있습니다",
                          "reverseSearchRiskScore": 30,
                          "reverseSearchRiskNote": "배경이 평범합니다"
                        }
                        """);

        RiskAnalysisResult result = analyzer.analyze(samplePngImage());

        assertThat(result.faceDetected()).isTrue();
        assertThat(result.factors()).hasSize(4);
        assertThat(result.faces()).hasSize(1);
        assertThat(result.overallRiskLevel()).isNotNull();
        assertThat(result.recommendations()).isNotEmpty();
        // 얼굴 박스가 실제 이미지(400x300) 픽셀 좌표로 환산되었는지 확인
        assertThat(result.faces().get(0).getBoundingBox().getX()).isEqualTo((int) Math.round(0.2 * 400));
        assertThat(result.faces().get(0).getBoundingBox().getY()).isEqualTo((int) Math.round(0.1 * 300));
    }

    @Test
    void LLM이_얼굴을_못_찾으면_faceNotDetected를_반환한다() throws IOException {
        LlmRiskAnalyzer analyzer = new LlmRiskAnalyzer(visionCompletionClient, objectMapper, true);
        when(visionCompletionClient.complete(any(byte[].class), anyString(), anyString(), anyString()))
                .thenReturn("""
                        {"faceDetected": false, "faceBox": null, "imageQualityScore": 0,
                         "imageQualityNote": "", "faceExposureScore": 0, "faceExposureNote": "",
                         "reverseSearchRiskScore": 0, "reverseSearchRiskNote": ""}
                        """);

        RiskAnalysisResult result = analyzer.analyze(samplePngImage());

        assertThat(result.faceDetected()).isFalse();
        assertThat(result.factors()).isEmpty();
        assertThat(result.faces()).isEmpty();
    }

    @Test
    void LLM_호출이_실패하면_룰_기반_분석으로_대체한다() throws IOException {
        LlmRiskAnalyzer analyzer = new LlmRiskAnalyzer(visionCompletionClient, objectMapper, true);
        when(visionCompletionClient.complete(any(byte[].class), anyString(), anyString(), anyString()))
                .thenThrow(new IllegalStateException("GROQ_API_KEY가 설정되지 않았습니다."));

        RiskAnalysisResult result = analyzer.analyze(samplePngImage());

        // 예외 없이 스텁(룰 기반) 결과가 대신 반환되어야 한다 (faceDetected 여부와 무관하게 정상 완료)
        assertThat(result).isNotNull();
    }

    @Test
    void vision_llm_비활성화면_LLM을_호출하지_않고_룰_기반_결과만_사용한다() throws IOException {
        LlmRiskAnalyzer analyzer = new LlmRiskAnalyzer(visionCompletionClient, objectMapper, false);

        RiskAnalysisResult result = analyzer.analyze(samplePngImage());

        assertThat(result).isNotNull();
        verify(visionCompletionClient, never()).complete(any(), any(), any(), any());
    }
}
