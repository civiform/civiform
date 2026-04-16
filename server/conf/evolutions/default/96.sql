# --- Add is_initial_question and initial_question_id columns to questions table

# --- !Ups
ALTER TABLE IF EXISTS questions ADD COLUMN IF NOT EXISTS is_initial_question boolean DEFAULT FALSE;
ALTER TABLE IF EXISTS questions ADD COLUMN IF NOT EXISTS initial_question_id bigint;

# --- !Downs
ALTER TABLE IF EXISTS questions DROP COLUMN IF EXISTS initial_question_id;
ALTER TABLE IF EXISTS questions DROP COLUMN IF EXISTS is_initial_question;
