CREATE TABLE report_handle (
                               report_handle_id BIGSERIAL PRIMARY KEY,
                               report_id BIGINT NOT NULL UNIQUE REFERENCES report (report_id),
                               handler_id BIGINT,
                               handler_name VARCHAR(255),
                               post_action VARCHAR(20),
                               post_action_detail VARCHAR(1000),
                               user_action VARCHAR(20),
                               user_action_duration_days INTEGER,
                               user_action_detail VARCHAR(1000),
                               created_at TIMESTAMP NOT NULL,
                               updated_at TIMESTAMP NOT NULL
);
