--- Drop status_definitions column in programs table.

# --- !Ups
alter table programs drop column if exists status_definitions;

# --- !Downs
alter table programs add status_definitions jsonb;
