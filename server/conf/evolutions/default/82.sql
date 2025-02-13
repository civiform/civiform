# --- !Ups

ALTER TABLE applications ADD COLUMN IF NOT EXISTS eligibility_determination varchar;

UPDATE applications SET eligibility_determination = 'NOT_COMPUTED' where eligibility_determination is null;

# --- !Downs
ALTER TABLE applications DROP COLUMN IF EXISTS eligibility_determination;