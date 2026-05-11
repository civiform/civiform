# --- Add initial_question_id column to questions table
# --- !Ups
ALTER TABLE IF EXISTS questions
ADD COLUMN IF NOT EXISTS initial_question_id bigint;

# --- !Downs
ALTER TABLE IF EXISTS questions
DROP COLUMN IF EXISTS initial_question_id;
