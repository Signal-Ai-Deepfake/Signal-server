package com.signal.domain.protection.protector;

import com.signal.domain.protection.entity.ProtectionLevel;

public interface ImageProtector {

    /**
     * 이미지 보호 처리(노이즈 삽입)를 비동기로 수행하고, 완료/실패 결과를 해당 Protection에 반영한다.
     * 실제 AI 서버 연동 전까지는 스텁 구현체가 대신한다.
     */
    void protect(Long protectionId, String originalImageUrl, ProtectionLevel protectionLevel);
}
