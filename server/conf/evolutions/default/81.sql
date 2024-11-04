# --- !Ups

ALTER TABLE programs ADD COLUMN IF NOT EXISTS localized_short_description jsonb DEFAULT ('{"isRequired": true, "translations": {"en_US": ""}}')::jsonb;
ALTER TABLE programs ADD COLUMN IF NOT EXISTS application_steps jsonb DEFAULT ('[]')::jsonb;

# --- !Downs

ALTER TABLE programs DROP COLUMN IF EXISTS localized_short_description;
ALTER TABLE programs DROP COLUMN IF EXISTS application_steps;