--- Add job_type column

# --- !Ups
alter table persisted_durable_jobs
add column if not exists job_type varchar(32) not null
default 'RECURRING';

alter table if exists persisted_durable_jobs
alter column job_type DROP DEFAULT;

# --- !Downs
alter table persisted_durable_jobs drop column if exists job_type;
