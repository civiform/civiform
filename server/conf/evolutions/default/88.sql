-- Setting up the Settings Cache

# --- !Ups

CREATE OR REPLACE FUNCTION notify_settings_update() RETURNS TRIGGER AS $$
BEGIN
  -- Payload is unused, but may be used/changed in the future.
  PERFORM pg_notify('settings_update', TG_OP);;
  RETURN NEW;;
END;;
$$ LANGUAGE plpgsql;

CREATE OR REPLACE TRIGGER settings_update_trigger
  AFTER INSERT OR UPDATE OR DELETE OR TRUNCATE
  ON civiform_settings
  FOR EACH STATEMENT
  EXECUTE PROCEDURE notify_settings_update();

# --- !Downs

DROP TRIGGER IF EXISTS settings_update_trigger ON civiform_settings;
DROP FUNCTION IF EXISTS notify_settings_update();