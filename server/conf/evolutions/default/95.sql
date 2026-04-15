-- Add unique index on files.name with varchar_pattern_ops to support prefix queries
-- https://www.postgresql.org/docs/8.4/indexes-opclass.html
# --- !Ups
CREATE UNIQUE INDEX IF NOT EXISTS index_file_name_prefix_pattern ON files (name varchar_pattern_ops);
DROP INDEX IF EXISTS index_file_names

# --- !Downs
-- From evolution 39, added IF NOT EXISTS.
CREATE UNIQUE INDEX IF NOT EXISTS index_file_names ON files (name);
DROP INDEX IF EXISTS index_file_name_prefix_pattern;
