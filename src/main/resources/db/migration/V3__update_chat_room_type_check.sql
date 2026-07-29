ALTER TABLE chat_room DROP CONSTRAINT chat_room_chat_room_type_check;
ALTER TABLE chat_room ADD CONSTRAINT chat_room_chat_room_type_check CHECK (chat_room_type IN ('TEAM', 'DIRECT', 'GROUP'));