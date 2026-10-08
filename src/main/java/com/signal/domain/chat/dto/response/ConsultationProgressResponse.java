package com.signal.domain.chat.dto.response;

import com.signal.domain.chat.progress.ConsultationProgress;
import com.signal.domain.chat.progress.ConsultationProgressStatus;
import com.signal.domain.chat.progress.ConsultationStage;
import com.signal.domain.chat.progress.StageStatus;
import java.util.List;

/** 상담 안내 진행도. progressPercent는 안내 단계 완료율이며 피해 해결률이 아니다. */
public record ConsultationProgressResponse(
        ConsultationProgressStatus status,
        ConsultationStage currentStage,
        List<ConsultationStage> completedStages,
        List<StageItem> stages,
        int progressPercent
) {

    public record StageItem(ConsultationStage stage, String label, StageStatus status) {
    }

    public static ConsultationProgressResponse from(ConsultationProgress progress) {
        List<StageItem> items = progress.stages().entrySet().stream()
                .map(e -> new StageItem(e.getKey(), e.getKey().label(), e.getValue()))
                .toList();
        return new ConsultationProgressResponse(
                progress.status(), progress.currentStage(), progress.completedStages(), items,
                progress.progressPercent());
    }
}
