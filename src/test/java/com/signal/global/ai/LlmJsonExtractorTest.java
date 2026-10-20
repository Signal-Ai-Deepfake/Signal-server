package com.signal.global.ai;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class LlmJsonExtractorTest {

    @Test
    void 순수_JSON은_그대로_반환한다() {
        assertThat(LlmJsonExtractor.stripCodeFence("{\"a\":1}")).isEqualTo("{\"a\":1}");
    }

    @Test
    void 코드블록을_제거한다() {
        assertThat(LlmJsonExtractor.stripCodeFence("```json\n{\"a\":1}\n```")).isEqualTo("{\"a\":1}");
    }

    @Test
    void think_블록을_제거한다() {
        assertThat(LlmJsonExtractor.stripCodeFence("<think>음...\n{\"x\":0}</think>\n{\"a\":1}")).isEqualTo("{\"a\":1}");
    }

    @Test
    void JSON_앞뒤의_설명_문장을_제거한다() {
        assertThat(LlmJsonExtractor.stripCodeFence("결과입니다: {\"a\":{\"b\":2}} 이상입니다.")).isEqualTo("{\"a\":{\"b\":2}}");
    }

    @Test
    void null이면_null을_반환한다() {
        assertThat(LlmJsonExtractor.stripCodeFence(null)).isNull();
    }
}
