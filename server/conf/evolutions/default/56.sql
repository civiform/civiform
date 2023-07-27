# --- !Ups

CREATE OR REPLACE FUNCTION fn_validate_draft_program_uniqueness() RETURNS TRIGGER
  AS $validate_draft_program_uniqueness$
  DECLARE
existing_draft_count integer;;
BEGIN
    existing_draft_count := (
      SELECT COUNT(*)
      FROM programs JOIN versions_programs
        ON programs.id = versions_programs.programs_id
      WHERE programs.name = NEW.name
        AND versions_programs.versions_id IN (SELECT id FROM versions WHERE versions.lifecycle_stage = 'draft' LIMIT 1)
    );;

    IF existing_draft_count > 0 THEN
      RAISE EXCEPTION 'Program % already has a draft!', NEW.name USING ERRCODE = 'integrity_constraint_violation';;
    END IF;;

    RETURN NEW;;
END;;
$validate_draft_program_uniqueness$ LANGUAGE plpgsql;

CREATE TRIGGER validate_draft_program_uniqueness BEFORE INSERT
  ON programs FOR EACH ROW EXECUTE PROCEDURE fn_validate_draft_program_uniqueness();

# --- !Downs

DROP TRIGGER IF EXISTS validate_draft_program_uniqueness ON programs;
DROP FUNCTION IF EXISTS fn_validate_draft_program_uniqueness;
