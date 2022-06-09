# --- Add acls to files

# --- !Ups
CREATE UNIQUE INDEX idx_file_names ON files (name);
ALTER TABLE files ADD COLUMN acls JSONB DEFAULT '{}'::jsonb;

# --- !Downs
DROP INDEX IF EXISTS idx_file_names;
ALTER TABLE files DROP COLUMN IF EXISTS acls;
