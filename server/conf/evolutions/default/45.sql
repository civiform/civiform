# --- Drops export definitions

# --- !Ups

alter table programs drop column if exists export_definitions;

# --- !Downs
alter table programs add export_definitions jsonb;

update programs set export_definitions = '[]' where export_definitions is null;
