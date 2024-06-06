# --- !Ups
CREATE TABLE IF NOT EXISTS application_statuses( id BIGSERIAL PRIMARY KEY NOT NULL,
                                                 program_name varchar not null,
                                                 status_definitions jsonb not null,
                                                 status_lifecycle_stage varchar not null,
                                                 create_time timestamp
);

# --- !Downs
drop table if exists application_statuses cascade;
