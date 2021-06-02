# --- !Ups
alter table questions drop column if exists path;

# --- !Downs
alter table questions add column if not exists path varchar;
