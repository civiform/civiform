# --- !Ups

ALTER TABLE accounts ADD COLUMN IF NOT EXISTS last_activity_time timestamp;

# --- !Downs
ALTER TABLE accounts DROP COLUMN IF EXISTS last_activity_time;
