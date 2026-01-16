# --- Add versioning and lifecycle management.

# --- !Ups

create index questions_by_name on questions (name);
create index programs_by_stage on programs (lifecycle_stage);

# --- !Downs

drop index if exists questions_by_name;
drop index if exists programs_by_stage;
