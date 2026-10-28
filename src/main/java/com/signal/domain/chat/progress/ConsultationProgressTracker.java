package com.signal.domain.chat.progress;

import com.signal.domain.chat.engine.ChatTurn;
import com.signal.domain.chat.engine.SituationType;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 단계 상태를 갱신한다. 완료 여부는 서버 규칙({@link StageRules})이 결정하고, LLM 판단({@link StageAnalyzer})은
 * 단계를 "진행 중"으로 표시하는 데까지만 반영한다. 위기 상황에서는 민감한 대화를 LLM에 보내지 않고,
 * LLM 호출이 실패해도 규칙 기반 값만으로 진행한다.
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

        Map<ConsultationStage, StageStatus> fromLlm = analyzeSafely(type, turns, before, facts);
        Map<ConsultationStage, StageStatus> merged = StageRules.merge(type, before, facts, fromLlm);
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
}
