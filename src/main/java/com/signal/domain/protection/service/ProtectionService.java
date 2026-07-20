package com.signal.domain.protection.service;

import com.signal.domain.protection.entity.Protection;
import com.signal.domain.protection.entity.ProtectionLevel;
import com.signal.domain.protection.entity.ProtectionStatus;
import com.signal.domain.protection.protector.ImageProtector;
import com.signal.domain.protection.repository.ProtectionRepository;
import com.signal.domain.riskassessment.entity.RiskAssessment;
import com.signal.domain.riskassessment.service.RiskAssessmentService;
import com.signal.global.exception.ErrorCode;
import com.signal.global.exception.SignalException;
import com.signal.global.file.FileStorage;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProtectionService {

    private final ProtectionRepository protectionRepository;
    private final RiskAssessmentService riskAssessmentService;
    private final ImageProtector imageProtector;
    private final FileStorage fileStorage;

    @Transactional
    public Protection createProtection(Long userId, Long assessmentId, ProtectionLevel protectionLevel) {
        RiskAssessment riskAssessment = riskAssessmentService.getAssessment(userId, assessmentId);
        ProtectionLevel level = protectionLevel != null ? protectionLevel : ProtectionLevel.NORMAL;

        Protection protection = Protection.builder()
                .userId(userId)
                .assessmentId(assessmentId)
                .protectionLevel(level)
                .originalImageUrl(riskAssessment.getImageUrl())
                .build();
        Protection saved = protectionRepository.save(protection);

        dispatchAfterCommit(() -> imageProtector.protect(saved.getId(), saved.getOriginalImageUrl(), level));

        return saved;
    }

    /**
     * 트랜잭션 커밋 전에 비동기 처리가 시작되면, 처리 스레드가 아직 커밋되지 않은
     * Protection을 조회하지 못해 결과가 조용히 버려질 수 있다. 커밋 완료 후에 시작되도록 보장한다.
     * 트랜잭션이 없는 컨텍스트(단위 테스트 등)에서는 즉시 실행한다.
     */
    private void dispatchAfterCommit(Runnable task) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            task.run();
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                task.run();
            }
        });
    }

    public Protection getProtection(Long userId, Long protectionId) {
        Protection protection = protectionRepository.findById(protectionId)
                .orElseThrow(() -> new SignalException(ErrorCode.NOT_FOUND));

        if (!protection.isOwnedBy(userId)) {
            throw new SignalException(ErrorCode.FORBIDDEN);
        }

        return protection;
    }

    public byte[] downloadProtectedImage(Long userId, Long protectionId) {
        Protection protection = getProtection(userId, protectionId);

        if (protection.getStatus() != ProtectionStatus.COMPLETED) {
            throw new SignalException(ErrorCode.PROTECTION_NOT_READY);
        }

        return fileStorage.load(protection.getProtectedImageUrl());
    }
}
