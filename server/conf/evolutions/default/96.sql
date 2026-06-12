-- Add enumerator_initial_question_id column to questions table for the new enumerator flow.
# --- !Ups
ALTER TABLE IF EXISTS questions
ADD COLUMN IF NOT EXISTS enumerator_initial_question_id bigint;

# --- !Downs
ALTER TABLE IF EXISTS questions
DROP COLUMN IF EXISTS enumerator_initial_question_id;
