# --- !Ups
CREATE TABLE IF NOT EXISTS application_statuses (
                                                  id BIGSERIAL PRIMARY KEY NOT NULL,
                                                  program_name VARCHAR NOT NULL,
                                                  status_definitions JSONB NOT NULL,
                                                  status_lifecycle_stage VARCHAR NOT NULL,
                                                  create_time TIMESTAMP
);

# --- !Downs
DROP TABLE IF EXISTS application_statuses CASCADE;
