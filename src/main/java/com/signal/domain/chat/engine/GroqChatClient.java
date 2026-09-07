package com.signal.domain.chat.engine;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;

/**
 * Groq(GroqCloud)의 OpenAI 호환 Chat Completions API를 호출하는 클라이언트.
 * 별도 SDK 없이 REST 호출만으로 동작하며, 무료 티어를 제공하는 Groq를 기본 LLM 제공자로 사용한다.
 *
 * 필요 설정: 환경변수 GROQ_API_KEY (https://console.groq.com 에서 발급, 카드 등록 없이 무료 티어 사용 가능).
 * API 문서: https://console.groq.com/docs/api-reference
 *
 * 참고: 2026-08-16부로 llama-3.3-70b-versatile / llama-3.1-8b-instant 등 기존 Llama 모델들이
 * Groq에서 순차 폐기(deprecate)되어, 현재는 openai/gpt-oss-120b(범용 기본값) 등으로 대체되었다.
 * 모델 목록은 chat.llm.model 설정으로 자유롭게 교체 가능.
 *
 * 같은 세션의 이전 대화(history)를 system 메시지 뒤·현재 메시지 앞에 user/assistant 턴으로 순서대로
 * 끼워 넣어 멀티턴 맥락을 유지한다 (몇 개까지 넘길지는 호출부인 ChatService가 결정).
 */
@Component
public class GroqChatClient implements ChatCompletionClient {

    private static final String SYSTEM_PROMPT = """
            당신은 딥페이크 피해 예방·탐지·대응 서비스 'Signal'의 상담 챗봇입니다.
            사용자는 딥페이크·이미지 합성·도용·유포 피해를 겪었거나, 겪을까 봐 걱정하는 사람일 수 있습니다.

            다음 원칙을 반드시 지켜 답하세요.
            - 항상 한국어 존댓말로, 공감적이고 차분하게 답한다.
            - 2~4문장 이내로 간결하게 답한다. 불필요하게 길게 설명하지 않는다.
            - 확정적인 법률 자문이나 의학적 진단을 하지 않는다. 필요하면 전문 기관 상담을 권유하는 정도로만 안내한다.
            - 사용자를 탓하거나 상황을 축소해서 말하지 않는다.
            - 자해·자살과 관련된 발언은 이 챗봇이 아닌 별도의 안전 로직이 전담해서 처리하므로,
              혹시 그런 발언을 보더라도 절대 자해 방법을 언급하거나 구체적으로 논하지 말고
              "많이 힘드셨겠다"는 공감만 짧게 표현한다.
            - 상황 분류가 IMAGE_ABUSE라면 증거 보존(URL·스크린샷), 플랫폼 신고, 필요 시 사이버수사대 신고 등
              구체적인 다음 행동을 자연스럽게 언급해도 좋다.
            """;

    private final RestClient restClient;
    private final String apiKey;
    private final String model;
    private final int maxCompletionTokens;

    public GroqChatClient(
            RestClient.Builder restClientBuilder,
            @Value("${chat.llm.base-url:https://api.groq.com/openai/v1}") String baseUrl,
            @Value("${chat.llm.api-key:}") String apiKey,
            @Value("${chat.llm.model:openai/gpt-oss-120b}") String model,
            @Value("${chat.llm.max-completion-tokens:400}") int maxCompletionTokens) {
        this.apiKey = apiKey;
        this.model = model;
        this.maxCompletionTokens = maxCompletionTokens;
        this.restClient = restClientBuilder.baseUrl(baseUrl).build();
    }

    @Override
    public String complete(String userMessage, SituationType situationType, List<ChatTurn> history) {
        if (!StringUtils.hasText(apiKey)) {
            throw new IllegalStateException("GROQ_API_KEY가 설정되지 않았습니다.");
        }

        Map<String, Object> requestBody = Map.of(
                "model", model,
                "max_completion_tokens", maxCompletionTokens,
                "temperature", 0.7,
                "messages", buildMessages(userMessage, situationType, history));

        GroqChatResponse response = restClient.post()
                .uri("/chat/completions")
                .header("Authorization", "Bearer " + apiKey)
                .contentType(MediaType.APPLICATION_JSON)
                .body(requestBody)
                .retrieve()
                .body(GroqChatResponse.class);

        return extractReply(response);
    }

    private List<Map<String, Object>> buildMessages(
            String userMessage, SituationType situationType, List<ChatTurn> history) {
        List<Map<String, Object>> messages = new ArrayList<>();
        messages.add(Map.of("role", "system", "content", SYSTEM_PROMPT + situationHint(situationType)));
        if (history != null) {
            for (ChatTurn turn : history) {
                String role = turn.speaker() == ChatSpeaker.USER ? "user" : "assistant";
                messages.add(Map.of("role", role, "content", turn.content()));
            }
        }
        messages.add(Map.of("role", "user", "content", userMessage));
        return messages;
    }

    private String situationHint(SituationType situationType) {
        return situationType == SituationType.IMAGE_ABUSE
                ? "\n\n[현재 상황 분류: IMAGE_ABUSE - 이미지 도용/유포 피해 관련 대화]"
                : "\n\n[현재 상황 분류: GENERAL - 일반 상담]";
    }

    private String extractReply(GroqChatResponse response) {
        if (response == null || response.choices() == null || response.choices().isEmpty()) {
            throw new IllegalStateException("Groq 응답에 choices가 없습니다.");
        }
        Message message = response.choices().get(0).message();
        String content = message == null ? null : message.content();
        if (!StringUtils.hasText(content)) {
            throw new IllegalStateException("Groq 응답 content가 비어 있습니다.");
        }
        return content.trim();
    }

    private record GroqChatResponse(List<Choice> choices) {
    }

    private record Choice(Message message) {
    }

    private record Message(String role, String content) {
    }
}
