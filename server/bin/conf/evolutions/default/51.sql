# --- !Ups

ALTER TABLE programs ADD COLUMN acls JSONB DEFAULT '{}'::jsonb;

# --- !Downs

ALTER TABLE programs DROP COLUMN IF EXISTS acls;
