package com.signal.domain.chat.progress;

import com.signal.domain.chat.engine.SituationType;
import java.util.Arrays;
import java.util.List;

/**
 * 상담 안내 단계. 어떤 단계가 필요한지는 상담 유형({@link SituationType})에 따라 달라지며,
 * 해당 유형에 필요 없는 단계는 {@link StageStatus#NOT_APPLICABLE}로 취급해 진행률 분모에서 제외한다.
 * 진행률은 "안내가 얼마나 진행됐는가"이지 피해 자체의 해결률이 아니다.
 */
public enum ConsultationStage {

    SITUATION_CHECK("상황 파악", true, true),
    EMOTIONAL_SUPPORT("정서 안정", true, true),
    EVIDENCE_PRESERVATION("증거 보존 안내", false, true),
    PLATFORM_REPORT("플랫폼 신고 안내", false, true),
    AGENCY_GUIDANCE("전문 기관 안내", true, true);

    private final String label;
    private final boolean requiredForGeneral;
    private final boolean requiredForImageAbuse;

    ConsultationStage(String label, boolean requiredForGeneral, boolean requiredForImageAbuse) {
        this.label = label;
        this.requiredForGeneral = requiredForGeneral;
        this.requiredForImageAbuse = requiredForImageAbuse;
    }

    public String label() {
        return label;
    }

    public boolean isApplicableTo(SituationType type) {
        return type == SituationType.IMAGE_ABUSE ? requiredForImageAbuse : requiredForGeneral;
    }

    public static List<ConsultationStage> applicableTo(SituationType type) {
        return Arrays.stream(values()).filter(stage -> stage.isApplicableTo(type)).toList();
    }
}
