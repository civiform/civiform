-- Add enumerator_initial_question_id column to questions table for the new enumerator flow.
# --- !Ups
ALTER TABLE questions
ADD COLUMN IF NOT EXISTS enumerator_initial_question_id bigint;

# --- !Downs
ALTER TABLE questions
DROP COLUMN IF EXISTS enumerator_initial_question_id;
