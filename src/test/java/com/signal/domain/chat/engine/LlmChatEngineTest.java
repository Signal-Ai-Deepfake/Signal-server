package com.signal.domain.chat.engine;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class LlmChatEngineTest {

    @Mock
    private ChatCompletionClient chatCompletionClient;

    @Test
    void 일반_대화는_LLM이_생성한_답변을_그대로_사용한다() {
        LlmChatEngine engine = new LlmChatEngine(chatCompletionClient, true);
        when(chatCompletionClient.complete("안녕하세요", SituationType.GENERAL, List.of()))
                .thenReturn("안녕하세요! 어떤 걱정이 있으신가요?");

        ChatEngineResponse response = engine.respond("안녕하세요", List.of());

        assertThat(response.reply()).isEqualTo("안녕하세요! 어떤 걱정이 있으신가요?");
        assertThat(response.situationType()).isEqualTo(SituationType.GENERAL);
        assertThat(response.crisisDetected()).isFalse();
    }

    @Test
    void 이전_대화_기록을_LLM_호출에_그대로_전달한다() {
        LlmChatEngine engine = new LlmChatEngine(chatCompletionClient, true);
        List<ChatTurn> history = List.of(
                new ChatTurn(ChatSpeaker.USER, "제 사진이 유포된 것 같아요"),
                new ChatTurn(ChatSpeaker.BOT, "많이 놀라셨겠어요. 어디에 유포됐는지 아시나요?"));
        when(chatCompletionClient.complete(eq("인스타그램에요"), eq(SituationType.IMAGE_ABUSE), eq(history)))
                .thenReturn("인스타그램이군요. 신고 절차를 안내해드릴게요.");

        ChatEngineResponse response = engine.respond("인스타그램에요", history);

        assertThat(response.reply()).isEqualTo("인스타그램이군요. 신고 절차를 안내해드릴게요.");
    }

    @Test
    void 위기_키워드가_있으면_LLM을_호출하지_않고_고정된_안전_문구로_응답한다() {
        LlmChatEngine engine = new LlmChatEngine(chatCompletionClient, true);

        ChatEngineResponse response = engine.respond("너무 힘들어서 자살 생각이 들어요", List.of());

        assertThat(response.crisisDetected()).isTrue();
        assertThat(response.recommendedAgencies()).isNotEmpty();
        verify(chatCompletionClient, never()).complete(anyString(), any(), any());
    }

    @Test
    void LLM_호출이_실패하면_룰_기반_응답으로_대체한다() {
        LlmChatEngine engine = new LlmChatEngine(chatCompletionClient, true);
        when(chatCompletionClient.complete(anyString(), any(), any()))
                .thenThrow(new IllegalStateException("GROQ_API_KEY가 설정되지 않았습니다."));

        ChatEngineResponse response = engine.respond("사진이 유포됐어요", List.of());

        assertThat(response.situationType()).isEqualTo(SituationType.IMAGE_ABUSE);
        assertThat(response.reply()).isEqualTo("이미지 도용·유포 피해는 신속한 대응이 중요해요. 아래 조치를 참고해주세요.");
        assertThat(response.suggestedActions()).isNotEmpty();
    }

    @Test
    void llm_비활성화면_LLM을_호출하지_않고_룰_기반_응답만_사용한다() {
        LlmChatEngine engine = new LlmChatEngine(chatCompletionClient, false);

        ChatEngineResponse response = engine.respond("안녕하세요", List.of());

        assertThat(response.reply()).isEqualTo("말씀해주셔서 감사해요. 조금 더 자세히 상황을 알려주시면 도와드릴게요.");
        verify(chatCompletionClient, never()).complete(anyString(), any(), any());
    }
}
