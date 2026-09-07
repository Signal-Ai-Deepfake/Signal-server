package com.signal.domain.chat.engine;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class GroqChatClientTest {

    private static final String BASE_URL = "https://api.groq.com/openai/v1";

    @Test
    void 정상_응답이면_choices의_첫_메시지_content를_반환한다() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        GroqChatClient client = new GroqChatClient(builder, BASE_URL, "test-key", "openai/gpt-oss-120b", 400);

        server.expect(requestTo(BASE_URL + "/chat/completions"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("Authorization", "Bearer test-key"))
                .andExpect(jsonPath("$.model").value("openai/gpt-oss-120b"))
                .andExpect(jsonPath("$.max_completion_tokens").value(400))
                .andExpect(jsonPath("$.messages[1].role").value("user"))
                .andExpect(jsonPath("$.messages[1].content").value("안녕하세요"))
                .andRespond(withSuccess(
                        """
                        {"choices":[{"message":{"role":"assistant","content":"안녕하세요, 무엇을 도와드릴까요?"}}]}
                        """,
                        MediaType.APPLICATION_JSON));

        String reply = client.complete("안녕하세요", SituationType.GENERAL, List.of());

        assertThat(reply).isEqualTo("안녕하세요, 무엇을 도와드릴까요?");
        server.verify();
    }

    @Test
    void 이전_대화_기록이_있으면_system_메시지_뒤에_순서대로_끼워넣는다() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        GroqChatClient client = new GroqChatClient(builder, BASE_URL, "test-key", "openai/gpt-oss-120b", 400);
        List<ChatTurn> history = List.of(
                new ChatTurn(ChatSpeaker.USER, "제 사진이 유포된 것 같아요"),
                new ChatTurn(ChatSpeaker.BOT, "많이 놀라셨겠어요. 어디에 유포됐는지 아시나요?"));

        server.expect(requestTo(BASE_URL + "/chat/completions"))
                .andExpect(jsonPath("$.messages[1].role").value("user"))
                .andExpect(jsonPath("$.messages[1].content").value("제 사진이 유포된 것 같아요"))
                .andExpect(jsonPath("$.messages[2].role").value("assistant"))
                .andExpect(jsonPath("$.messages[2].content").value("많이 놀라셨겠어요. 어디에 유포됐는지 아시나요?"))
                .andExpect(jsonPath("$.messages[3].role").value("user"))
                .andExpect(jsonPath("$.messages[3].content").value("인스타그램에요"))
                .andRespond(withSuccess(
                        """
                        {"choices":[{"message":{"role":"assistant","content":"인스타그램이군요."}}]}
                        """,
                        MediaType.APPLICATION_JSON));

        String reply = client.complete("인스타그램에요", SituationType.IMAGE_ABUSE, history);

        assertThat(reply).isEqualTo("인스타그램이군요.");
        server.verify();
    }

    @Test
    void API_키가_비어있으면_호출하지_않고_예외를_던진다() {
        RestClient.Builder builder = RestClient.builder();
        GroqChatClient client = new GroqChatClient(builder, BASE_URL, "", "openai/gpt-oss-120b", 400);

        assertThatThrownBy(() -> client.complete("안녕", SituationType.GENERAL, List.of()))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void choices가_비어있으면_예외를_던진다() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        GroqChatClient client = new GroqChatClient(builder, BASE_URL, "test-key", "openai/gpt-oss-120b", 400);

        server.expect(requestTo(BASE_URL + "/chat/completions"))
                .andRespond(withSuccess("""
                        {"choices":[]}
                        """, MediaType.APPLICATION_JSON));

        assertThatThrownBy(() -> client.complete("안녕", SituationType.GENERAL, List.of()))
                .isInstanceOf(IllegalStateException.class);
    }
}
