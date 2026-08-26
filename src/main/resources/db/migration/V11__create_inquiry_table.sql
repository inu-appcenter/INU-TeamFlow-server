CREATE TABLE inquiry (
                         inquiry_id BIGSERIAL PRIMARY KEY,
                         inquirer_id BIGINT NOT NULL,
                         inquirer_name VARCHAR(255) NOT NULL,
                         type VARCHAR(20) NOT NULL,
                         detail VARCHAR(1000),
                         status VARCHAR(20) NOT NULL,
                         answer VARCHAR(1000),
                         answerer_id BIGINT,
                         answerer_name VARCHAR(255),
                         answered_at TIMESTAMP,
                         created_at TIMESTAMP NOT NULL,
                         updated_at TIMESTAMP NOT NULL
);