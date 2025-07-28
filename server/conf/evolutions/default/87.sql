# --- !Ups
create table if not exists geo_json_data (
  endpoint varchar not null,
  geo_json jsonb not null,
  create_time timestamp not null,
  confirm_time timestamp not null);

# --- !Downs
drop table if exists geo_json_data;
