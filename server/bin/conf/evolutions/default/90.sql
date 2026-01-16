# --- !Ups
alter table if exists geo_json_data
add column if not exists id bigserial primary key not null;

# --- !Downs
alter table if exists geo_json_data
drop column if exists id;
