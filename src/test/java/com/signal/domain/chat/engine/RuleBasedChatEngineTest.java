package com.signal.domain.chat.engine;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class RuleBasedChatEngineTest {

    private final RuleBasedChatEngine chatEngine = new RuleBasedChatEngine();

    @Test
    void 도용_유포_키워드가_있으면_IMAGE_ABUSE로_분류한다() {
        ChatEngineResponse response = chatEngine.respond("제 사진이 도용되어서 다른 사이트에 유포됐어요");

        assertThat(response.situationType()).isEqualTo(SituationType.IMAGE_ABUSE);
        assertThat(response.crisisDetected()).isFalse();
        assertThat(response.recommendedAgencies()).isEmpty();
        assertThat(response.suggestedActions()).isNotEmpty();
    }

    @Test
    void 자해_관련_키워드가_있으면_위기로_감지하고_상담기관을_추천한다() {
        ChatEngineResponse response = chatEngine.respond("너무 힘들어서 자살 생각이 들어요");

        assertThat(response.crisisDetected()).isTrue();
        assertThat(response.recommendedAgencies()).isNotEmpty();
        assertThat(response.suggestedActions()).isNotEmpty();
    }

    @Test
    void 일반_메시지는_GENERAL로_분류되고_위기가_감지되지_않는다() {
        ChatEngineResponse response = chatEngine.respond("요즘 고민이 있어서 얘기하고 싶어요");

        assertThat(response.situationType()).isEqualTo(SituationType.GENERAL);
        assertThat(response.crisisDetected()).isFalse();
        assertThat(response.recommendedAgencies()).isEmpty();
    }

    @Test
    void 도용_유포와_자해_키워드가_동시에_있으면_IMAGE_ABUSE이면서_위기로_감지된다() {
        ChatEngineResponse response = chatEngine.respond("사진이 유포돼서 너무 힘들고 자살하고 싶어요");

        assertThat(response.situationType()).isEqualTo(SituationType.IMAGE_ABUSE);
        assertThat(response.crisisDetected()).isTrue();
        assertThat(response.recommendedAgencies()).isNotEmpty();
    }
}
