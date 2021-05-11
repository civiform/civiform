# --- Add slug for deep linking.

# --- !Ups
alter table programs add column slug varchar;

# --- !Downs
alter table programs drop column slug;
