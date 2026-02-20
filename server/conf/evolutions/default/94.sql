# --- Add original_applicant_id to applications to track
# --- the original owner during an account merge
# --- !Ups
ALTER TABLE IF EXISTS applications
ADD COLUMN IF NOT EXISTS original_applicant_id bigint;

# --- !Downs
ALTER TABLE IF EXISTS applications
DROP COLUMN IF EXISTS original_applicant_id;
