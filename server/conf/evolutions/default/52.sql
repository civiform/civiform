# --- !Ups

CREATE TABLE IF NOT EXISTS civiform_settings (
  id BIGSERIAL PRIMARY KEY NOT NULL,
  settings JSONB NOT NULL,
  create_time TIMESTAMP NOT NULL,
  created_by VARCHAR NOT NULL
);

CREATE UNIQUE INDEX civiform_settings_create_time
ON civiform_settings(create_time);

# --- !Downs

DROP INDEX IF EXISTS civiform_settings_create_time;

DROP TABLE IF EXISTS civiform_settings;
