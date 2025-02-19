# --- !Ups

ALTER TABLE applications ADD COLUMN IF NOT EXISTS eligibility_determination VARCHAR DEFAULT 'NOT_COMPUTED' NOT NULL;

# --- !Downs
ALTER TABLE applications DROP COLUMN IF EXISTS eligibility_determination;
