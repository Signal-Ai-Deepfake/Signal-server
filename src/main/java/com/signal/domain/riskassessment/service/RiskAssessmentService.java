package com.signal.domain.riskassessment.service;

import com.signal.domain.riskassessment.analyzer.RiskAnalysisResult;
import com.signal.domain.riskassessment.analyzer.RiskAnalyzer;
import com.signal.domain.riskassessment.entity.RiskAssessment;
import com.signal.domain.riskassessment.repository.RiskAssessmentRepository;
import com.signal.global.exception.ErrorCode;
import com.signal.global.exception.SignalException;
import com.signal.global.file.FileStorage;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RiskAssessmentService {

    private static final long MAX_IMAGE_SIZE = 10L * 1024 * 1024;
    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of("image/jpeg", "image/png");
    private static final String IMAGE_DIRECTORY = "risk-assessments";
    private static final long ANONYMOUS_ASSESSMENT_LIMIT = 5;

    private final RiskAssessmentRepository riskAssessmentRepository;
    private final RiskAnalyzer riskAnalyzer;
    private final FileStorage fileStorage;

    @Transactional
    public RiskAssessment createAssessment(Long userId, String anonymousId, MultipartFile image) {
        validateImage(image);
        if (userId == null) {
            validateAnonymousUsage(anonymousId);
        }

        RiskAnalysisResult result = riskAnalyzer.analyze(image);
        if (!result.faceDetected()) {
            throw new SignalException(ErrorCode.FACE_NOT_DETECTED);
        }

        String imageUrl = fileStorage.store(image, IMAGE_DIRECTORY);

        RiskAssessment riskAssessment = RiskAssessment.builder()
                .userId(userId)
                .anonymousId(userId == null ? anonymousId : null)
                .imageUrl(imageUrl)
                .overallRiskLevel(result.overallRiskLevel())
                .overallScore(result.overallScore())
                .faceDetected(true)
                .factors(result.factors())
                .recommendations(result.recommendations())
                .faces(result.faces())
                .build();

        return riskAssessmentRepository.save(riskAssessment);
    }

    public RiskAssessment getAssessment(Long userId, String anonymousId, Long assessmentId) {
        RiskAssessment riskAssessment = riskAssessmentRepository.findById(assessmentId)
                .orElseThrow(() -> new SignalException(ErrorCode.NOT_FOUND));

        if (!riskAssessment.isOwnedBy(userId, anonymousId)) {
            throw new SignalException(ErrorCode.FORBIDDEN);
        }

        return riskAssessment;
    }

    private void validateAnonymousUsage(String anonymousId) {
        if (!StringUtils.hasText(anonymousId)) {
            throw new SignalException(ErrorCode.ANONYMOUS_ID_REQUIRED);
        }
        if (riskAssessmentRepository.countByAnonymousId(anonymousId) >= ANONYMOUS_ASSESSMENT_LIMIT) {
            throw new SignalException(ErrorCode.ANONYMOUS_RISK_ASSESSMENT_LIMIT_EXCEEDED);
        }
    }

    private void validateImage(MultipartFile image) {
        if (image == null || image.isEmpty()) {
            throw new SignalException(ErrorCode.INVALID_INPUT);
        }
        if (image.getSize() > MAX_IMAGE_SIZE) {
            throw new SignalException(ErrorCode.FILE_TOO_LARGE);
        }
        if (!ALLOWED_CONTENT_TYPES.contains(image.getContentType())) {
            throw new SignalException(ErrorCode.UNSUPPORTED_MEDIA_TYPE);
        }
    }
}
