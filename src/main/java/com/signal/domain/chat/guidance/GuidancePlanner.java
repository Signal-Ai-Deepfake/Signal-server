package com.signal.domain.chat.guidance;

import com.signal.domain.chat.engine.SituationType;
import com.signal.domain.chat.progress.ConsultationStage;
import com.signal.domain.chat.progress.StageStatus;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 이번 턴에 LLM 답변 뒤에 붙일 안내 블록(단계)을 고른다. 한 턴에 하나만 붙여 사용자가 한꺼번에 읽을 양을 제한한다.
 *
 * 규칙:
 * - 위기 메시지에는 붙이지 않는다(위기 응답만 보낸다).
 * - 첫 메시지에는 원칙적으로 붙이지 않는다. 먼저 들어주고 상황을 묻는 게 우선이다.
 *   다만 첫 메시지에서 이미지 피해가 드러났고 증거(URL)가 아직 없으면 증거 보존 안내만 바로 붙인다.
 *   게시물은 삭제되거나 확산되기 전에 남겨야 해서 시간이 중요하기 때문이다.
 * - 이미지 피해는 증거 보존 → 기관 → 플랫폼 신고 → 정서 확인 순서, 일반 상담은 정서 확인 → 기관 순서다.
 *   증거는 시간이 지나면 사라지므로 가장 먼저, 정서 확인은 앞선 안내를 읽은 뒤에 건넨다.
 */
public final class GuidancePlanner {

    private static final List<ConsultationStage> ORDER_IMAGE_ABUSE = List.of(
            ConsultationStage.EVIDENCE_PRESERVATION,
            ConsultationStage.AGENCY_GUIDANCE,
            ConsultationStage.PLATFORM_REPORT,
            ConsultationStage.EMOTIONAL_SUPPORT);

    private static final List<ConsultationStage> ORDER_GENERAL = List.of(
            ConsultationStage.EMOTIONAL_SUPPORT,
            ConsultationStage.AGENCY_GUIDANCE);

    private GuidancePlanner() {
    }

    public static Optional<ConsultationStage> next(
            SituationType type, Map<ConsultationStage, StageStatus> stages, int userTurns, boolean crisisNow) {
        if (crisisNow) {
            return Optional.empty();
        }
        if (userTurns < 2) {
            boolean evidencePending = type == SituationType.IMAGE_ABUSE
                    && stages.getOrDefault(ConsultationStage.EVIDENCE_PRESERVATION, StageStatus.NOT_STARTED)
                    != StageStatus.COMPLETED;
            return evidencePending ? Optional.of(ConsultationStage.EVIDENCE_PRESERVATION) : Optional.empty();
        }
        List<ConsultationStage> order = type == SituationType.IMAGE_ABUSE ? ORDER_IMAGE_ABUSE : ORDER_GENERAL;
        for (ConsultationStage stage : order) {
            StageStatus status = stages.getOrDefault(stage, StageStatus.NOT_STARTED);
            // 정서 확인은 IN_PROGRESS가 "이미 건넸음/위기 직후"를 뜻하므로 NOT_STARTED일 때만 건넨다.
            boolean pending = stage == ConsultationStage.EMOTIONAL_SUPPORT
                    ? status == StageStatus.NOT_STARTED
                    : status != StageStatus.COMPLETED;
            if (pending) {
                return Optional.of(stage);
            }
        }
        return Optional.empty();
    }
}
