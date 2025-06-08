# --- !Ups

CREATE OR REPLACE FUNCTION notify_settings_update() RETURNS TRIGGER AS $$
BEGIN
  -- Payload with ID is unused, but in case we need it in the future.
  PERFORM pg_notify('settings_update', NEW.id::text);;
  RETURN NEW;;
END;;
$$ LANGUAGE plpgsql;

CREATE TRIGGER settings_update_trigger
  AFTER INSERT OR UPDATE OR DELETE
  ON civiform_settings
  FOR EACH ROW
  EXECUTE PROCEDURE notify_settings_update();

# --- !Downs

DROP TRIGGER IF EXISTS settings_update_trigger ON civiform_settings;
DROP FUNCTION IF EXISTS notify_settings_update();
