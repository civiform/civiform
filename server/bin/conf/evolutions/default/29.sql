# --- !Ups
alter table programs add column external_link varchar default '';

# --- !Downs
alter table programs drop column external_link;
