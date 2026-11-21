-- INTIP 공지의 이미지 외 첨부파일. 파일은 저장하지 않고 학교 서버 다운로드 URL만 보관한다.
CREATE TABLE info_post_attachment (
    info_post_attachment_id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    info_post_id BIGINT NOT NULL REFERENCES info_post(info_post_id),
    name VARCHAR(500) NOT NULL,
    url VARCHAR(500) NOT NULL,
    file_type VARCHAR(20),
    sort_order INTEGER NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_info_post_attachment_info_post_id_sort_order
    ON info_post_attachment (info_post_id, sort_order);
