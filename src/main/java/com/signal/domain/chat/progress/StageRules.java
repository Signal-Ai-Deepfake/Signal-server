package com.signal.domain.chat.progress;

import com.signal.domain.chat.engine.SituationType;
import java.util.EnumMap;
import java.util.Map;

/**
 * 단계 완료를 서버가 결정하는 규칙. 스프링에 의존하지 않는 순수 로직이라 단독으로 테스트할 수 있다.
 *
 * 원칙:
 * - LLM 판단({@code llm})은 단계를 시작(IN_PROGRESS)으로까지만 올릴 수 있고 COMPLETED로 만들 수 없다.
 * - 완료된 단계는 되돌아가지 않는다. 단, 새로 위기가 감지되면 정서 안정 단계는 다시 진행 중으로 돌린다.
 * - 정서 안정은 "서버가 확인 질문을 건넸고(IN_PROGRESS), 그 뒤 위기 신호 없는 메시지가 한 번 더 온 경우"에만 완료된다.
 *   위기 직후도 같은 방식으로, 위기가 지나간 다음 메시지에서 완료된다.
 */
public final class StageRules {

    /** 상황 파악이 끝났다고 보는 최소 사용자 메시지 수. */
    static final int SITUATION_TURNS_IMAGE_ABUSE = 2;
    static final int SITUATION_TURNS_GENERAL = 3;

    private StageRules() {
    }

    public static Map<ConsultationStage, StageStatus> merge(
            SituationType type,
            Map<ConsultationStage, StageStatus> before,
            StageFacts facts,
            Map<ConsultationStage, StageStatus> llm) {
        Map<ConsultationStage, StageStatus> merged = new EnumMap<>(ConsultationStage.class);

        merged.put(ConsultationStage.SITUATION_CHECK, furthest(
                before.get(ConsultationStage.SITUATION_CHECK),
                situationCheck(type, facts.userTurnCount()),
                capped(llm.get(ConsultationStage.SITUATION_CHECK))));

        merged.put(ConsultationStage.EMOTIONAL_SUPPORT,
                emotionalSupport(before.get(ConsultationStage.EMOTIONAL_SUPPORT), facts.crisisDetected()));

        merged.put(ConsultationStage.EVIDENCE_PRESERVATION, furthest(
                before.get(ConsultationStage.EVIDENCE_PRESERVATION),
                facts.evidenceUrlMentioned() ? StageStatus.COMPLETED : StageStatus.NOT_STARTED,
                capped(llm.get(ConsultationStage.EVIDENCE_PRESERVATION))));

        merged.put(ConsultationStage.PLATFORM_REPORT, furthest(
                before.get(ConsultationStage.PLATFORM_REPORT),
                facts.reportFinalized() ? StageStatus.COMPLETED
                        : facts.reportStarted() ? StageStatus.IN_PROGRESS : StageStatus.NOT_STARTED,
                capped(llm.get(ConsultationStage.PLATFORM_REPORT))));

        // 이미지 피해에서는 위기 상담전화만으로는 기관 안내가 끝난 게 아니다(삭제·수사 기관은 따로 안내한다).
        boolean crisisLinesCount = type != SituationType.IMAGE_ABUSE && facts.everCrisisDetected();
        merged.put(ConsultationStage.AGENCY_GUIDANCE, furthest(
                before.get(ConsultationStage.AGENCY_GUIDANCE),
                crisisLinesCount ? StageStatus.COMPLETED : StageStatus.NOT_STARTED,
                capped(llm.get(ConsultationStage.AGENCY_GUIDANCE))));
        return merged;
    }

    /** 서버가 안내 블록을 이번 턴에 내보냈을 때의 단계 상태 변화. 안내 단계는 "안내했다"가 완료다. */
    public static StageSnapshot markDelivered(StageSnapshot snapshot, ConsultationStage stage) {
        Map<ConsultationStage, StageStatus> stages = new EnumMap<>(ConsultationStage.class);
        stages.putAll(snapshot.stages());
        stages.put(stage, stage == ConsultationStage.EMOTIONAL_SUPPORT
                ? StageStatus.IN_PROGRESS      // 확인 질문을 건넸을 뿐, 사용자의 답을 받아야 완료
                : StageStatus.COMPLETED);
        return new StageSnapshot(snapshot.type(), stages);
    }

    private static StageStatus situationCheck(SituationType type, int userTurns) {
        int needed = type == SituationType.IMAGE_ABUSE ? SITUATION_TURNS_IMAGE_ABUSE : SITUATION_TURNS_GENERAL;
        if (userTurns >= needed) {
            return StageStatus.COMPLETED;
        }
        return userTurns >= 1 ? StageStatus.IN_PROGRESS : StageStatus.NOT_STARTED;
    }

    private static StageStatus emotionalSupport(StageStatus previous, boolean crisisNow) {
        if (crisisNow) {
            return StageStatus.IN_PROGRESS;
        }
        if (previous == StageStatus.IN_PROGRESS) {
            return StageStatus.COMPLETED;
        }
        return previous == null ? StageStatus.NOT_STARTED : previous;
    }

    private static StageStatus capped(StageStatus llmStatus) {
        return llmStatus == StageStatus.COMPLETED ? StageStatus.IN_PROGRESS : llmStatus;
    }

    private static StageStatus furthest(StageStatus... candidates) {
        StageStatus best = StageStatus.NOT_STARTED;
        for (StageStatus candidate : candidates) {
            if (candidate != null && candidate != StageStatus.NOT_APPLICABLE
                    && candidate.ordinal() > best.ordinal()) {
                best = candidate;
            }
        }
        return best;
    }
}
