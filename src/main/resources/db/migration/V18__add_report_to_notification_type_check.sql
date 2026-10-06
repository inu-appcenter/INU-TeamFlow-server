ALTER TABLE notification DROP CONSTRAINT notification_type_check;

ALTER TABLE notification ADD CONSTRAINT notification_type_check
    CHECK (type IN ('NOTICE', 'INVITE', 'APPLICATION', 'CALENDAR', 'CHAT', 'REPORT'));
