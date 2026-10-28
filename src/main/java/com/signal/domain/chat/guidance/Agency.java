package com.signal.domain.chat.guidance;

import com.signal.global.i18n.ResponseLanguage;
import java.util.List;

/**
 * 안내할 상담·신고 기관. 이름은 언어별, 연락처는 공통이다.
 * contact는 사용자에게 보이는 문구, phones는 전화번호 원문이다(기관 조회 API 시드와 비교할 때 쓴다).
 */
public record Agency(String id, String ko, String en, String ja, String zh, String contact, List<String> phones) {

    public String format(ResponseLanguage language) {
        String name = switch (language) {
            case EN -> en;
            case JA -> ja;
            case ZH_CN -> zh;
            default -> ko;
        };
        return name + " (" + contact + ")";
    }
}
