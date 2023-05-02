# --- Add acls to programs

# --- !Ups
ALTER TABLE programs ADD COLUMN acls JSONB DEFAULT '{}'::jsonb;

# --- !Downs
ALTER TABLE files DROP COLUMN IF EXISTS acls;
