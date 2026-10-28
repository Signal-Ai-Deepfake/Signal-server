package com.signal.domain.chat.guidance;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class ReplyGuardTest {

    @Test
    void 기관_목록에_있는_번호는_허용한다() {
        assertThat(ReplyGuard.containsUnknownNumber("109나 1388에 연락하세요", "")).isFalse();
        assertThat(ReplyGuard.containsUnknownNumber("디성센터 02-735-8994, 방심위 1377", "")).isFalse();
    }

    @Test
    void 기관_목록에_없는_번호는_거른다() {
        assertThat(ReplyGuard.containsUnknownNumber("1393으로 전화하세요", "")).isTrue();
        assertThat(ReplyGuard.containsUnknownNumber("1577-0199에 연락하세요", "")).isTrue();
        assertThat(ReplyGuard.containsUnknownNumber("call 182 now", "")).isTrue();
    }

    @Test
    void 사용자가_쓴_숫자는_되풀이해도_된다() {
        assertThat(ReplyGuard.containsUnknownNumber("말씀하신 3000건이라니 놀라셨겠어요", "3000건이 유포됐어요")).isFalse();
    }

    @Test
    void 연도와_퍼센트와_두자리_숫자는_번호로_보지_않는다() {
        assertThat(ReplyGuard.containsUnknownNumber("2024년 기준 90% 이상, 24시간 운영", "")).isFalse();
        assertThat(ReplyGuard.containsUnknownNumber("", "")).isFalse();
        assertThat(ReplyGuard.containsUnknownNumber(null, "")).isFalse();
    }
}
