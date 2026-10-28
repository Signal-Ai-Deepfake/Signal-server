-- 실제 지원 기관 참고 데이터. 고정 id를 명시하고 INSERT IGNORE를 사용해 매 기동마다
-- 재실행돼도 안전하도록(idempotent) 한다.
--
-- INSERT IGNORE는 이미 있는 행을 갱신하지 않아서 운영 DB에는 번호 정정이 반영되지 않는다.
-- 그래서 아래 UPDATE로 이 파일의 값을 항상 기준으로 맞춘다(운영 DB에서 직접 고친 값은 다음 기동에 덮어써진다).
-- 번호를 바꿀 때는 챗봇의 AgencyCatalog와 docs/chat-guidance.md의 확인 표도 함께 고친다.
-- (AgencyDataSqlIntegrationTest가 두 곳의 번호가 어긋나면 실패하도록 검증한다.)

INSERT IGNORE INTO agencies (id, name, phone, website, available_hours) VALUES
    (1, '디지털성범죄피해자지원센터', '02-735-8994', 'https://d4u.stop.or.kr', '평일 09:00~18:00 (야간·휴일 자동응답)'),
    (2, '경찰 사이버범죄 신고', '112', 'https://ecrm.police.go.kr', '24시간'),
    (3, '자살예방 상담전화', '109', 'https://www.spckorea.or.kr', '24시간'),
    (4, '정신건강 위기상담전화', '1577-0199', 'https://www.mentalhealth.go.kr', '24시간');

-- 이미 시드된 DB(운영)에도 반영되도록 번호·이름을 이 파일의 값으로 맞춘다.
--   2: 182는 경찰 비긴급 상담 번호라 사이버범죄 신고 번호가 아니다 -> 112(긴급)와 온라인 신고 주소로 안내
--   3: 1393은 2024-01부터 109로 통합됐다(보건복지부 발표)
UPDATE agencies SET name = '디지털성범죄피해자지원센터', phone = '02-735-8994', website = 'https://d4u.stop.or.kr', available_hours = '평일 09:00~18:00 (야간·휴일 자동응답)' WHERE id = 1;
UPDATE agencies SET name = '경찰 사이버범죄 신고', phone = '112', website = 'https://ecrm.police.go.kr', available_hours = '24시간' WHERE id = 2;
UPDATE agencies SET name = '자살예방 상담전화', phone = '109', website = 'https://www.spckorea.or.kr', available_hours = '24시간' WHERE id = 3;
UPDATE agencies SET name = '정신건강 위기상담전화', phone = '1577-0199', website = 'https://www.mentalhealth.go.kr', available_hours = '24시간' WHERE id = 4;

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
