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

# --- !Downs

alter table applications drop column if exists latest_status;