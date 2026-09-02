-- INTIP에서 수집된 공지의 원문 링크. 직접 작성한 정보글은 null.
ALTER TABLE info_post ADD COLUMN source_url VARCHAR(500);