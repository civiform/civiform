# --- !Ups

CREATE TABLE IF NOT EXISTS persisted_durable_jobs (
  id BIGSERIAL PRIMARY KEY,
  job_name VARCHAR NOT NULL,
  execution_time TIMESTAMP NOT NULL,
  success_time TIMESTAMP,
  create_time TIMESTAMP NOT NULL,
  remaining_attempts SMALLINT NOT NULL,
  error_message VARCHAR
);

CREATE INDEX index_persisted_durable_jobs_by_success_time
  ON persisted_durable_jobs(success_time);
CREATE INDEX index_persisted_durable_jobs_by_execution_time
  ON persisted_durable_jobs(execution_time);

# --- !Downs

DROP INDEX IF EXISTS index_persisted_durable_jobs_by_success_time;
DROP INDEX IF EXISTS index_persisted_durable_jobs_by_execution_time;
DROP TABLE IF EXISTS persisted_durable_jobs;
