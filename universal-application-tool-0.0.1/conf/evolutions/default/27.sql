# --- !Ups
alter table questions drop column path;

# --- !Downs
alter table questions add column path varchar;
