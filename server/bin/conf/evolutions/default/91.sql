--Add question settings for map questions

# --- !Ups
alter table if exists questions add column if not exists question_settings jsonb;

# --- !Downs
alter table if exists questions drop column question_settings;
