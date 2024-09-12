# --- !Ups

ALTER TABLE applications ADD COLUMN IF NOT EXISTS latest_note varchar;
-- Retrieves the note text from the latest event corresponding to a note change. If no
-- application event entries exist, the column is set to NULL.
CREATE OR REPLACE FUNCTION retrieve_latest_application_note(app_id applications.id%TYPE) RETURNS text AS $$
SELECT
  (CASE WHEN details->'note_event'->>'note' = ''
          THEN NULL
        ELSE details->'note_event'->>'note'
    END) AS latest_note
FROM application_events
WHERE
  application_id = app_id AND
  details->>'event_type' = 'NOTE_CHANGE'
ORDER BY create_time DESC
  LIMIT 1
  $$ LANGUAGE SQL;

-- A function that sets the "latest_note" column anytime an application is updated.
-- Note: The "NEW" variable corresponds to the application record to be inserted / updated.
CREATE OR REPLACE FUNCTION process_latest_note_on_application_change() RETURNS TRIGGER AS $$
BEGIN
    NEW.latest_note := retrieve_latest_application_note(NEW.id);;
RETURN NEW;;
END;;
$$ LANGUAGE plpgsql;

-- A function that sets the "latest_note" column on the referenced application record anytime
-- an event record is updated.
-- Note: The "NEW" variable corresponds to the application event record to be inserted / updated.
CREATE OR REPLACE FUNCTION process_latest_note_on_application_event_change() RETURNS TRIGGER AS $$
BEGIN
    IF ((NEW.details->>'event_type') = 'NOTE_CHANGE') THEN
UPDATE applications
-- Don't always use the note event being modified since we care only about setting the
-- latest_note column based on the most recent application event, which isn't necessarily
-- the event being modified.
SET latest_note = retrieve_latest_application_note(NEW.application_id)
WHERE id = NEW.application_id;;
END IF;;
RETURN NULL;; -- result is ignored for triggers that execute after an insert
END;;
$$ LANGUAGE plpgsql;

-- Triggers are set for any insert/updates to the applications / application_events tables. The
-- application_events trigger is necessary in order to ensure the latest_note field is kept up to
-- date on the application. The application trigger provides defense against application code
-- attempting to explicitly set / modify the latest_note column. This column is intended to be
-- readonly and the trigger enforces that even if application code attempts otherwise. There are
-- unit tests to assert this in ApplicationTest.java.
CREATE TRIGGER application_change_on_note BEFORE INSERT OR UPDATE ON applications
                                                     FOR EACH ROW EXECUTE FUNCTION process_latest_note_on_application_change();
CREATE TRIGGER application_event_change_on_note AFTER INSERT OR UPDATE ON application_events
                                                        FOR EACH ROW EXECUTE FUNCTION process_latest_note_on_application_event_change();
# --- !Downs

DROP TRIGGER application_change_on_note ON application_events;
DROP TRIGGER application_event_change_on_note ON applications;
DROP FUNCTION IF EXISTS process_latest_note_on_application_event_change;
DROP FUNCTION IF EXISTS process_latest_note_on_application_change;
DROP FUNCTION IF EXISTS retrieve_latest_application_note;
alter table applications drop column if exists latest_note;

