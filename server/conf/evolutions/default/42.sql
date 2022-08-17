 --- Create the ApplicationEvents table.

# --- !Ups

CREATE TABLE IF NOT EXISTS application_events (
  id BIGSERIAL PRIMARY KEY NOT NULL,
  event_type VARCHAR NOT NULL,
  details JSONB NOT NULL,
  create_time TIMESTAMP NOT NULL
);

# --- !Downs

DROP TABLE IF EXISTS application_events;
