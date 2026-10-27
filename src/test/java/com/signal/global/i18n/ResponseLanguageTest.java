package com.signal.global.i18n;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class ResponseLanguageTest {

    @Test
    void 헤더가_없거나_비어_있으면_한국어를_사용한다() {
        assertThat(ResponseLanguage.fromAcceptLanguage(null)).isEqualTo(ResponseLanguage.KO);
        assertThat(ResponseLanguage.fromAcceptLanguage("")).isEqualTo(ResponseLanguage.KO);
        assertThat(ResponseLanguage.fromAcceptLanguage("   ")).isEqualTo(ResponseLanguage.KO);
    }

    @Test
    void 프론트가_보내는_네_가지_값을_인식한다() {
        assertThat(ResponseLanguage.fromAcceptLanguage("ko")).isEqualTo(ResponseLanguage.KO);
        assertThat(ResponseLanguage.fromAcceptLanguage("en")).isEqualTo(ResponseLanguage.EN);
        assertThat(ResponseLanguage.fromAcceptLanguage("ja")).isEqualTo(ResponseLanguage.JA);
        assertThat(ResponseLanguage.fromAcceptLanguage("zh-CN")).isEqualTo(ResponseLanguage.ZH_CN);
    }

    @Test
    void 지역이_붙거나_대소문자가_달라도_인식한다() {
        assertThat(ResponseLanguage.fromAcceptLanguage("en-US,en;q=0.9")).isEqualTo(ResponseLanguage.EN);
        assertThat(ResponseLanguage.fromAcceptLanguage("ja-JP")).isEqualTo(ResponseLanguage.JA);
        assertThat(ResponseLanguage.fromAcceptLanguage("zh-cn")).isEqualTo(ResponseLanguage.ZH_CN);
        assertThat(ResponseLanguage.fromAcceptLanguage("zh")).isEqualTo(ResponseLanguage.ZH_CN);
        assertThat(ResponseLanguage.fromAcceptLanguage("zh-Hans-CN")).isEqualTo(ResponseLanguage.ZH_CN);
    }

    @Test
    void 지원하지_않는_언어는_한국어로_처리한다() {
        assertThat(ResponseLanguage.fromAcceptLanguage("fr-FR,fr;q=0.9")).isEqualTo(ResponseLanguage.KO);
        assertThat(ResponseLanguage.fromAcceptLanguage("zh-TW")).isEqualTo(ResponseLanguage.KO);
        assertThat(ResponseLanguage.fromAcceptLanguage("*")).isEqualTo(ResponseLanguage.KO);
    }

    @Test
    void 선호도_q_순서대로_지원하는_첫_언어를_고른다() {
        assertThat(ResponseLanguage.fromAcceptLanguage("fr,ja;q=0.8,en;q=0.9")).isEqualTo(ResponseLanguage.EN);
        assertThat(ResponseLanguage.fromAcceptLanguage("zh-TW,en;q=0.5")).isEqualTo(ResponseLanguage.EN);
        assertThat(ResponseLanguage.fromAcceptLanguage("en;q=0,ja;q=0.1")).isEqualTo(ResponseLanguage.JA);
    }

    @Test
    void 형식이_잘못된_헤더도_예외_없이_한국어나_인식_가능한_언어로_처리한다() {
        assertThat(ResponseLanguage.fromAcceptLanguage(";;,,")).isEqualTo(ResponseLanguage.KO);
        assertThat(ResponseLanguage.fromAcceptLanguage("en;q=abc, ja")).isEqualTo(ResponseLanguage.JA);
    }

    @Test
    void 한국어는_프롬프트에_지시문을_더하지_않고_다른_언어는_해당_언어를_지시한다() {
        assertThat(ResponseLanguage.KO.promptInstruction()).isEmpty();
        assertThat(ResponseLanguage.EN.promptInstruction()).contains("English");
        assertThat(ResponseLanguage.JA.promptInstruction()).contains("Japanese");
        assertThat(ResponseLanguage.ZH_CN.promptInstruction()).contains("Simplified Chinese");
        assertThat(ResponseLanguage.EN.promptInstruction()).contains("JSON keys");
    }
}
