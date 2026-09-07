package com.signal.global.ai;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class GroqVisionClientTest {

    private static final String BASE_URL = "https://api.groq.com/openai/v1";

    @Test
    void 정상_응답이면_choices의_첫_메시지_content를_반환한다() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        GroqVisionClient client = new GroqVisionClient(builder, BASE_URL, "test-key", "qwen/qwen3.6-27b", 1024);

        server.expect(requestTo(BASE_URL + "/chat/completions"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("Authorization", "Bearer test-key"))
                .andExpect(jsonPath("$.model").value("qwen/qwen3.6-27b"))
                .andExpect(jsonPath("$.messages[1].content[0].type").value("text"))
                .andExpect(jsonPath("$.messages[1].content[1].type").value("image_url"))
                .andExpect(jsonPath("$.messages[1].content[1].image_url.url").value(
                        org.hamcrest.Matchers.startsWith("data:image/png;base64,")))
                .andRespond(withSuccess(
                        """
                        {"choices":[{"message":{"role":"assistant","content":"{\\"faceDetected\\":true}"}}]}
                        """,
                        MediaType.APPLICATION_JSON));

        String reply = client.complete(new byte[]{1, 2, 3, 4}, "image/png", "system prompt", "user prompt");

        assertThat(reply).isEqualTo("{\"faceDetected\":true}");
        server.verify();
    }

    @Test
    void API_키가_비어있으면_호출하지_않고_예외를_던진다() {
        RestClient.Builder builder = RestClient.builder();
        GroqVisionClient client = new GroqVisionClient(builder, BASE_URL, "", "qwen/qwen3.6-27b", 1024);

        assertThatThrownBy(() -> client.complete(new byte[]{1, 2, 3}, "image/png", "sys", "user"))
                .isInstanceOf(IllegalStateException.class);
    }
}
