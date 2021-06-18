# --- !Ups
alter table applications add column if not exists preferred_locale varchar;

# --- !Downs
alter table applications drop column if exists preferred_locale;
