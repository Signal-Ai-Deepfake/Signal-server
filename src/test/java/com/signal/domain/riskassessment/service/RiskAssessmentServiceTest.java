package com.signal.domain.riskassessment.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.signal.domain.riskassessment.analyzer.RiskAnalysisResult;
import com.signal.domain.riskassessment.analyzer.RiskAnalyzer;
import com.signal.domain.riskassessment.entity.BoundingBox;
import com.signal.domain.riskassessment.entity.DetectedFace;
import com.signal.domain.riskassessment.entity.RiskAssessment;
import com.signal.domain.riskassessment.entity.RiskFactor;
import com.signal.domain.riskassessment.entity.RiskLevel;
import com.signal.domain.riskassessment.repository.RiskAssessmentRepository;
import com.signal.global.exception.ErrorCode;
import com.signal.global.exception.SignalException;
import com.signal.global.file.FileStorage;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

@ExtendWith(MockitoExtension.class)
class RiskAssessmentServiceTest {

    @Mock
    private RiskAssessmentRepository riskAssessmentRepository;

    @Mock
    private RiskAnalyzer riskAnalyzer;

    @Mock
    private FileStorage fileStorage;

    private RiskAssessmentService riskAssessmentService;

    @BeforeEach
    void setUp() {
        riskAssessmentService = new RiskAssessmentService(riskAssessmentRepository, riskAnalyzer, fileStorage);
    }

    @Test
    void 위험도_진단을_요청하면_분석결과가_저장되고_반환된다() {
        MockMultipartFile image = new MockMultipartFile("image", "photo.png", "image/png", new byte[]{1, 2, 3});
        RiskAnalysisResult result = new RiskAnalysisResult(
                true,
                RiskLevel.HIGH,
                80,
                List.of(RiskFactor.builder().type("IMAGE_QUALITY").label("이미지 품질").score(80).description("설명").build()),
                List.of("공개를 자제하세요."),
                List.of(DetectedFace.builder()
                        .faceIndex(0)
                        .boundingBox(BoundingBox.builder().x(1).y(2).width(3).height(4).build())
                        .build()));

        when(riskAnalyzer.analyze(image)).thenReturn(result);
        when(fileStorage.store(any(), anyString())).thenReturn("/uploads/risk-assessments/generated.png");
        when(riskAssessmentRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        RiskAssessment saved = riskAssessmentService.createAssessment(1L, null, image);

        assertThat(saved.getUserId()).isEqualTo(1L);
        assertThat(saved.getOverallRiskLevel()).isEqualTo(RiskLevel.HIGH);
        assertThat(saved.getOverallScore()).isEqualTo(80);
        assertThat(saved.isFaceDetected()).isTrue();
        assertThat(saved.getFactors()).hasSize(1);
        assertThat(saved.getFaces()).hasSize(1);
        verify(fileStorage).store(image, "risk-assessments");
    }

    @Test
    void 얼굴이_검출되지_않으면_예외가_발생한다() {
        MockMultipartFile image = new MockMultipartFile("image", "photo.png", "image/png", new byte[]{1, 2, 3});
        when(riskAnalyzer.analyze(image)).thenReturn(RiskAnalysisResult.faceNotDetected());

        assertThatThrownBy(() -> riskAssessmentService.createAssessment(1L, null, image))
                .isInstanceOf(SignalException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.FACE_NOT_DETECTED);
    }

    @Test
    void 지원하지_않는_파일_형식이면_예외가_발생한다() {
        MockMultipartFile file = new MockMultipartFile("image", "malware.exe", "application/octet-stream", new byte[]{1});

        assertThatThrownBy(() -> riskAssessmentService.createAssessment(1L, null, file))
                .isInstanceOf(SignalException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.UNSUPPORTED_MEDIA_TYPE);
    }

    @Test
    void 파일_크기가_10MB를_초과하면_예외가_발생한다() {
        byte[] tooLarge = new byte[(int) (10L * 1024 * 1024) + 1];
        MockMultipartFile file = new MockMultipartFile("image", "big.png", "image/png", tooLarge);

        assertThatThrownBy(() -> riskAssessmentService.createAssessment(1L, null, file))
                .isInstanceOf(SignalException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.FILE_TOO_LARGE);
    }

    @Test
    void 존재하는_소유자가_조회하면_결과를_반환한다() {
        RiskAssessment riskAssessment = RiskAssessment.builder()
                .userId(1L)
                .imageUrl("/uploads/risk-assessments/generated.png")
                .overallRiskLevel(RiskLevel.LOW)
                .overallScore(10)
                .faceDetected(true)
                .factors(List.of())
                .recommendations(List.of())
                .faces(List.of())
                .build();
        when(riskAssessmentRepository.findById(1L)).thenReturn(Optional.of(riskAssessment));

        RiskAssessment result = riskAssessmentService.getAssessment(1L, null, 1L);

        assertThat(result).isEqualTo(riskAssessment);
    }

    @Test
    void 존재하지_않는_진단이면_예외가_발생한다() {
        when(riskAssessmentRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> riskAssessmentService.getAssessment(1L, null, 1L))
                .isInstanceOf(SignalException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.NOT_FOUND);
    }

    @Test
    void 소유자가_아니면_예외가_발생한다() {
        RiskAssessment riskAssessment = RiskAssessment.builder()
                .userId(2L)
                .imageUrl("/uploads/risk-assessments/generated.png")
                .overallRiskLevel(RiskLevel.LOW)
                .overallScore(10)
                .faceDetected(true)
                .factors(List.of())
                .recommendations(List.of())
                .faces(List.of())
                .build();
        when(riskAssessmentRepository.findById(1L)).thenReturn(Optional.of(riskAssessment));

        assertThatThrownBy(() -> riskAssessmentService.getAssessment(1L, null, 1L))
                .isInstanceOf(SignalException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.FORBIDDEN);
    }
}
