# --- Add repeater id reference to questions

# --- !Ups

alter table questions add repeater_id bigint;

# --- !Downs

alter table questions drop column if exists repeater_id;
