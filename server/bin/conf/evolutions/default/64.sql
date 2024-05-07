# --- Remove legacy program columns

# --- !Ups
alter table programs drop column if exists legacy_localized_name;
alter table programs drop column if exists legacy_localized_description;

# --- !Downs
alter table programs add column if not exists legacy_localized_name jsonb;
alter table programs add column if not exists legacy_localized_description jsonb;


