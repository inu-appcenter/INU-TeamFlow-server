-- prod는 baseline-version 13이라 V13이 실행되지 않아 커서 싱글턴 행이 없다.
-- (부트스트랩 SQL은 pg_dump 스키마 전용이라 테이블만 있고 데이터가 없음)
-- 행이 없으면 IntipSyncService.sync()가 매 실행마다 예외로 중단된다.
-- dev에는 V13이 이미 넣어둔 행이 있으므로 충돌 시 그냥 넘어간다.
INSERT INTO intip_sync_cursor (id, last_processed_notice_id, created_at, updated_at)
VALUES (1, NULL, now(), now())
ON CONFLICT (id) DO NOTHING;

-- 자유 입력 본문 필드를 varchar(255) -> text로 넓힌다.
-- 255를 넘기면 DataIntegrityViolationException으로 400이 아닌 500이 나가는데,
-- 이 필드들엔 DTO 검증(@Size)이 없어서 상한이 사실상 우연에 가까웠다.
-- chat_message.content는 처음부터 text였고 그 선례를 따른다.
-- varchar -> text는 binary coercible이라 테이블 재작성 없이 카탈로그만 갱신된다.
--
-- info_post.content: INTIP 공지 본문 전문이 그대로 저장된다.
--   sync()가 배치 전체를 한 트랜잭션으로 묶어서, 한 건만 초과해도 커서 갱신까지 롤백된다.
ALTER TABLE info_post ALTER COLUMN content TYPE TEXT;
ALTER TABLE team_notice ALTER COLUMN content TYPE TEXT;
ALTER TABLE team ALTER COLUMN description TYPE TEXT;
ALTER TABLE recruitment ALTER COLUMN description TYPE TEXT;
