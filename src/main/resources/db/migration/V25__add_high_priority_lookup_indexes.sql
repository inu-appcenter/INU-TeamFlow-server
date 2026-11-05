CREATE INDEX IF NOT EXISTS idx_notification_receiver_id_created_at
    ON notification (receiver_id, created_at DESC);

CREATE INDEX IF NOT EXISTS idx_notification_unread_receiver_id
    ON notification (receiver_id)
    WHERE is_read = false;

CREATE INDEX IF NOT EXISTS idx_push_token_created_by_token
    ON push_token (created_by, token);

CREATE INDEX IF NOT EXISTS idx_push_token_token
    ON push_token (token);

CREATE INDEX IF NOT EXISTS idx_event_team_id_is_single_start_at
    ON event (team_id, is_single, start_at);

CREATE INDEX IF NOT EXISTS idx_event_reminder_single_unfinished_start_at
    ON event (start_at)
    WHERE is_single = true AND is_finished = false;

CREATE INDEX IF NOT EXISTS idx_recruitment_info_post_id_created_at
    ON recruitment (info_post_id, created_at DESC);

CREATE INDEX IF NOT EXISTS idx_info_post_image_info_post_id_sort_order
    ON info_post_image (info_post_id, sort_order);

CREATE INDEX IF NOT EXISTS idx_team_notice_image_team_notice_id_sort_order
    ON team_notice_image (team_notice_id, sort_order);

CREATE INDEX IF NOT EXISTS idx_chat_room_member_user_id_chat_room_id
    ON chat_room_member (user_id, chat_room_id);

CREATE INDEX IF NOT EXISTS idx_vote_availability_vote_time_slot_id
    ON vote_availability (vote_time_slot_id);
