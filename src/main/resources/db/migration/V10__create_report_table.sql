CREATE TABLE report (
                        report_id BIGSERIAL PRIMARY KEY,
                        reporter_id BIGINT NOT NULL,
                        reporter_name VARCHAR(255) NOT NULL,
                        target_type VARCHAR(20) NOT NULL,
                        target_post_id BIGINT,
                        target_post_title VARCHAR(255),
                        target_user_id BIGINT,
                        target_user_name VARCHAR(255),
                        reason VARCHAR(20) NOT NULL,
                        detail VARCHAR(1000),
                        status VARCHAR(20) NOT NULL,
                        created_at TIMESTAMP NOT NULL,
                        updated_at TIMESTAMP NOT NULL
);