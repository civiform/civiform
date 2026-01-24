# --- !Ups

ALTER TABLE IF EXISTS programs 
ADD COLUMN IF NOT EXISTS localized_short_description jsonb NOT NULL
DEFAULT ('{"isRequired": true, "translations": {"en_US": ""}}')::jsonb;

ALTER TABLE IF EXISTS programs 
ADD COLUMN IF NOT EXISTS application_steps jsonb NOT NULL
DEFAULT ('[]')::jsonb;

# --- !Downs

ALTER TABLE IF EXISTS programs 
DROP COLUMN IF EXISTS localized_short_description;

ALTER TABLE IF EXISTS programs 
DROP COLUMN IF EXISTS application_steps;
