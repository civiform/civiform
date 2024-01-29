# --- Add primary applicant information columns

# --- !Ups
CREATE EXTENSION pg_trgm;
CREATE EXTENSION btree_gin;

ALTER TABLE applicants add column first_name varchar;
ALTER TABLE applicants add column middle_name varchar;
ALTER TABLE applicants add column last_name varchar;
ALTER TABLE applicants add column email_address varchar;
ALTER TABLE applicants add column country_code varchar;
ALTER TABLE applicants add column phone_number varchar;
ALTER TABLE applicants add column date_of_birth date;

CREATE INDEX IF NOT EXISTS index_first_name ON applicants USING gin (first_name gin_trgm_ops);
CREATE INDEX IF NOT EXISTS index_middle_name ON applicants USING gin (middle_name gin_trgm_ops);
CREATE INDEX IF NOT EXISTS index_last_name ON applicants USING gin (last_name gin_trgm_ops);
CREATE INDEX IF NOT EXISTS index_email_address ON applicants (email_address);
CREATE INDEX IF NOT EXISTS index_country_code ON applicants USING gin(country_code);
CREATE INDEX IF NOT EXISTS index_phone_number ON applicants USING gin (phone_number gin_trgm_ops);
CREATE INDEX IF NOT EXISTS index_date_of_birth ON applicants (date_of_birth);

# --- !Downs
ALTER TABLE applicants drop column first_name;
ALTER TABLE applicants drop column middle_name;
ALTER TABLE applicants drop column last_name;
ALTER TABLE applicants drop column email_address;
ALTER TABLE applicants drop column country_code;
ALTER TABLE applicants drop column phone_number;
ALTER TABLE applicants drop column date_of_birth;

DROP INDEX IF EXISTS index_first_name;
DROP INDEX IF EXISTS index_middle_name;
DROP INDEX IF EXISTS index_last_name;
DROP INDEX IF EXISTS index_email_address;
DROP INDEX IF EXISTS index_country_code;
DROP INDEX IF EXISTS index_phone_number;
DROP INDEX IF EXISTS index_date_of_birth;