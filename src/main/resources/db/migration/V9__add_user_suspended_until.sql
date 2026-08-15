-- 기간 정지(임시 정지) enforcement용 컬럼. 정지 만료 시각, 미정지 시 null.
ALTER TABLE users ADD COLUMN suspended_until TIMESTAMP;
