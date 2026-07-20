package com.signal.domain.protection.protector;

import com.signal.domain.protection.entity.Protection;
import com.signal.domain.protection.entity.ProtectionLevel;
import com.signal.domain.protection.repository.ProtectionRepository;
import com.signal.global.file.FileStorage;
import java.util.Arrays;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/**
 * 실제 AI 서버 연동 전까지 사용하는 스텁 구현체. 원본 이미지를 그대로 복제해 보호 이미지로 저장하고,
 * 이미지 바이트 해시 기반으로 결정론적인 시각적 차이 값을 산출한다. 실서버 연동 시 이 클래스를 교체한다.
 *
 * ProtectionService(→ImageProtector) 의존 방향의 순환을 피하기 위해 완료/실패 처리 시
 * ProtectionService가 아닌 ProtectionRepository에 직접 접근한다.
 */
@Slf4j
@Component
public class StubImageProtector implements ImageProtector {

    private static final String PROTECTED_IMAGE_DIRECTORY = "protections";

    private final FileStorage fileStorage;
    private final ProtectionRepository protectionRepository;
    private final long processingDelayMs;

    public StubImageProtector(FileStorage fileStorage,
                               ProtectionRepository protectionRepository,
                               @Value("${protection.processing-delay-ms:3000}") long processingDelayMs) {
        this.fileStorage = fileStorage;
        this.protectionRepository = protectionRepository;
        this.processingDelayMs = processingDelayMs;
    }

    @Override
    @Async
    public void protect(Long protectionId, String originalImageUrl, ProtectionLevel protectionLevel) {
        try {
            Thread.sleep(processingDelayMs);

            byte[] originalBytes = fileStorage.load(originalImageUrl);
            String protectedImageUrl = fileStorage.store(originalBytes, "protected.png", PROTECTED_IMAGE_DIRECTORY);
            double visualDifference = calculateVisualDifference(originalBytes, protectionLevel);

            markCompleted(protectionId, protectedImageUrl, visualDifference);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            markFailed(protectionId);
        } catch (Exception e) {
            log.error("이미지 보호 처리 실패: protectionId={}", protectionId, e);
            markFailed(protectionId);
        }
    }

    private double calculateVisualDifference(byte[] content, ProtectionLevel protectionLevel) {
        double base = switch (protectionLevel) {
            case LIGHT -> 0.01;
            case NORMAL -> 0.03;
            case STRONG -> 0.06;
        };
        int hash = Math.abs(Arrays.hashCode(content));
        double jitter = (hash % 100) / 1000.0;
        return Math.round((base + jitter) * 1000) / 1000.0;
    }

    private void markCompleted(Long protectionId, String protectedImageUrl, double visualDifference) {
        protectionRepository.findById(protectionId).ifPresent(protection -> {
            protection.complete(protectedImageUrl, visualDifference);
            protectionRepository.save(protection);
        });
    }

    private void markFailed(Long protectionId) {
        protectionRepository.findById(protectionId).ifPresent(protection -> {
            protection.fail();
            protectionRepository.save(protection);
        });
    }
}
