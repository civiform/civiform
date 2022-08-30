# --- Add database triggers to set the latest_status column on creating an application_event or
# --- updating an application.

# --- !Ups

alter table applications add column if not exists latest_status varchar;

CREATE OR REPLACE FUNCTION retrieve_latest_status(app_id applications.id%TYPE) RETURNS text AS $$
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

CREATE OR REPLACE FUNCTION process_application_change() RETURNS TRIGGER AS $$
  BEGIN
    NEW.latest_status := retrieve_latest_status(NEW.id);;
    RETURN NEW;;
  END;;
$$ LANGUAGE plpgsql;

CREATE OR REPLACE FUNCTION process_application_event_change() RETURNS TRIGGER AS $$
  BEGIN
    IF ((NEW.details->>'event_type') = 'STATUS_CHANGE') THEN
      UPDATE applications
        SET latest_status = retrieve_latest_status(NEW.application_id)
      WHERE id = NEW.application_id;;
    END IF;;
    RETURN NULL;; -- result is ignored since this is an AFTER
  END;;
$$ LANGUAGE plpgsql;

CREATE TRIGGER application_event_change
AFTER INSERT OR UPDATE ON application_events
  FOR EACH ROW EXECUTE FUNCTION process_application_event_change();

CREATE TRIGGER application_change BEFORE INSERT OR UPDATE ON applications
    FOR EACH ROW EXECUTE FUNCTION process_application_change();

# --- !Downs

DROP TRIGGER application_change ON applications;
DROP TRIGGER application_event_change ON application_events;
DROP FUNCTION IF EXISTS process_application_event_change;
DROP FUNCTION IF EXISTS process_application_change;
DROP FUNCTION IF EXISTS retrieve_latest_status;
alter table applications drop column if exists latest_status;
