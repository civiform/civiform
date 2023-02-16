# --- !Ups

ALTER TABLE applications
ADD COLUMN submission_duration interval
GENERATED ALWAYS AS (submit_time - create_time) STORED;

# --- !Downs

alter table applications drop column if exists submission_duration;
