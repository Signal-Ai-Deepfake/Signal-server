package com.signal.domain.deepfakedetection.service;

import com.signal.domain.deepfakedetection.detector.DeepfakeDetector;
import com.signal.domain.deepfakedetection.entity.DeepfakeDetection;
import com.signal.domain.deepfakedetection.repository.DeepfakeDetectionRepository;
import com.signal.global.exception.ErrorCode;
import com.signal.global.exception.SignalException;
import com.signal.global.file.FileStorage;
import com.signal.global.util.TransactionUtils;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.util.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DeepfakeDetectionService {

    private static final long MAX_FILE_SIZE = 100L * 1024 * 1024;
    private static final Set<String> VIDEO_CONTENT_TYPES = Set.of("video/mp4", "video/quicktime");
    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of(
            "image/jpeg", "image/png", "video/mp4", "video/quicktime");
    private static final String FILE_DIRECTORY = "deepfake-detections";
    private static final long ANONYMOUS_DETECTION_LIMIT = 5;

    private final DeepfakeDetectionRepository deepfakeDetectionRepository;
    private final DeepfakeDetector deepfakeDetector;
    private final FileStorage fileStorage;

    @Transactional
    public DeepfakeDetection createDetection(Long userId, String anonymousId, MultipartFile file) {
        validateFile(file);
        if (userId == null) {
            validateAnonymousUsage(anonymousId);
        }
        boolean isVideo = VIDEO_CONTENT_TYPES.contains(file.getContentType());

        String fileUrl = fileStorage.store(file, FILE_DIRECTORY);

        DeepfakeDetection detection = DeepfakeDetection.builder()
                .userId(userId)
                .anonymousId(userId == null ? anonymousId : null)
                .fileUrl(fileUrl)
                .build();
        DeepfakeDetection saved = deepfakeDetectionRepository.save(detection);

        TransactionUtils.runAfterCommit(() -> deepfakeDetector.detect(saved.getId(), fileUrl, isVideo));

        return saved;
    }

    public DeepfakeDetection getDetection(Long userId, String anonymousId, Long detectionId) {
        DeepfakeDetection detection = deepfakeDetectionRepository.findById(detectionId)
                .orElseThrow(() -> new SignalException(ErrorCode.NOT_FOUND));

        if (!detection.isOwnedBy(userId, anonymousId)) {
            throw new SignalException(ErrorCode.FORBIDDEN);
        }

        return detection;
    }

    private void validateAnonymousUsage(String anonymousId) {
        if (!StringUtils.hasText(anonymousId)) {
            throw new SignalException(ErrorCode.ANONYMOUS_ID_REQUIRED);
        }
        if (deepfakeDetectionRepository.countByAnonymousId(anonymousId) >= ANONYMOUS_DETECTION_LIMIT) {
            throw new SignalException(ErrorCode.ANONYMOUS_DETECTION_LIMIT_EXCEEDED);
        }
    }

    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new SignalException(ErrorCode.INVALID_INPUT);
        }
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new SignalException(ErrorCode.FILE_TOO_LARGE);
        }
        if (!ALLOWED_CONTENT_TYPES.contains(file.getContentType())) {
            throw new SignalException(ErrorCode.UNSUPPORTED_MEDIA_TYPE);
        }
    }
}
