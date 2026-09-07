package com.signal.domain.chat.engine;

import java.util.List;

public interface ChatEngine {

    /**
     * 사용자 메시지를 분석해 응답과 상황 분류, 후속 조치, 위기 감지 여부를 반환한다.
     *
     * @param content 이번에 사용자가 보낸 메시지
     * @param history 같은 세션에서 이전에 오간 대화 (최신 N개, 이번 메시지는 미포함). 룰 기반 구현체는
     *                사용하지 않아도 되지만, LLM 기반 구현체는 대화 맥락 유지를 위해 이를 함께 전달한다.
     */
    ChatEngineResponse respond(String content, List<ChatTurn> history);
}
