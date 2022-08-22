 --- Create the ApplicationEvents table.

# --- !Ups

CREATE TABLE IF NOT EXISTS application_events (
  id BIGSERIAL PRIMARY KEY NOT NULL,
  application_id BIGINT,
  actor_id BIGINT,
  event_type VARCHAR NOT NULL,
  details JSONB NOT NULL,
  create_time TIMESTAMP NOT NULL,
  CONSTRAINT fk_application FOREIGN KEY(application_id) REFERENCES applications(id),
  CONSTRAINT fk_actor FOREIGN KEY(actor_id) REFERENCES accounts(id)
);

# --- !Downs

DROP TABLE IF EXISTS application_events;
