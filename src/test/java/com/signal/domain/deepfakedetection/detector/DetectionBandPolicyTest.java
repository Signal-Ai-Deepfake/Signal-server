package com.signal.domain.deepfakedetection.detector;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.signal.domain.deepfakedetection.entity.AssessmentBasis;
import com.signal.domain.deepfakedetection.entity.DetectionBand;
import com.signal.domain.deepfakedetection.entity.ProvenanceSignal;
import com.signal.domain.deepfakedetection.entity.UndeterminedReason;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class DetectionBandPolicyTest {

    private final DetectionBandPolicy policy = new DetectionBandPolicy(30, 70, 256);

    @ParameterizedTest
    @CsvSource({"0, LOW", "29, LOW", "30, UNCERTAIN", "50, UNCERTAIN", "70, UNCERTAIN", "71, HIGH", "100, HIGH"})
    void 점수는_구간으로_나뉜다(int riskScore, DetectionBand band) {
        DetectionBandPolicy.Assessment assessment = policy.assess(riskScore, 1024, 768, List.of());

        assertThat(assessment.band()).isEqualTo(band);
        assertThat(assessment.basis()).isEqualTo(AssessmentBasis.MODEL);
        assertThat(assessment.undeterminedReason()).isNull();
    }

    @Test
    void 짧은_변이_기준보다_작으면_점수와_무관하게_판단_불가() {
        for (int riskScore : new int[]{0, 50, 100}) {
            DetectionBandPolicy.Assessment assessment = policy.assess(riskScore, 255, 4000, List.of());
            assertThat(assessment.band()).isEqualTo(DetectionBand.UNDETERMINED);
            assertThat(assessment.undeterminedReason()).isEqualTo(UndeterminedReason.LOW_RESOLUTION);
        }
        assertThat(policy.assess(50, 256, 256, List.of()).band()).isEqualTo(DetectionBand.UNCERTAIN);
    }

    @Test
    void 크기를_모르면_해상도로_판단_불가를_내지_않는다() {
        assertThat(policy.assess(10, 0, 0, List.of()).band()).isEqualTo(DetectionBand.LOW);
    }

    @Test
    void 강한_출처_신호는_점수와_해상도보다_우선해서_높음() {
        DetectionBandPolicy.Assessment assessment =
                policy.assess(2, 100, 100, List.of(ProvenanceSignal.GENERATOR_METADATA));

        assertThat(assessment.band()).isEqualTo(DetectionBand.HIGH);
        assertThat(assessment.basis()).isEqualTo(AssessmentBasis.PROVENANCE);
    }

    @Test
    void C2PA_매니페스트만_있으면_등급에_영향이_없다() {
        assertThat(policy.assess(10, 1024, 768, List.of(ProvenanceSignal.C2PA_MANIFEST)).band())
                .isEqualTo(DetectionBand.LOW);
    }

    @Test
    void AI_편집_표시는_높음으로_올리지_않고_낮음을_불확실로만_올린다() {
        List<ProvenanceSignal> edited = List.of(ProvenanceSignal.AI_EDIT_DECLARED);

        DetectionBandPolicy.Assessment low = policy.assess(10, 1024, 768, edited);
        assertThat(low.band()).isEqualTo(DetectionBand.UNCERTAIN);
        assertThat(low.basis()).isEqualTo(AssessmentBasis.PROVENANCE);

        assertThat(policy.assess(50, 1024, 768, edited).band()).isEqualTo(DetectionBand.UNCERTAIN);
        assertThat(policy.assess(50, 1024, 768, edited).basis()).isEqualTo(AssessmentBasis.MODEL);
        assertThat(policy.assess(95, 1024, 768, edited).band()).isEqualTo(DetectionBand.HIGH);
        assertThat(policy.assess(95, 1024, 768, edited).basis()).isEqualTo(AssessmentBasis.MODEL);
        // 해상도 부족은 편집 표시보다 우선한다
        assertThat(policy.assess(10, 100, 100, edited).band()).isEqualTo(DetectionBand.UNDETERMINED);
    }

    @Test
    void 출처_신호가_없다고_진짜로_보지_않는다() {
        // 점수가 높으면 신호가 없어도 높음 그대로다.
        assertThat(policy.assess(95, 1024, 768, List.of()).band()).isEqualTo(DetectionBand.HIGH);
    }

    @Test
    void 임계값이_뒤집히면_만들_수_없다() {
        assertThatThrownBy(() -> new DetectionBandPolicy(80, 20, 256)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new DetectionBandPolicy(-1, 70, 256)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new DetectionBandPolicy(30, 101, 256)).isInstanceOf(IllegalArgumentException.class);
    }
}
