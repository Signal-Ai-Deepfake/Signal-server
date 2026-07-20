package com.signal.domain.protection.protector;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.signal.domain.protection.entity.Protection;
import com.signal.domain.protection.entity.ProtectionLevel;
import com.signal.domain.protection.entity.ProtectionStatus;
import com.signal.domain.protection.repository.ProtectionRepository;
import com.signal.global.exception.ErrorCode;
import com.signal.global.exception.SignalException;
import com.signal.global.file.FileStorage;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class StubImageProtectorTest {

    @Mock
    private FileStorage fileStorage;

    @Mock
    private ProtectionRepository protectionRepository;

    private StubImageProtector stubImageProtector;

    @BeforeEach
    void setUp() {
        stubImageProtector = new StubImageProtector(fileStorage, protectionRepository, 0);
    }

    private Protection sampleProtection() {
        return Protection.builder()
                .userId(1L)
                .assessmentId(10L)
                .protectionLevel(ProtectionLevel.NORMAL)
                .originalImageUrl("/uploads/risk-assessments/original.png")
                .build();
    }

    @Test
    void 처리에_성공하면_보호이미지_URL과_시각적_차이를_반영하고_저장한다() {
        Protection protection = sampleProtection();
        when(protectionRepository.findById(1L)).thenReturn(Optional.of(protection));
        when(fileStorage.load("/uploads/risk-assessments/original.png")).thenReturn(new byte[]{1, 2, 3});
        when(fileStorage.store(any(byte[].class), anyString(), anyString())).thenReturn("/uploads/protections/generated.png");

        stubImageProtector.protect(1L, "/uploads/risk-assessments/original.png", ProtectionLevel.NORMAL);

        assertThat(protection.getStatus()).isEqualTo(ProtectionStatus.COMPLETED);
        assertThat(protection.getProtectedImageUrl()).isEqualTo("/uploads/protections/generated.png");
        assertThat(protection.getVisualDifference()).isNotNull();
        verify(protectionRepository).save(protection);
    }

    @Test
    void 원본이미지_로드에_실패하면_FAILED로_저장한다() {
        Protection protection = sampleProtection();
        when(protectionRepository.findById(1L)).thenReturn(Optional.of(protection));
        when(fileStorage.load(anyString())).thenThrow(new SignalException(ErrorCode.NOT_FOUND));

        stubImageProtector.protect(1L, "/uploads/risk-assessments/original.png", ProtectionLevel.NORMAL);

        assertThat(protection.getStatus()).isEqualTo(ProtectionStatus.FAILED);
        verify(protectionRepository).save(protection);
    }

    @Test
    void 대상_Protection이_존재하지_않으면_아무것도_하지_않는다() {
        when(protectionRepository.findById(1L)).thenReturn(Optional.empty());
        when(fileStorage.load(anyString())).thenReturn(new byte[]{1, 2, 3});
        when(fileStorage.store(any(byte[].class), anyString(), anyString())).thenReturn("/uploads/protections/generated.png");

        stubImageProtector.protect(1L, "/uploads/risk-assessments/original.png", ProtectionLevel.NORMAL);

        verify(protectionRepository, org.mockito.Mockito.never()).save(any());
    }
}
