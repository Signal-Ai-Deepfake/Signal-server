package com.signal.domain.chat.guidance;

import com.signal.global.i18n.ResponseLanguage;

/** 안내할 상담·신고 기관. 이름은 언어별, 연락처는 공통(번호·주소)이다. */
public record Agency(String id, String ko, String en, String ja, String zh, String contact) {

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
