# --- !Ups
alter table applications add column if not exists create_time timestamp;
update applications set create_time = current_timestamp where create_time is null;

# --- !Downs
alter table applications drop column if exists create_time;