-- 전용 탐지 모델 결과의 합성 가능성 등급, 모델 정보, 출처 신호
ALTER TABLE deepfake_detections
    ADD COLUMN band VARCHAR(20) NULL,
    ADD COLUMN assessment_basis VARCHAR(20) NULL,
    ADD COLUMN undetermined_reason VARCHAR(30) NULL,
    ADD COLUMN model_name VARCHAR(100) NULL,
    ADD COLUMN model_version VARCHAR(100) NULL;

CREATE TABLE deepfake_detection_signals (
    deepfake_detection_id BIGINT NOT NULL,
    signal_order INT NOT NULL,
    signal_code VARCHAR(40) NULL,
    PRIMARY KEY (deepfake_detection_id, signal_order),
    CONSTRAINT fk_deepfake_detection_signals_detection
        FOREIGN KEY (deepfake_detection_id) REFERENCES deepfake_detections (id)
);
