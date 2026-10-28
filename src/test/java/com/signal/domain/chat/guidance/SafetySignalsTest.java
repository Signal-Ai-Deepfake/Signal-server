package com.signal.domain.chat.guidance;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class SafetySignalsTest {

    @ParameterizedTest
    @ValueSource(strings = {
            "너무 힘들어서 자살 생각이 들어요",
            "I just want to die",
            "I keep thinking about suicide",
            "I want to kill myself",
            "もう死にたいです",
            "我不想活了",
            "我想自杀"})
    void 네_가지_언어의_위기_표현을_감지한다(String message) {
        assertThat(SafetySignals.isCrisis(message)).isTrue();
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "요즘 고민이 있어서 얘기하고 싶어요",
            "I had a rough day at work",
            "今日は疲れました",
            "今天有点累"})
    void 평범한_메시지는_위기로_감지하지_않는다(String message) {
        assertThat(SafetySignals.isCrisis(message)).isFalse();
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "제 사진이 합성돼서 유포됐어요",
            "Someone made a deepfake of me",
            "He is threatening to leak my photos, it's sextortion",
            "私の写真が流出しました",
            "我的裸照被泄露了"})
    void 네_가지_언어의_이미지_피해_표현을_감지한다(String message) {
        assertThat(SafetySignals.isImageAbuse(message)).isTrue();
    }

    @Test
    void 대소문자와_빈_입력을_안전하게_처리한다() {
        assertThat(SafetySignals.isCrisis("I WANT TO DIE")).isTrue();
        assertThat(SafetySignals.isCrisis(null)).isFalse();
        assertThat(SafetySignals.isImageAbuse("   ")).isFalse();
    }
}
