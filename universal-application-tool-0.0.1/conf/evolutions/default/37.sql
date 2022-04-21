# --- Add creation and modification timestamps to programs.

# --- !Ups
alter table programs add column create_time timestamp;
alter table programs add column last_modified_time timestamp;

# --- !Downs
alter table programs drop column if exists create_time;
alter table programs drop column if exists last_modified_time;
