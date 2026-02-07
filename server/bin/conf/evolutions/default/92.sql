--Add bridge definitions for program level api bridge configuration
# --- !Ups
ALTER TABLE IF EXISTS programs
ADD COLUMN IF NOT EXISTS bridge_definitions jsonb NOT NULL DEFAULT '{}';

# --- !Downs
ALTER TABLE IF EXISTS programs
DROP COLUMN IF EXISTS bridge_definitions;
