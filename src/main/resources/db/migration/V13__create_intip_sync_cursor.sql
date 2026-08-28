CREATE TABLE intip_sync_cursor (
    id BIGINT PRIMARY KEY,
    last_processed_notice_id BIGINT,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
);

INSERT INTO intip_sync_cursor (id, last_processed_notice_id, created_at, updated_at)
VALUES (1, NULL, now(), now());
