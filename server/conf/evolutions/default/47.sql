# --- !Ups

alter table programs add column program_type varchar default 'default';
--update programs set program_type = 'default' where program_type is null;

# --- !Downs

alter table programs drop column if exists program_type;
