package com.signal.domain.chat.progress;

import com.signal.domain.chat.engine.SituationType;
import java.util.Map;

/** 세션에 저장되는 단계별 상태. type은 한 번 IMAGE_ABUSE로 분류되면 유지된다(메시지마다 분류가 달라질 수 있어서). */
public record StageSnapshot(SituationType type, Map<ConsultationStage, StageStatus> stages) {
}
