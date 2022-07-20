# --- Add status definitions to programs.

# --- !Ups
ALTER TABLE programs ADD COLUMN status_definitions JSONB DEFAULT '{"statuses": []}'::jsonb;

# --- !Downs
ALTER TABLE programs DROP COLUMN IF EXISTS status_definitions;
