# --- Add versioning and lifecycle management.

# --- !Ups

alter table questions add repeater_id jsonb;

# --- !Downs

alter table questions drop column if exists repeater_id;