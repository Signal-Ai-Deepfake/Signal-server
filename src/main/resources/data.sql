-- 실제 지원 기관 참고 데이터. 고정 id를 명시하고 INSERT IGNORE를 사용해 매 기동마다
-- 재실행돼도 안전하도록(idempotent) 한다.

INSERT IGNORE INTO agencies (id, name, phone, website, available_hours) VALUES
    (1, '디지털성범죄피해자지원센터', '02-735-8994', 'https://d4u.stop.or.kr', '평일 09:00~18:00 (야간·휴일 자동응답)'),
    (2, '경찰청 사이버수사국', '182', 'https://ecrm.police.go.kr', '24시간'),
    (3, '한국생명존중희망재단 자살예방상담전화', '1393', 'https://www.spckorea.or.kr', '24시간'),
    (4, '정신건강 위기상담전화', '1577-0199', 'https://www.mentalhealth.go.kr', '24시간');

INSERT IGNORE INTO agency_supported_actions (agency_id, supported_action_order, supported_action) VALUES
    (1, 0, '삭제 지원'),
    (1, 1, '법률 상담 연계'),
    (1, 2, '심리 상담 연계'),
    (2, 0, '사이버 범죄 신고'),
    (2, 1, '수사 요청'),
    (3, 0, '위기 상담'),
    (3, 1, '긴급 개입'),
    (4, 0, '정신건강 위기 상담');

INSERT IGNORE INTO agency_supported_situation_types (agency_id, supported_situation_type_order, supported_situation_type) VALUES
    (1, 0, 'DEEPFAKE_IMAGE'),
    (1, 1, 'IMAGE_ABUSE'),
    (2, 0, 'DEEPFAKE_IMAGE'),
    (2, 1, 'IMAGE_ABUSE'),
    (3, 0, 'CRISIS'),
    (4, 0, 'CRISIS');
