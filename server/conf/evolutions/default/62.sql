# --- Add primary applicant information columns

# --- !Ups
CREATE EXTENSION IF NOT EXISTS pg_trgm;
CREATE EXTENSION IF NOT EXISTS btree_gin;

ALTER TABLE applicants add column IF NOT EXISTS first_name varchar;
ALTER TABLE applicants add column IF NOT EXISTS middle_name varchar;
ALTER TABLE applicants add column IF NOT EXISTS last_name varchar;
ALTER TABLE applicants add column IF NOT EXISTS email_address varchar;
ALTER TABLE applicants add column IF NOT EXISTS country_code varchar;
ALTER TABLE applicants add column IF NOT EXISTS phone_number varchar;
ALTER TABLE applicants add column IF NOT EXISTS date_of_birth date;

CREATE INDEX IF NOT EXISTS index_first_name ON applicants USING gin (first_name gin_trgm_ops);
CREATE INDEX IF NOT EXISTS index_middle_name ON applicants USING gin (middle_name gin_trgm_ops);
CREATE INDEX IF NOT EXISTS index_last_name ON applicants USING gin (last_name gin_trgm_ops);
CREATE INDEX IF NOT EXISTS index_name_suffix ON applicants USING gin (name_suffix gin_trgm_ops);
CREATE INDEX IF NOT EXISTS index_email_address ON applicants (email_address);
CREATE INDEX IF NOT EXISTS index_country_code ON applicants USING gin (country_code);
CREATE INDEX IF NOT EXISTS index_phone_number ON applicants USING gin (phone_number gin_trgm_ops);
CREATE INDEX IF NOT EXISTS index_date_of_birth ON applicants (date_of_birth);

# --- !Downs
DROP INDEX IF EXISTS index_first_name;
DROP INDEX IF EXISTS index_middle_name;
DROP INDEX IF EXISTS index_last_name;
DROP INDEX IF EXISTS index_name_suffix;
DROP INDEX IF EXISTS index_email_address;
DROP INDEX IF EXISTS index_country_code;
DROP INDEX IF EXISTS index_phone_number;
DROP INDEX IF EXISTS index_date_of_birth;

ALTER TABLE applicants DROP column IF EXISTS first_name;
ALTER TABLE applicants DROP column IF EXISTS middle_name;
ALTER TABLE applicants DROP column IF EXISTS last_name;
ALTER TABLE applicants DROP column IF EXISTS email_address;
ALTER TABLE applicants DROP column IF EXISTS country_code;
ALTER TABLE applicants DROP column IF EXISTS phone_number;
ALTER TABLE applicants DROP column IF EXISTS date_of_birth;

DROP EXTENSION IF EXISTS pg_trgm;
DROP EXTENSION IF EXISTS btree_gin;