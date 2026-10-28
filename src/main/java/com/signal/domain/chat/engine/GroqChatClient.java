package com.signal.domain.chat.engine;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.signal.global.i18n.LanguageContext;
import java.util.ArrayList;
import java.util.HashMap;
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
            - 기본 응답 언어는 한국어 존댓말이다. 이 지시문 끝에 응답 언어 지시가 따로 있으면 그 언어와 정중한 말투를 따른다.
              어느 쪽이든 공감적이고 차분하게 답한다.
            - 2~3문장 이내로 간결하게 답하고, 질문은 한 번에 하나만 한다. 사용자가 한 말을 그대로 되풀이해 요약하지 않는다.
            - 확정적인 법률 자문이나 의학적 진단을 하지 않는다.
            - 사용자를 탓하거나 상황을 축소해서 말하지 않는다.
            - 기관 이름, 전화번호, 웹사이트 주소는 직접 말하지 않는다. 서버가 확인된 정보를 답변 뒤에 따로 덧붙인다.
              번호나 주소를 지어내거나 기억에 의존해 말하는 것은 금지다.
            - 증거 보존(URL·스크린샷), 플랫폼 신고, 전문 기관 연결의 구체적인 절차도 서버가 이어서 안내하므로
              같은 내용을 길게 설명하지 않는다. 대신 공감하고, 상황을 이해하는 데 필요한 질문
              (어디에 올라왔는지, 언제 알게 됐는지, 지금 가장 걱정되는 것이 무엇인지)을 하나 한다.
            - 자해·자살과 관련된 발언은 이 챗봇이 아닌 별도의 안전 로직이 전담해서 처리하므로,
              혹시 그런 발언을 보더라도 절대 자해 방법을 언급하거나 구체적으로 논하지 말고
              "많이 힘드셨겠다"는 공감만 짧게 표현한다.
            """;

    private final RestClient restClient;
    private final String apiKey;
    private final String model;
    private final int maxCompletionTokens;
    private final String reasoningEffort;

    public GroqChatClient(
            RestClient.Builder restClientBuilder,
            @Value("${chat.llm.base-url:https://api.groq.com/openai/v1}") String baseUrl,
            @Value("${chat.llm.api-key:}") String apiKey,
            @Value("${chat.llm.model:openai/gpt-oss-120b}") String model,
            @Value("${chat.llm.max-completion-tokens:1024}") int maxCompletionTokens,
            @Value("${chat.llm.reasoning-effort:low}") String reasoningEffort) {
        this.apiKey = apiKey;
        this.model = model;
        this.maxCompletionTokens = maxCompletionTokens;
        this.reasoningEffort = reasoningEffort;
        this.restClient = restClientBuilder.baseUrl(baseUrl).build();
    }

    @Override
    public String complete(String userMessage, SituationType situationType, List<ChatTurn> history) {
        return request(0.7, buildMessages(userMessage, situationType, history));
    }

    @Override
    public String completeJson(String systemPrompt, String userPrompt) {
        return request(0.0, List.of(
                Map.of("role", "system", "content", systemPrompt),
                Map.of("role", "user", "content", userPrompt)));
    }

    private String request(double temperature, List<Map<String, Object>> messages) {
        if (!StringUtils.hasText(apiKey)) {
            throw new IllegalStateException("GROQ_API_KEY가 설정되지 않았습니다.");
        }

        Map<String, Object> requestBody = new HashMap<>(Map.of(
                "model", model,
                "max_completion_tokens", maxCompletionTokens,
                "temperature", temperature,
                "messages", messages));
        // 추론 모델(gpt-oss 등)만 지원하는 옵션이라, 비워 두면 요청에 넣지 않는다.
        if (StringUtils.hasText(reasoningEffort)) {
            requestBody.put("reasoning_effort", reasoningEffort);
        }

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
        messages.add(Map.of("role", "system", "content",
                SYSTEM_PROMPT + situationHint(situationType) + LanguageContext.current().promptInstruction()));
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
        Choice choice = response.choices().get(0);
        // 토큰 상한에 걸려 잘린 답변은 정상 답변처럼 내보내지 않고 실패로 처리해, 호출부가 룰 기반 응답으로 대체하게 한다.
        if ("length".equals(choice.finishReason())) {
            throw new IllegalStateException("Groq 응답이 토큰 상한(max_completion_tokens)에 걸려 잘렸습니다.");
        }
        Message message = choice.message();
        String content = message == null ? null : message.content();
        if (!StringUtils.hasText(content)) {
            throw new IllegalStateException("Groq 응답 content가 비어 있습니다.");
        }
        return content.trim();
    }

    private record GroqChatResponse(List<Choice> choices) {
    }

    private record Choice(Message message, @JsonProperty("finish_reason") String finishReason) {
    }

    private record Message(String role, String content) {
    }
}
