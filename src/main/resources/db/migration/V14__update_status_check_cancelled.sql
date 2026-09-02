-- Status enum 오타 수정(CANCELED → CANCELLED)에 맞춰 CHECK 제약을 갱신한다.
-- 기존 제약이 'CANCELLED'를 거부하므로 DROP -> UPDATE -> ADD 순서로 실행해야 한다.

ALTER TABLE team_invitation DROP CONSTRAINT team_invitation_invite_status_check;
UPDATE team_invitation SET invite_status = 'CANCELLED' WHERE invite_status = 'CANCELED';
ALTER TABLE team_invitation ADD CONSTRAINT team_invitation_invite_status_check CHECK (invite_status IN ('WAITING', 'ACCEPTED', 'DECLINED', 'CANCELLED'));

ALTER TABLE recruitment_application DROP CONSTRAINT recruitment_application_application_status_check;
UPDATE recruitment_application SET application_status = 'CANCELLED' WHERE application_status = 'CANCELED';
ALTER TABLE recruitment_application ADD CONSTRAINT recruitment_application_application_status_check CHECK (application_status IN ('WAITING', 'ACCEPTED', 'DECLINED', 'CANCELLED'));
