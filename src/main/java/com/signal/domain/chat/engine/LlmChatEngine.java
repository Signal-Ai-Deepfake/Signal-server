package com.signal.domain.chat.engine;

import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * 실제 LLM({@link ChatCompletionClient}, 기본 구현은 Groq)을 호출해 응답을 생성하는 챗봇 엔진.
 * ChatEngine의 유일한 Spring 빈으로, 기존 RuleBasedChatEngine을 대체한다.
 *
 * 안전을 위한 역할 분담:
 * - 위기(자해·자살 등) 신호 감지, 상황 분류(GENERAL/IMAGE_ABUSE), 추천 행동, 추천 상담기관은
 *   LLM의 환각/누락 위험을 배제하기 위해 계속 결정론적인 {@link RuleBasedChatEngine}이 산출한다.
 * - 위기 상황일 때는 LLM을 아예 호출하지 않고 검증된 고정 문구로만 응답한다.
 * - 그 외 일반 대화의 답변 텍스트(reply)만 LLM이 생성한다. 이때 같은 세션의 이전 대화(history)도 함께
 *   넘겨 LLM이 맥락을 이어서 답할 수 있게 한다.
 * - GROQ_API_KEY 미설정(chat.llm.api-key 비어있음), chat.llm.enabled=false, 또는 API 호출 실패(타임아웃,
 *   네트워크 오류, 빈 응답 등) 시에는 예외를 삼키고 룰 기반 응답으로 안전하게 대체하여 챗봇 자체가 죽지 않게 한다.
 */
@Slf4j
@Component
public class LlmChatEngine implements ChatEngine {

    private final RuleBasedChatEngine ruleBasedChatEngine = new RuleBasedChatEngine();
    private final ChatCompletionClient chatCompletionClient;
    private final boolean llmEnabled;

    public LlmChatEngine(ChatCompletionClient chatCompletionClient,
                          @Value("${chat.llm.enabled:true}") boolean llmEnabled) {
        this.chatCompletionClient = chatCompletionClient;
        this.llmEnabled = llmEnabled;
    }

    @Override
    public ChatEngineResponse respond(String content, List<ChatTurn> history) {
        ChatEngineResponse ruleBasedResponse = ruleBasedChatEngine.respond(content, history);

        if (!llmEnabled || ruleBasedResponse.crisisDetected()) {
            return ruleBasedResponse;
        }

        try {
            String llmReply = chatCompletionClient.complete(content, ruleBasedResponse.situationType(), history);
            return new ChatEngineResponse(
                    llmReply,
                    ruleBasedResponse.situationType(),
                    ruleBasedResponse.suggestedActions(),
                    false,
                    List.of());
        } catch (Exception e) {
            log.warn("LLM 챗봇 응답 생성 실패, 룰 기반 응답으로 대체합니다.", e);
            return ruleBasedResponse;
        }
    }
}
