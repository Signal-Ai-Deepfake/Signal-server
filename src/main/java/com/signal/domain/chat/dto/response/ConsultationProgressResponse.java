package com.signal.domain.chat.dto.response;

import com.signal.domain.chat.progress.ConsultationProgress;
import com.signal.domain.chat.progress.ConsultationProgressStatus;
import com.signal.domain.chat.progress.ConsultationStage;
import com.signal.domain.chat.progress.StageStatus;
import java.util.List;

/**
 * 상담 안내 진행도. progressPercent는 안내 단계 완료율이며 피해 해결률이 아니다.
 * completedCount/totalCount는 같은 값을 "N/M 단계"로 보여 주기 위한 필드다(해당 없는 단계는 totalCount에서 제외).
 */
public record ConsultationProgressResponse(
        ConsultationProgressStatus status,
        ConsultationStage currentStage,
        List<ConsultationStage> completedStages,
        List<StageItem> stages,
        int progressPercent,
        int completedCount,
        int totalCount
) {

    public record StageItem(ConsultationStage stage, String label, StageStatus status) {
    }

    public static ConsultationProgressResponse from(ConsultationProgress progress) {
        List<StageItem> items = progress.stages().entrySet().stream()
                .map(e -> new StageItem(e.getKey(), e.getKey().label(), e.getValue()))
                .toList();
        return new ConsultationProgressResponse(
                progress.status(), progress.currentStage(), progress.completedStages(), items,
                progress.progressPercent(), progress.completedStages().size(), totalCount(progress));
    }

    private static int totalCount(ConsultationProgress progress) {
        return (int) progress.stages().values().stream()
                .filter(status -> status != StageStatus.NOT_APPLICABLE)
                .count();
    }
}
