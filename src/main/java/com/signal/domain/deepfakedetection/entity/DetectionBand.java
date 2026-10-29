package com.signal.domain.deepfakedetection.entity;

/**
 * 이미지가 합성(AI 생성·편집)되었을 가능성의 등급. 퍼센트 대신 이 등급을 사용자에게 보여 준다.
 *
 * <ul>
 *   <li>{@link #LOW}: 합성 가능성이 낮음. 진짜라는 보증은 아니다.</li>
 *   <li>{@link #UNCERTAIN}: 모델 점수가 중간이라 어느 쪽이라고 말하기 어려움.</li>
 *   <li>{@link #HIGH}: 합성 가능성이 높음. 모델 점수가 높거나 생성 도구의 흔적이 발견된 경우이며, "가짜 확정"이 아니다.</li>
 *   <li>{@link #UNDETERMINED}: 입력이 모델이 믿을 만한 수준이 아니어서(저해상도 등) 판단하지 않음. 이유는 {@link UndeterminedReason}.</li>
 * </ul>
 */
public enum DetectionBand {
    LOW, UNCERTAIN, HIGH, UNDETERMINED
}
