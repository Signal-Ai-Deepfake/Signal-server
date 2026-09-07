package com.signal.domain.chat.engine;

import java.util.List;

/**
 * 실제 LLM 제공자를 호출해 한 턴의 대화 응답 텍스트를 생성하는 클라이언트.
 * 제공자를 교체하려면(Groq → 다른 서비스 등) 이 인터페이스의 구현체만 바꾸면 된다.
 */
public interface ChatCompletionClient {

    /**
     * 사용자 메시지에 대한 LLM 응답 텍스트를 생성한다.
     *
     * @param userMessage   사용자가 보낸 메시지 원문
     * @param situationType 룰 기반 로직이 먼저 분류한 상황(GENERAL/IMAGE_ABUSE) — 프롬프트 힌트로 사용
     * @param history       같은 세션에서 이전에 오간 대화(이번 메시지는 미포함) — LLM이 맥락을 이어가도록 함께 전달
     * @return LLM이 생성한 응답 텍스트
     * @throws RuntimeException API 키 미설정, 네트워크 오류, 타임아웃, 빈 응답 등 실패 시 (호출부에서 폴백 처리)
     */
    String complete(String userMessage, SituationType situationType, List<ChatTurn> history);
}
