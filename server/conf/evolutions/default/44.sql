# --- Add database triggers to set the latest_status column on creating an application_event or
# --- updating an application.
# --- For rationale behind choosing database triggers, see:
# ---   https://github.com/civiform/civiform/issues/3269#issuecomment-1232193163
# --- Relevant documentation:
# --- Trigger functions: https://www.postgresql.org/docs/current/sql-createtrigger.html
# --- Create trigger statement: https://www.postgresql.org/docs/current/sql-createtrigger.html

# --- !Ups

alter table applications add column if not exists latest_status varchar;

-- Retrieves the status text from the latest event corresponding to a status change. If no
-- application event entries exist, the column is set to NULL.
CREATE OR REPLACE FUNCTION retrieve_latest_application_status(app_id applications.id%TYPE) RETURNS text AS $$
  SELECT
      (CASE WHEN details->'status_event'->>'status_text' = ''
        THEN NULL
        ELSE details->'status_event'->>'status_text'
       END) AS latest_status
      FROM application_events
      WHERE
        application_id = app_id AND
        details->>'event_type' = 'STATUS_CHANGE'
      ORDER BY create_time DESC
      LIMIT 1
$$ LANGUAGE SQL;

-- A function that sets the "latest_status" column anytime an application is updated.
-- Note: The "NEW" variable corresponds to the application record to be inserted / updated.
CREATE OR REPLACE FUNCTION process_latest_status_on_application_change() RETURNS TRIGGER AS $$
  BEGIN
    NEW.latest_status := retrieve_latest_application_status(NEW.id);;
    RETURN NEW;;
  END;;
$$ LANGUAGE plpgsql;

-- A function that sets the "latest_status" column on the referenced application record anytime
-- an event record is updated.
-- Note: The "NEW" variable corresponds to the application event record to be inserted / updated.
CREATE OR REPLACE FUNCTION process_latest_status_on_application_event_change() RETURNS TRIGGER AS $$
  BEGIN
    IF ((NEW.details->>'event_type') = 'STATUS_CHANGE') THEN
      UPDATE applications
        -- Don't always use the status event being modified since we care only about setting the
        -- latest_status column based on the most recent application event, which isn't necessarily
        -- the event being modified.
        SET latest_status = retrieve_latest_application_status(NEW.application_id)
      WHERE id = NEW.application_id;;
    END IF;;
    RETURN NULL;; -- result is ignored for triggers that execute after an insert
  END;;
$$ LANGUAGE plpgsql;

-- Triggers are set for any insert/updates to the applications / application_events tables. The
-- application_events trigger is necessary in order to ensure the latest_status field is kept up to
-- date on the application. The application trigger provides defense against application code
-- attempting to explicitly set / modify the latest_status column. This column is intended to be
-- readonly and the trigger enforces that even if application code attempts otherwise. There are
-- unit tests to assert this in ApplicationTest.java.
CREATE TRIGGER application_change BEFORE INSERT OR UPDATE ON applications
    FOR EACH ROW EXECUTE FUNCTION process_latest_status_on_application_change();
CREATE TRIGGER application_event_change AFTER INSERT OR UPDATE ON application_events
    FOR EACH ROW EXECUTE FUNCTION process_latest_status_on_application_event_change();

# --- !Downs

DROP TRIGGER application_event_change ON application_events;
DROP TRIGGER application_change ON applications;
DROP FUNCTION IF EXISTS process_latest_status_on_application_event_change;
DROP FUNCTION IF EXISTS process_latest_status_on_application_change;
DROP FUNCTION IF EXISTS retrieve_latest_application_status;
alter table applications drop column if exists latest_status;
