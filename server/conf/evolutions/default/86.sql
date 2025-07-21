# --- !Ups
create table if not exists map_data (
  endpoint TEXT NOT NULL,
  geojson JSONB NOT NULL,
  last_updated TIMESTAMP NOT NULL
);

# --- !Downs
drop table if exists map_data;
