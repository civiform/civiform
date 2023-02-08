# --- !Ups

alter table programs add column program_type varchar default 'default';

# --- !Downs

alter table programs drop column if exists program_type;
