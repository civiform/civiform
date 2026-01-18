# --- !Ups

CREATE OR REPLACE FUNCTION fn_validate_draft_question_uniqueness() RETURNS TRIGGER
  AS $validate_draft_question_uniqueness$
  DECLARE
    existing_draft_count integer;;
  BEGIN
    existing_draft_count := (
      SELECT COUNT(*)
      FROM questions JOIN versions_questions
        ON questions.id = versions_questions.questions_id
      WHERE questions.name = NEW.name
        AND versions_questions.versions_id IN (SELECT id FROM versions WHERE versions.lifecycle_stage = 'draft' LIMIT 1)
    );;

    IF existing_draft_count > 0 THEN
      RAISE EXCEPTION 'Question % already has a draft!', NEW.name USING ERRCODE = 'integrity_constraint_violation';;
    END IF;;

    RETURN NEW;;
  END;;
$validate_draft_question_uniqueness$ LANGUAGE plpgsql;

CREATE TRIGGER validate_draft_question_uniqueness BEFORE INSERT
  ON questions FOR EACH ROW EXECUTE PROCEDURE fn_validate_draft_question_uniqueness();

# --- !Downs

DROP TRIGGER IF EXISTS validate_draft_question_uniqueness ON questions;
DROP FUNCTION IF EXISTS fn_validate_draft_question_uniqueness;
