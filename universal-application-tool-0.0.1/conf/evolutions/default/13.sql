# --- Add Program Export Definitions

# --- !Ups

alter table programs add export_definitions jsonb;

# --- !Downs

alter table programs drop column if exists export_definitions;
