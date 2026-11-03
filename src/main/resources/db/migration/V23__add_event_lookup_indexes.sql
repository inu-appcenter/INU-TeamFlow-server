CREATE INDEX IF NOT EXISTS idx_event_personal_created_by_is_single_start_at
    ON event (created_by, is_single, start_at)
    WHERE team_id IS NULL;

CREATE INDEX IF NOT EXISTS idx_event_participant_team_member_id
    ON event_participant (team_member_id);

CREATE INDEX IF NOT EXISTS idx_team_member_user_id
    ON team_member (user_id);

CREATE INDEX IF NOT EXISTS idx_recurrence_rule_by_day_recurrence_rule_id
    ON recurrence_rule_by_day (recurrence_rule_id);
