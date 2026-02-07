# --- !Ups

ALTER TABLE programs ADD COLUMN IF NOT EXISTS notification_preferences varchar[] DEFAULT '{ EMAIL_PROGRAM_ADMIN_ALL_SUBMISSIONS }';

# --- !Downs

ALTER TABLE programs DROP COLUMN IF EXISTS notification_preferences;
