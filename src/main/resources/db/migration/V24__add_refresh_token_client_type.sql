-- 웹/모바일 리프레시 토큰을 따로 저장하기 위해 클라이언트 타입 추가. 기존 토큰은 모두 웹 토큰.
ALTER TABLE refresh_token ADD COLUMN client_type VARCHAR(20) NOT NULL DEFAULT 'WEB';

ALTER TABLE refresh_token ADD CONSTRAINT refresh_token_client_type_check
    CHECK (client_type IN ('WEB', 'MOBILE'));

-- user_id 단일 UNIQUE 제약은 Hibernate가 생성해 이름이 환경마다 다르므로 조회해서 삭제
DO $$
DECLARE
    user_id_unique_name TEXT;
BEGIN
    SELECT con.conname INTO user_id_unique_name
    FROM pg_constraint con
    JOIN pg_attribute att ON att.attrelid = con.conrelid AND att.attnum = con.conkey[1]
    WHERE con.conrelid = 'refresh_token'::regclass
      AND con.contype = 'u'
      AND array_length(con.conkey, 1) = 1
      AND att.attname = 'user_id';

    IF user_id_unique_name IS NOT NULL THEN
        EXECUTE format('ALTER TABLE refresh_token DROP CONSTRAINT %I', user_id_unique_name);
    END IF;
END $$;

ALTER TABLE refresh_token ADD CONSTRAINT refresh_token_user_id_client_type_key
    UNIQUE (user_id, client_type);
