# --- Add versioning and lifecycle management.

# --- !Ups

alter table questions add repeater_id bigserial;

# --- !Downs

alter table questions drop column if exists repeater_id;