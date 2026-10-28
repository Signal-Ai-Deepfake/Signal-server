package com.signal.domain.chat.guidance;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class AgencyCatalogTest {

    @Test
    void 모든_기관의_전화번호는_안내_문구에_그대로_들어_있다() {
        for (Agency agency : AgencyCatalog.all()) {
            assertThat(agency.phones()).as(agency.id()).isNotEmpty();
            for (String phone : agency.phones()) {
                assertThat(agency.contact()).as("%s의 안내 문구에 번호 %s가 없다", agency.id(), phone).contains(phone);
            }
        }
    }

    @Test
    void 전화번호_원문_목록은_세_자리_이상_숫자_목록을_포함한다() {
        // knownNumbers()는 LLM 답변의 번호 검증용(숫자 조각), phoneNumbers()는 DB 시드 비교용(번호 원문)이다.
        assertThat(AgencyCatalog.phoneNumbers()).contains("109", "1388", "1377", "02-735-8994");
        assertThat(AgencyCatalog.knownNumbers()).contains("735", "8994", "109", "1388", "1377");
    }

    @Test
    void 경찰은_온라인_신고를_먼저_안내하고_112는_긴급_번호로_함께_둔다() {
        assertThat(AgencyCatalog.imageAbuse(com.signal.global.i18n.ResponseLanguage.KO))
                .anyMatch(agency -> agency.indexOf("ecrm.police.go.kr") >= 0
                        && agency.indexOf("ecrm.police.go.kr") < agency.indexOf("112"));
    }
}
