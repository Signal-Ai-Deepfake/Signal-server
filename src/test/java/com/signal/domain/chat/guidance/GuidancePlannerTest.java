package com.signal.domain.chat.guidance;

import static org.assertj.core.api.Assertions.assertThat;

import com.signal.domain.chat.engine.SituationType;
import com.signal.domain.chat.progress.ConsultationStage;
import com.signal.domain.chat.progress.StageStatus;
import java.util.EnumMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

class GuidancePlannerTest {

    private Map<ConsultationStage, StageStatus> stages(Object... pairs) {
        Map<ConsultationStage, StageStatus> map = new EnumMap<>(ConsultationStage.class);
        for (int i = 0; i < pairs.length; i += 2) {
            map.put((ConsultationStage) pairs[i], (StageStatus) pairs[i + 1]);
        }
        return map;
    }

    @Test
    void 첫_메시지와_위기_메시지에는_안내를_붙이지_않는다() {
        assertThat(GuidancePlanner.next(SituationType.IMAGE_ABUSE, stages(), 1, false)).isEmpty();
        assertThat(GuidancePlanner.next(SituationType.IMAGE_ABUSE, stages(), 5, true)).isEmpty();
    }

    @Test
    void 이미지_피해는_증거_기관_신고_정서_순서로_하나씩_안내한다() {
        Map<ConsultationStage, StageStatus> stages = stages();
        assertThat(GuidancePlanner.next(SituationType.IMAGE_ABUSE, stages, 2, false))
                .contains(ConsultationStage.EVIDENCE_PRESERVATION);

        stages.put(ConsultationStage.EVIDENCE_PRESERVATION, StageStatus.COMPLETED);
        assertThat(GuidancePlanner.next(SituationType.IMAGE_ABUSE, stages, 3, false))
                .contains(ConsultationStage.AGENCY_GUIDANCE);

        stages.put(ConsultationStage.AGENCY_GUIDANCE, StageStatus.COMPLETED);
        assertThat(GuidancePlanner.next(SituationType.IMAGE_ABUSE, stages, 4, false))
                .contains(ConsultationStage.PLATFORM_REPORT);

        stages.put(ConsultationStage.PLATFORM_REPORT, StageStatus.COMPLETED);
        assertThat(GuidancePlanner.next(SituationType.IMAGE_ABUSE, stages, 5, false))
                .contains(ConsultationStage.EMOTIONAL_SUPPORT);
    }

    @Test
    void 정서_확인은_이미_건넸거나_위기_직후면_다시_건네지_않는다() {
        Map<ConsultationStage, StageStatus> stages = stages(
                ConsultationStage.EVIDENCE_PRESERVATION, StageStatus.COMPLETED,
                ConsultationStage.AGENCY_GUIDANCE, StageStatus.COMPLETED,
                ConsultationStage.PLATFORM_REPORT, StageStatus.COMPLETED,
                ConsultationStage.EMOTIONAL_SUPPORT, StageStatus.IN_PROGRESS);
        assertThat(GuidancePlanner.next(SituationType.IMAGE_ABUSE, stages, 6, false)).isEmpty();
    }

    @Test
    void LLM이_진행_중으로_표시한_단계도_서버_안내가_나갈_때까지_안내한다() {
        Map<ConsultationStage, StageStatus> stages = stages(
                ConsultationStage.EVIDENCE_PRESERVATION, StageStatus.IN_PROGRESS);
        assertThat(GuidancePlanner.next(SituationType.IMAGE_ABUSE, stages, 2, false))
                .contains(ConsultationStage.EVIDENCE_PRESERVATION);
    }

    @Test
    void 일반_상담은_정서_확인_다음_기관_순서다() {
        Map<ConsultationStage, StageStatus> stages = stages();
        assertThat(GuidancePlanner.next(SituationType.GENERAL, stages, 2, false))
                .contains(ConsultationStage.EMOTIONAL_SUPPORT);
        stages.put(ConsultationStage.EMOTIONAL_SUPPORT, StageStatus.IN_PROGRESS);
        assertThat(GuidancePlanner.next(SituationType.GENERAL, stages, 3, false))
                .contains(ConsultationStage.AGENCY_GUIDANCE);
    }
}
