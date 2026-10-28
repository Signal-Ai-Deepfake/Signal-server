package com.signal.domain.chat.guidance;

import static org.assertj.core.api.Assertions.assertThat;

import com.signal.domain.chat.engine.SituationType;
import com.signal.domain.chat.progress.ConsultationStage;
import com.signal.global.i18n.ResponseLanguage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;

class GuidanceTextTest {

    @ParameterizedTest
    @EnumSource(ResponseLanguage.class)
    void 모든_언어에_위기_응답과_기관_목록이_있고_109를_안내한다(ResponseLanguage language) {
        assertThat(GuidanceText.crisisReply(language)).isNotBlank().contains("112");
        assertThat(AgencyCatalog.crisis(language)).anyMatch(agency -> agency.contains("109"));
        assertThat(AgencyCatalog.crisis(language)).noneMatch(agency -> agency.contains("1393"));
    }

    @ParameterizedTest
    @EnumSource(ResponseLanguage.class)
    void 안내가_필요한_모든_단계에_모든_언어의_블록이_있다(ResponseLanguage language) {
        for (ConsultationStage stage : ConsultationStage.values()) {
            if (stage == ConsultationStage.SITUATION_CHECK) {
                continue;
            }
            assertThat(GuidanceText.guidanceBlock(stage, SituationType.IMAGE_ABUSE, language))
                    .as("%s/%s", stage, language).isNotBlank();
            assertThat(GuidanceText.guidanceBlock(stage, SituationType.GENERAL, language))
                    .as("%s/%s", stage, language).isNotBlank();
        }
    }

    @Test
    void 이미지_피해_기관_안내는_삭제_지원과_심의_신고_경찰을_포함한다() {
        assertThat(AgencyCatalog.imageAbuse(ResponseLanguage.KO))
                .anyMatch(agency -> agency.contains("d4u.stop.or.kr"))
                .anyMatch(agency -> agency.contains("1377"))
                .anyMatch(agency -> agency.contains("112"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"네 좋아요", "응", "네, 괜찮아요", "그래요", "마무리해주세요", "Yes, please finish", "はい", "好的", "好的,我没事", "はい、大丈夫です"})
    void 종료_제안에_긍정하면_종료로_판단한다(String reply) {
        assertThat(GuidanceText.isEndConfirmation(reply)).isTrue();
    }

    @ParameterizedTest
    @ValueSource(strings = {"더 얘기하고 싶어요", "그래도 계속 얘기할래", "그래도 얘기하니 낫네요", "응원해주셔서 감사해요", "no, I want to keep talking", "我还想继续", "我不想结束", "終わりたくない", "大丈夫じゃない", "大丈夫ではありません", "끝내고 싶지 않아요", "아직 마무리 안 할래요", "I don't want to finish", "not okay, I want to talk more", "先不结束,好的"})
    void 더_이야기하고_싶다는_답은_종료로_판단하지_않는다(String reply) {
        assertThat(GuidanceText.isEndConfirmation(reply)).isFalse();
    }

    @Test
    void 문장에_쓰인_기관_번호는_모두_기관_목록에_있는_번호다() {
        java.util.Set<String> known = AgencyCatalog.knownNumbers();
        assertThat(known).contains("109", "112", "119", "1377", "1388");
        java.util.regex.Pattern digits = java.util.regex.Pattern.compile("\\d{3,}");

        for (ResponseLanguage language : ResponseLanguage.values()) {
            java.util.List<String> texts = new java.util.ArrayList<>();
            texts.add(GuidanceText.crisisReply(language));
            for (ConsultationStage stage : ConsultationStage.values()) {
                for (SituationType type : SituationType.values()) {
                    String block = GuidanceText.guidanceBlock(stage, type, language);
                    if (block != null) {
                        texts.add(block);
                    }
                }
            }
            for (String text : texts) {
                digits.matcher(text).results().forEach(match ->
                        assertThat(known).as("문장에 있는 번호 %s (%s)", match.group(), language).contains(match.group()));
            }
        }
    }
}
