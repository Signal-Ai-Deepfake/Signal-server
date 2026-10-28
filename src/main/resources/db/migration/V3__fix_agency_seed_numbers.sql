-- 기관 시드의 폐지·오기 번호 정정 (data.sql은 INSERT IGNORE라 이미 시드된 운영 DB를 갱신하지 못한다).
-- 값이 아직 옛 시드 그대로일 때만 바꾸므로, 운영에서 직접 고친 값은 덮어쓰지 않는다.

-- 2: 182는 경찰 비긴급 상담 번호라 사이버범죄 신고 번호가 아니다 -> 온라인 신고(ecrm)와 112(긴급)로 안내
UPDATE agencies
SET name = '경찰 사이버범죄 신고', phone = '112', available_hours = '온라인 신고 24시간 (112는 긴급 신고)'
WHERE id = 2 AND phone = '182';

-- 3: 1393은 2024-01부터 109로 통합됐다(보건복지부 발표)
UPDATE agencies
SET name = '자살예방 상담전화', phone = '109'
WHERE id = 3 AND phone = '1393';
