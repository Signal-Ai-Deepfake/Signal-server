package com.signal.global.ai;

import java.util.Base64;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;

/**
 * Groq(GroqCloud)의 OpenAI 호환 비전(이미지 입력) Chat Completions API를 호출하는 클라이언트.
 * {@link com.signal.domain.chat.engine.GroqChatClient}와 같은 Groq 계정(GROQ_API_KEY)을 쓰되,
 * 모델만 비전 지원 모델(vision.llm.model)로 별도 설정한다.
 *
 * 참고: 2026-09 기준 Groq에서 이미지 입력을 지원하는 모델은 qwen/qwen3.6-27b, qwen/qwen3.8-27b
 * 정도이며(둘 다 Preview), 과거 비전을 지원하던 Llama 4 Scout/Maverick 계열은 이미 폐기되었다.
 * https://console.groq.com/docs/vision
 */
@Component
public class GroqVisionClient implements VisionCompletionClient {

    private final RestClient restClient;
    private final String apiKey;
    private final String model;
    private final int maxCompletionTokens;

    public GroqVisionClient(
            RestClient.Builder restClientBuilder,
            @Value("${chat.llm.base-url:https://api.groq.com/openai/v1}") String baseUrl,
            @Value("${chat.llm.api-key:}") String apiKey,
            @Value("${vision.llm.model:qwen/qwen3.6-27b}") String model,
            @Value("${vision.llm.max-completion-tokens:1024}") int maxCompletionTokens) {
        this.apiKey = apiKey;
        this.model = model;
        this.maxCompletionTokens = maxCompletionTokens;
        this.restClient = restClientBuilder.baseUrl(baseUrl).build();
    }

    @Override
    public String complete(byte[] imageBytes, String mimeType, String systemPrompt, String userPrompt) {
        if (!StringUtils.hasText(apiKey)) {
            throw new IllegalStateException("GROQ_API_KEY가 설정되지 않았습니다.");
        }

        String effectiveMimeType = StringUtils.hasText(mimeType) ? mimeType : "image/jpeg";
        String base64Image = Base64.getEncoder().encodeToString(imageBytes);
        String dataUri = "data:" + effectiveMimeType + ";base64," + base64Image;

        Map<String, Object> requestBody = Map.of(
                "model", model,
                "max_completion_tokens", maxCompletionTokens,
                "temperature", 0.3,
                "messages", List.of(
                        Map.of("role", "system", "content", systemPrompt),
                        Map.of("role", "user", "content", List.of(
                                Map.of("type", "text", "text", userPrompt),
                                Map.of("type", "image_url", "image_url", Map.of("url", dataUri))))));

        GroqVisionResponse response = restClient.post()
                .uri("/chat/completions")
                .header("Authorization", "Bearer " + apiKey)
                .contentType(MediaType.APPLICATION_JSON)
                .body(requestBody)
                .retrieve()
                .body(GroqVisionResponse.class);

        return extractReply(response);
    }

    private String extractReply(GroqVisionResponse response) {
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

    private record GroqVisionResponse(List<Choice> choices) {
    }

    private record Choice(Message message) {
    }

    private record Message(String role, String content) {
    }
}
