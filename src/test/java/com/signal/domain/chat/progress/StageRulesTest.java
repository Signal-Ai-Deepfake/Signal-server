package com.signal.domain.chat.progress;

import static org.assertj.core.api.Assertions.assertThat;

import com.signal.domain.chat.engine.SituationType;
import java.util.EnumMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

class StageRulesTest {

    private static final Map<ConsultationStage, StageStatus> NONE = Map.of();

    private StageFacts facts(boolean crisis, boolean everCrisis, boolean url, int turns) {
        return new StageFacts(crisis, everCrisis, url, false, false, turns);
    }

    @Test
    void 이미지_피해는_사용자_메시지_두_개부터_상황_파악이_완료된다() {
        assertThat(StageRules.merge(SituationType.IMAGE_ABUSE, NONE, facts(false, false, false, 1), NONE)
                .get(ConsultationStage.SITUATION_CHECK)).isEqualTo(StageStatus.IN_PROGRESS);
        assertThat(StageRules.merge(SituationType.IMAGE_ABUSE, NONE, facts(false, false, false, 2), NONE)
                .get(ConsultationStage.SITUATION_CHECK)).isEqualTo(StageStatus.COMPLETED);
    }

    @Test
    void 일반_상담은_세_번째_메시지부터_상황_파악이_완료된다() {
        assertThat(StageRules.merge(SituationType.GENERAL, NONE, facts(false, false, false, 2), NONE)
                .get(ConsultationStage.SITUATION_CHECK)).isEqualTo(StageStatus.IN_PROGRESS);
        assertThat(StageRules.merge(SituationType.GENERAL, NONE, facts(false, false, false, 3), NONE)
                .get(ConsultationStage.SITUATION_CHECK)).isEqualTo(StageStatus.COMPLETED);
    }

    @Test
    void LLM_판단은_진행_중까지만_반영하고_완료로_만들지_못한다() {
        Map<ConsultationStage, StageStatus> llm = new EnumMap<>(ConsultationStage.class);
        for (ConsultationStage stage : ConsultationStage.values()) {
            llm.put(stage, StageStatus.COMPLETED);
        }
        Map<ConsultationStage, StageStatus> merged = StageRules.merge(
                SituationType.IMAGE_ABUSE, NONE, facts(false, false, false, 1), llm);

        assertThat(merged.get(ConsultationStage.EVIDENCE_PRESERVATION)).isEqualTo(StageStatus.IN_PROGRESS);
        assertThat(merged.get(ConsultationStage.PLATFORM_REPORT)).isEqualTo(StageStatus.IN_PROGRESS);
        assertThat(merged.get(ConsultationStage.AGENCY_GUIDANCE)).isEqualTo(StageStatus.IN_PROGRESS);
        assertThat(merged.get(ConsultationStage.EMOTIONAL_SUPPORT)).isEqualTo(StageStatus.NOT_STARTED);
    }

    @Test
    void 사용자가_URL을_남기면_증거_보존이_완료되고_완료된_단계는_되돌아가지_않는다() {
        Map<ConsultationStage, StageStatus> first = StageRules.merge(
                SituationType.IMAGE_ABUSE, NONE, facts(false, false, true, 1), NONE);
        assertThat(first.get(ConsultationStage.EVIDENCE_PRESERVATION)).isEqualTo(StageStatus.COMPLETED);

        Map<ConsultationStage, StageStatus> second = StageRules.merge(
                SituationType.IMAGE_ABUSE, first, facts(false, false, false, 2), NONE);
        assertThat(second.get(ConsultationStage.EVIDENCE_PRESERVATION)).isEqualTo(StageStatus.COMPLETED);
    }

