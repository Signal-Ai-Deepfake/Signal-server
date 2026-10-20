package com.signal.domain.chat.progress;

import com.signal.domain.chat.engine.ChatTurn;
import com.signal.domain.chat.engine.SituationType;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 단계 상태를 갱신한다: LLM 판단({@link StageAnalyzer}) + 서버가 아는 사실({@link StageFacts}) + 이전 상태를 합쳐
 * 단계별로 가장 진행된 값을 취한다(완료된 단계는 되돌아가지 않음). 위기 상황에서는 민감한 대화를 LLM에
 * 보내지 않고 룰 기반 값만 사용하며, LLM 호출이 실패해도 룰 기반 값으로 대체한다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ConsultationProgressTracker {

    private final StageAnalyzer stageAnalyzer;

    public StageSnapshot update(StageSnapshot previous, SituationType currentType,
                                List<ChatTurn> turns, StageFacts facts) {
        SituationType type = resolveType(previous, currentType);
        Map<ConsultationStage, StageStatus> before = previous == null ? Map.of() : previous.stages();

        Map<ConsultationStage, StageStatus> merged = new EnumMap<>(ConsultationStage.class);
        Map<ConsultationStage, StageStatus> fromRules = fromFacts(type, facts);
        Map<ConsultationStage, StageStatus> fromLlm = analyzeSafely(type, turns, before, facts);

        for (ConsultationStage stage : ConsultationStage.values()) {
            merged.put(stage, furthest(
                    before.get(stage), fromRules.get(stage), fromLlm.get(stage)));
        }
        return new StageSnapshot(type, merged);
    }

    private SituationType resolveType(StageSnapshot previous, SituationType currentType) {
        if (currentType == SituationType.IMAGE_ABUSE
                || (previous != null && previous.type() == SituationType.IMAGE_ABUSE)) {
            return SituationType.IMAGE_ABUSE;
        }
        return SituationType.GENERAL;
    }

    private Map<ConsultationStage, StageStatus> analyzeSafely(
            SituationType type, List<ChatTurn> turns,
            Map<ConsultationStage, StageStatus> previous, StageFacts facts) {
        if (facts.crisisDetected()) {
            return Map.of();
        }
        try {
            return stageAnalyzer.analyze(type, turns, previous);
        } catch (Exception e) {
            log.warn("LLM 상담 단계 분석 실패, 룰 기반 단계 상태만 사용합니다.", e);
            return Map.of();
        }
    }

    private Map<ConsultationStage, StageStatus> fromFacts(SituationType type, StageFacts facts) {
        Map<ConsultationStage, StageStatus> result = new EnumMap<>(ConsultationStage.class);
        result.put(ConsultationStage.SITUATION_CHECK,
                type == SituationType.IMAGE_ABUSE ? StageStatus.COMPLETED : StageStatus.IN_PROGRESS);
        result.put(ConsultationStage.EMOTIONAL_SUPPORT,
                facts.emotionallyStabilized() ? StageStatus.COMPLETED : StageStatus.IN_PROGRESS);
        result.put(ConsultationStage.EVIDENCE_PRESERVATION,
                facts.evidenceUrlMentioned() ? StageStatus.COMPLETED : StageStatus.NOT_STARTED);
        result.put(ConsultationStage.PLATFORM_REPORT,
                facts.reportFinalized() ? StageStatus.COMPLETED
                        : facts.reportStarted() ? StageStatus.IN_PROGRESS : StageStatus.NOT_STARTED);
        result.put(ConsultationStage.AGENCY_GUIDANCE,
                facts.agenciesRecommended() ? StageStatus.COMPLETED : StageStatus.NOT_STARTED);
        return result;
    }

    private StageStatus furthest(StageStatus... candidates) {
        StageStatus best = StageStatus.NOT_STARTED;
        for (StageStatus candidate : candidates) {
            if (candidate != null && candidate != StageStatus.NOT_APPLICABLE && candidate.ordinal() > best.ordinal()) {
                best = candidate;
            }
        }
        return best;
    }
}
