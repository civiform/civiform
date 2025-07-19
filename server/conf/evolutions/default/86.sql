# --- !Ups
alter table accounts add last_activity_time timestamp;

# --- !Downs
alter table accounts drop column last_activity_times;
