 --- Create the ApplicationEvents table.

# --- !Ups

CREATE TABLE IF NOT EXISTS application_events (
  id BIGSERIAL PRIMARY KEY NOT NULL,
  application_id BIGINT,
  actor_id BIGINT,
  event_type VARCHAR NOT NULL,
  details JSONB NOT NULL,
  create_time TIMESTAMP NOT NULL,
  constraint fk_application foreign key(application_id) references applications(id),
    constraint fk_actor foreign key(actor_id) references accounts(id)
);

# --- !Downs

DROP TABLE IF EXISTS application_events;
