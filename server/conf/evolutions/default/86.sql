# --- !Ups
create table if not exists geojson_map_data (
  endpoint varchar not null,
  geojson jsonb not null,
  create_time timestamp not null,
  update_time timestamp not null);

# --- !Downs
drop table if exists geojson_map_data;
