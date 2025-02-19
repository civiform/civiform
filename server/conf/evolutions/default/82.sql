# --- !Ups

ALTER TABLE applications ADD COLUMN IF NOT EXISTS eligibility_determination VARCHAR;

UPDATE applications SET eligibility_determination = 'NOT_COMPUTED' WHERE eligibility_determination IS NULL;

# --- !Downs
ALTER TABLE applications DROP COLUMN IF EXISTS eligibility_determination;
