# --- !Ups
create table if not exists geo_json_data (
  endpoint varchar not null,
  geo_json jsonb not null,
  create_time timestamp not null,
  update_time timestamp not null);

# --- !Downs
drop table if exists geojson_map_data;
