# --- !Ups

alter table programs add column program_type varchar;
update programs set program_type = 'DEFAULT' where program_type is null;

# --- !Downs

alter table programs drop column if exists program_type;