    @Test
    void 정서_안정은_확인_질문_뒤_위기_없는_답을_받아야_완료된다() {
        StageSnapshot delivered = StageRules.markDelivered(
                new StageSnapshot(SituationType.GENERAL, NONE), ConsultationStage.EMOTIONAL_SUPPORT);
        assertThat(delivered.stages().get(ConsultationStage.EMOTIONAL_SUPPORT)).isEqualTo(StageStatus.IN_PROGRESS);

        Map<ConsultationStage, StageStatus> afterReply = StageRules.merge(
                SituationType.GENERAL, delivered.stages(), facts(false, false, false, 3), NONE);
        assertThat(afterReply.get(ConsultationStage.EMOTIONAL_SUPPORT)).isEqualTo(StageStatus.COMPLETED);
    }

    @Test
    void 위기가_감지되면_정서_안정은_완료였어도_다시_진행_중이_된다() {
        Map<ConsultationStage, StageStatus> before = Map.of(
                ConsultationStage.EMOTIONAL_SUPPORT, StageStatus.COMPLETED);
        Map<ConsultationStage, StageStatus> merged = StageRules.merge(
                SituationType.GENERAL, before, facts(true, true, false, 4), NONE);
        assertThat(merged.get(ConsultationStage.EMOTIONAL_SUPPORT)).isEqualTo(StageStatus.IN_PROGRESS);
    }

    @Test
    void 위기_상담전화는_일반_상담의_기관_안내로만_인정된다() {
        assertThat(StageRules.merge(SituationType.GENERAL, NONE, facts(false, true, false, 2), NONE)
                .get(ConsultationStage.AGENCY_GUIDANCE)).isEqualTo(StageStatus.COMPLETED);
        assertThat(StageRules.merge(SituationType.IMAGE_ABUSE, NONE, facts(false, true, false, 2), NONE)
                .get(ConsultationStage.AGENCY_GUIDANCE)).isEqualTo(StageStatus.NOT_STARTED);
    }

    @Test
    void 안내_블록을_내보낸_단계는_완료로_기록한다() {
        StageSnapshot snapshot = StageRules.markDelivered(
                new StageSnapshot(SituationType.IMAGE_ABUSE, NONE), ConsultationStage.PLATFORM_REPORT);
        assertThat(snapshot.stages().get(ConsultationStage.PLATFORM_REPORT)).isEqualTo(StageStatus.COMPLETED);
    }

    @Test
    void 일반_상담에서_이미지_피해로_바뀌면_기관_안내를_다시_시작한다() {
        Map<ConsultationStage, StageStatus> before = Map.of(
                ConsultationStage.SITUATION_CHECK, StageStatus.COMPLETED,
                ConsultationStage.AGENCY_GUIDANCE, StageStatus.COMPLETED);

        Map<ConsultationStage, StageStatus> adjusted = StageRules.forTypeChange(
                SituationType.GENERAL, SituationType.IMAGE_ABUSE, before);
        Map<ConsultationStage, StageStatus> merged = StageRules.merge(
                SituationType.IMAGE_ABUSE, adjusted, facts(false, false, false, 4), NONE);

        assertThat(merged.get(ConsultationStage.AGENCY_GUIDANCE)).isEqualTo(StageStatus.NOT_STARTED);
        assertThat(merged.get(ConsultationStage.SITUATION_CHECK)).isEqualTo(StageStatus.COMPLETED);
    }

    @Test
    void 유형이_그대로거나_이미지_피해에서_시작하면_상태를_바꾸지_않는다() {
        Map<ConsultationStage, StageStatus> before = Map.of(ConsultationStage.AGENCY_GUIDANCE, StageStatus.COMPLETED);

        assertThat(StageRules.forTypeChange(SituationType.GENERAL, SituationType.GENERAL, before)).isEqualTo(before);
        assertThat(StageRules.forTypeChange(SituationType.IMAGE_ABUSE, SituationType.IMAGE_ABUSE, before))
                .isEqualTo(before);
        assertThat(StageRules.forTypeChange(null, SituationType.IMAGE_ABUSE, before)).isEqualTo(before);
    }
}
