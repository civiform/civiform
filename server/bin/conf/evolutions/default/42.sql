 --- Create the ApplicationEvents table.

# --- !Ups

CREATE TABLE IF NOT EXISTS application_events (
  id BIGSERIAL PRIMARY KEY NOT NULL,
  application_id BIGINT,
  creator_id BIGINT,
  event_type VARCHAR NOT NULL,
  details JSONB NOT NULL,
  create_time TIMESTAMP NOT NULL,
  CONSTRAINT fk_application FOREIGN KEY(application_id) REFERENCES applications(id),
  CONSTRAINT fk_creator FOREIGN KEY(creator_id) REFERENCES accounts(id)
);

 CREATE UNIQUE INDEX index_application_events_by_application ON application_events (application_id);
 CREATE UNIQUE INDEX index_application_events_by_creator ON application_events (creator_id);

# --- !Downs

DROP TABLE IF EXISTS application_events;
