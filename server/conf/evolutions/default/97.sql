# --- Whether the program applies answer-option scores to submitted applications
# --- !Ups
ALTER TABLE IF EXISTS programs
ADD COLUMN IF NOT EXISTS uses_scoring boolean DEFAULT FALSE NOT NULL;

# --- !Downs
ALTER TABLE IF EXISTS programs
DROP COLUMN IF EXISTS uses_scoring;
