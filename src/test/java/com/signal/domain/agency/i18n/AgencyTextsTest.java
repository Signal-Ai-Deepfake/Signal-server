package com.signal.domain.agency.i18n;

import static org.assertj.core.api.Assertions.assertThat;

import com.signal.global.i18n.ResponseLanguage;
import java.util.List;
import org.junit.jupiter.api.Test;

class AgencyTextsTest {

    @Test
    void 한국어는_원문_그대로다() {
        assertThat(AgencyTexts.name("자살예방 상담전화", ResponseLanguage.KO)).isEqualTo("자살예방 상담전화");
        assertThat(AgencyTexts.availableHours("24시간", ResponseLanguage.KO)).isEqualTo("24시간");
        assertThat(AgencyTexts.supportedActions(List.of("위기 상담"), ResponseLanguage.KO)).containsExactly("위기 상담");
    }

    @Test
    void 다른_언어의_기관명은_한국어_명칭을_유지하고_번역을_괄호로_덧붙인다() {
        assertThat(AgencyTexts.name("경찰 사이버범죄 신고", ResponseLanguage.EN))
                .isEqualTo("경찰 사이버범죄 신고 (Police Cyber Crime Report)");
        assertThat(AgencyTexts.name("경찰 사이버범죄 신고", ResponseLanguage.JA))
                .isEqualTo("경찰 사이버범죄 신고 (警察サイバー犯罪通報)");
        assertThat(AgencyTexts.name("경찰 사이버범죄 신고", ResponseLanguage.ZH_CN))
                .isEqualTo("경찰 사이버범죄 신고 (警方网络犯罪举报)");
    }

    @Test
    void 운영시간과_지원_내용이_요청_언어로_바뀐다() {
        assertThat(AgencyTexts.availableHours("24시간", ResponseLanguage.EN)).isEqualTo("24 hours");
        assertThat(AgencyTexts.supportedActions(List.of("삭제 지원", "수사 요청"), ResponseLanguage.JA))
                .containsExactly("削除支援", "捜査の依頼");
    }

    @Test
    void 표에_없는_문구는_번역하지_않고_원문을_그대로_둔다() {
        assertThat(AgencyTexts.name("새로 생긴 기관", ResponseLanguage.EN)).isEqualTo("새로 생긴 기관");
        assertThat(AgencyTexts.availableHours("평일 10:00~17:00", ResponseLanguage.ZH_CN)).isEqualTo("평일 10:00~17:00");
        assertThat(AgencyTexts.supportedActions(List.of("새 지원"), ResponseLanguage.EN)).containsExactly("새 지원");
    }

    @Test
    void null_값은_그대로_null이다() {
        assertThat(AgencyTexts.availableHours(null, ResponseLanguage.EN)).isNull();
        assertThat(AgencyTexts.supportedActions(null, ResponseLanguage.EN)).isNull();
    }
}
