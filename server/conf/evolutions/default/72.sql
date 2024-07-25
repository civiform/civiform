# --- Add suffix column to applicants table

# --- !Ups
CREATE EXTENSION IF NOT EXISTS pg_trgm;
CREATE EXTENSION IF NOT EXISTS btree_gin;
ALTER TABLE applicants add column IF NOT EXISTS suffix varchar;

# --- !Downs
ALTER TABLE applicants DROP column IF EXISTS suffix;
DROP EXTENSION IF EXISTS pg_trgm;
DROP EXTENSION IF EXISTS btree_gin;