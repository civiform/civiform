--Add question settings for map questions

# --- !Ups
alter table if exists questions add question_settings jsonb;
alter table if exists geo_json_data
add column if not exists id bigserial primary key not null;

# --- !Downs
alter table if exists questions drop column question_settings;
alter table if exists geo_json_data
drop column if exists id;
