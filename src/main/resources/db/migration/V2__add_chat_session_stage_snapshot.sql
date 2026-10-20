-- 상담 단계별 안내 상태(JSON) 저장용 컬럼
ALTER TABLE chat_sessions ADD COLUMN stage_snapshot TEXT NULL;
