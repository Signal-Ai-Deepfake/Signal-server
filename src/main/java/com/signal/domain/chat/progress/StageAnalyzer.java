package com.signal.domain.chat.progress;

import com.signal.domain.chat.engine.ChatTurn;
import com.signal.domain.chat.engine.SituationType;
import java.util.List;
import java.util.Map;

public interface StageAnalyzer {

    /**
     * 대화 내용을 바탕으로 각 단계의 안내 상태를 판단한다.
     *
     * @param type     상담 유형(필요한 단계를 결정)
     * @param turns    지금까지의 대화(이번 사용자 메시지와 봇 응답 포함, 오래된 순)
     * @param previous 직전까지 저장된 단계 상태
     * @return 단계별 상태(NOT_STARTED/IN_PROGRESS/COMPLETED). 누락된 단계는 호출부가 보정한다.
     * @throws RuntimeException 분석 실패 시(호출부에서 룰 기반 값으로 대체)
     */
    Map<ConsultationStage, StageStatus> analyze(
            SituationType type, List<ChatTurn> turns, Map<ConsultationStage, StageStatus> previous);
}
