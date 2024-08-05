# --- Add suffix column to applicants table

# --- !Ups
ALTER TABLE applicants add column IF NOT EXISTS suffix varchar;

# --- !Downs
ALTER TABLE applicants DROP column IF EXISTS suffix;