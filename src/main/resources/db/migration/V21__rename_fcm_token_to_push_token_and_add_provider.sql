-- Expo 푸시 토큰을 함께 저장하기 위해 FCM 전용 이름을 push로 변경
ALTER TABLE fcm_token RENAME TO push_token;
ALTER TABLE push_token RENAME COLUMN fcm_token_id TO push_token_id;
ALTER TABLE push_token RENAME COLUMN fcm_token TO token;
ALTER TABLE push_token RENAME CONSTRAINT fcm_token_pkey TO push_token_pkey;
ALTER SEQUENCE fcm_token_fcm_token_id_seq RENAME TO push_token_push_token_id_seq;

-- 토큰 발급 제공자 (FCM: Firebase SDK, EXPO: Expo Push Token). 기존 토큰은 모두 웹 FCM 토큰.
ALTER TABLE push_token ADD COLUMN provider VARCHAR(20) NOT NULL DEFAULT 'FCM';

ALTER TABLE push_token ADD CONSTRAINT push_token_provider_check
    CHECK (provider IN ('FCM', 'EXPO'));
