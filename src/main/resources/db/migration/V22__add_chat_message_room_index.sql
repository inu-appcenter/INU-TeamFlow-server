CREATE INDEX IF NOT EXISTS idx_chat_message_room_id_message_id
    ON chat_message (chat_room_id, chat_message_id);