package com.signal.domain.chat.engine;

import static org.assertj.core.api.Assertions.assertThat;

import com.signal.global.i18n.LanguageContext;
import com.signal.global.i18n.ResponseLanguage;
import java.util.List;
import org.junit.jupiter.api.Test;

class RuleBasedChatEngineTest {

    private final RuleBasedChatEngine chatEngine = new RuleBasedChatEngine();

    @Test
    void 도용_유포_키워드가_있으면_IMAGE_ABUSE로_분류한다() {
        ChatEngineResponse response = chatEngine.respond("제 사진이 도용되어서 다른 사이트에 유포됐어요", List.of());

        assertThat(response.situationType()).isEqualTo(SituationType.IMAGE_ABUSE);
        assertThat(response.crisisDetected()).isFalse();
        assertThat(response.recommendedAgencies()).isEmpty();
        assertThat(response.suggestedActions()).isNotEmpty();
    }

    @Test
    void 자해_관련_키워드가_있으면_위기로_감지하고_상담기관을_추천한다() {
        ChatEngineResponse response = chatEngine.respond("너무 힘들어서 자살 생각이 들어요", List.of());

        assertThat(response.crisisDetected()).isTrue();
        assertThat(response.recommendedAgencies()).isNotEmpty();
        assertThat(response.suggestedActions()).isNotEmpty();
    }

    @Test
    void 일반_메시지는_GENERAL로_분류되고_위기가_감지되지_않는다() {
        ChatEngineResponse response = chatEngine.respond("요즘 고민이 있어서 얘기하고 싶어요", List.of());

        assertThat(response.situationType()).isEqualTo(SituationType.GENERAL);
        assertThat(response.crisisDetected()).isFalse();
        assertThat(response.recommendedAgencies()).isEmpty();
    }

    @Test
    void 도용_유포와_자해_키워드가_동시에_있으면_IMAGE_ABUSE이면서_위기로_감지된다() {
        ChatEngineResponse response = chatEngine.respond("사진이 유포돼서 너무 힘들고 자살하고 싶어요", List.of());

        assertThat(response.situationType()).isEqualTo(SituationType.IMAGE_ABUSE);
        assertThat(response.crisisDetected()).isTrue();
        assertThat(response.recommendedAgencies()).isNotEmpty();
    }

    @Test
    void 위기_기관_목록은_109를_포함하고_폐지된_1393을_안내하지_않는다() {
        ChatEngineResponse response = chatEngine.respond("자살하고 싶어요", List.of());

        assertThat(response.recommendedAgencies()).anyMatch(agency -> agency.contains("109"));
        assertThat(response.recommendedAgencies()).noneMatch(agency -> agency.contains("1393"));
    }

    @Test
    void Accept_Language가_영어면_위기_응답과_기관명이_영어로_나간다() {
        LanguageContext.set(ResponseLanguage.EN);
        try {
            ChatEngineResponse response = chatEngine.respond("I want to die", List.of());

            assertThat(response.crisisDetected()).isTrue();
            assertThat(response.reply()).startsWith("That sounds");
            assertThat(response.recommendedAgencies()).anyMatch(agency -> agency.contains("Suicide Prevention"));
            assertThat(response.suggestedActions()).anyMatch(action -> action.contains("support service"));
        } finally {
            LanguageContext.clear();
        }
    }

    @Test
    void 한국어_헤더가_아니어도_다른_언어의_위기_표현을_감지한다() {
        assertThat(chatEngine.respond("もう死にたいです", List.of()).crisisDetected()).isTrue();
        assertThat(chatEngine.respond("我不想活了", List.of()).crisisDetected()).isTrue();
    }

    @Test
    void 영어_딥페이크_표현도_IMAGE_ABUSE로_분류한다() {
        ChatEngineResponse response = chatEngine.respond("Someone made a deepfake of me", List.of());

        assertThat(response.situationType()).isEqualTo(SituationType.IMAGE_ABUSE);
    }
}
