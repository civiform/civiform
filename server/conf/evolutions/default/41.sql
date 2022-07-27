# --- Add creation and modification timestamps to questions.

# --- !Ups
alter table questions add column create_time timestamp;
alter table questions add column last_modified_time timestamp;

# --- !Downs
alter table questions drop column if exists create_time;
alter table questions drop column if exists last_modified_time;
