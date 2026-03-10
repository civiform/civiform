# --- Add original_applicant_id to applications to track
# --- the original owner during an account merge
# --- !Ups
ALTER TABLE IF EXISTS applications
ADD COLUMN IF NOT EXISTS original_applicant_id bigint;

CREATE INDEX IF NOT EXISTS index_applications_original_applicant_id
ON applications (original_applicant_id);

# --- !Downs
DROP INDEX IF EXISTS index_applications_original_applicant_id;

ALTER TABLE IF EXISTS applications
DROP COLUMN IF EXISTS original_applicant_id;
