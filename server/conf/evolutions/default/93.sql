# --- Specificies if the program is available to logged in applicants only
# --- !Ups
ALTER TABLE IF EXISTS programs
ADD COLUMN IF NOT EXISTS login_only boolean DEFAULT FALSE NOT NULL;

# --- !Downs
ALTER TABLE IF EXISTS programs
DROP COLUMN IF EXISTS login_only;
