# --- Add unique index on files.name with varchar_pattern_ops to support prefix queries
# --- !Ups
CREATE UNIQUE INDEX IF NOT EXISTS index_file_name_pattern ON files (name varchar_pattern_ops);

# --- !Downs
DROP INDEX IF EXISTS index_file_name_pattern;
