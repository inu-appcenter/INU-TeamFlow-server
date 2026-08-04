CREATE TABLE recruitment_scrap (
                                   recruitment_scrap_id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
                                   recruitment_id BIGINT NOT NULL REFERENCES recruitment(recruitment_id),
                                   user_id BIGINT NOT NULL REFERENCES users(user_id),
                                   created_at TIMESTAMP NOT NULL,
                                   updated_at TIMESTAMP NOT NULL,
                                   UNIQUE (recruitment_id, user_id)
);

CREATE TABLE info_post_scrap (
                                 info_post_scrap_id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
                                 info_post_id BIGINT NOT NULL REFERENCES info_post(info_post_id),
                                 user_id BIGINT NOT NULL REFERENCES users(user_id),
                                 created_at TIMESTAMP NOT NULL,
                                 updated_at TIMESTAMP NOT NULL,
                                 UNIQUE (info_post_id, user_id)
);