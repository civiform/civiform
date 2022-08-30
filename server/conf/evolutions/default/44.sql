# --- Fix indexes made in evolution 42 to not be unique

# --- !Ups

alter table applications add column if not exists latest_status varchar;

CREATE OR REPLACE FUNCTION process_status_change() RETURNS TRIGGER AS $$
  BEGIN
    IF ((NEW.details->>'event_type') = 'STATUS_CHANGE') THEN
      UPDATE applications
        SET latest_status = (
          CASE WHEN NEW.details->'status_event'->>'status_text' = ''
            THEN NULL
            ELSE NEW.details->'status_event'->>'status_text'
          END)
        WHERE id = NEW.application_id;;
    END IF;;
    RETURN NULL;; -- result is ignored since this is an AFTER
  END;;
$$ LANGUAGE plpgsql;

CREATE TRIGGER status_change
AFTER INSERT OR UPDATE ON application_events
  FOR EACH ROW EXECUTE FUNCTION process_status_change();

CREATE OR REPLACE FUNCTION process_application_change() RETURNS TRIGGER AS $$
  BEGIN
    NEW.latest_status := (SELECT
      (CASE WHEN details->'status_event'->>'status_text' = ''
        THEN NULL
        ELSE details->'status_event'->>'status_text'
       END) AS latest_status
      FROM application_events
      WHERE
        application_id = NEW.id AND
        details->>'event_type' = 'STATUS_CHANGE'
      ORDER BY create_time DESC
      LIMIT 1);;

    RETURN NEW;;
  END;;
$$ LANGUAGE plpgsql;

CREATE TRIGGER application_change BEFORE INSERT OR UPDATE ON applications
    FOR EACH ROW EXECUTE FUNCTION process_application_change();

# --- !Downs

alter table applications drop column if exists latest_status;