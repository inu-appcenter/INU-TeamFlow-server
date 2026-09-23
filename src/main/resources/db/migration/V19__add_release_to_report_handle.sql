ALTER TABLE report_handle ADD COLUMN released_at TIMESTAMP;
ALTER TABLE report_handle ADD COLUMN released_by_id BIGINT;
ALTER TABLE report_handle ADD COLUMN released_by_name VARCHAR(255);
