package com.signal.domain.chat.engine;

public interface ChatEngine {

    /**
     * 사용자 메시지를 분석해 응답과 상황 분류, 후속 조치, 위기 감지 여부를 반환한다.
     * 실제 AI 서버 연동 전까지는 룰 기반 스텁 구현체가 대신한다.
     */
    ChatEngineResponse respond(String content);
}
