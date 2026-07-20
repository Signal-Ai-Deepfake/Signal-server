package com.signal.domain.monitoring.service;

import com.signal.domain.monitoring.entity.Detection;
import com.signal.domain.monitoring.entity.Monitoring;
import com.signal.domain.monitoring.monitor.FaceMonitor;
import com.signal.domain.monitoring.repository.DetectionRepository;
import com.signal.domain.monitoring.repository.MonitoringRepository;
import com.signal.global.exception.ErrorCode;
import com.signal.global.exception.SignalException;
import com.signal.global.file.FileStorage;
import com.signal.global.util.TransactionUtils;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MonitoringService {

    private static final long MAX_IMAGE_SIZE = 10L * 1024 * 1024;
    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of("image/jpeg", "image/png");
    private static final String REFERENCE_IMAGE_DIRECTORY = "monitorings";

    private final MonitoringRepository monitoringRepository;
    private final DetectionRepository detectionRepository;
    private final FaceMonitor faceMonitor;
    private final FileStorage fileStorage;

    @Transactional
    public Monitoring createMonitoring(Long userId, MultipartFile referenceImage) {
        validateImage(referenceImage);

        String referenceImageUrl = fileStorage.store(referenceImage, REFERENCE_IMAGE_DIRECTORY);

        Monitoring monitoring = Monitoring.builder()
                .userId(userId)
                .referenceImageUrl(referenceImageUrl)
                .build();
        Monitoring saved = monitoringRepository.save(monitoring);

        TransactionUtils.runAfterCommit(() -> faceMonitor.startMonitoring(saved.getId(), referenceImageUrl));

        return saved;
    }

    public Page<Detection> getDetections(Long userId, Long monitoringId, Pageable pageable) {
        getMonitoring(userId, monitoringId);
        return detectionRepository.findByMonitoringIdOrderByDetectedAtDesc(monitoringId, pageable);
    }

    @Transactional
    public void deleteMonitoring(Long userId, Long monitoringId) {
        Monitoring monitoring = getMonitoring(userId, monitoringId);
        detectionRepository.deleteAllByMonitoringId(monitoringId);
        monitoringRepository.delete(monitoring);
    }

    private Monitoring getMonitoring(Long userId, Long monitoringId) {
        Monitoring monitoring = monitoringRepository.findById(monitoringId)
                .orElseThrow(() -> new SignalException(ErrorCode.NOT_FOUND));

        if (!monitoring.isOwnedBy(userId)) {
            throw new SignalException(ErrorCode.FORBIDDEN);
        }

        return monitoring;
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
