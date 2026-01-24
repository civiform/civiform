# --- Replace index on email_address column with trigram index

# --- !Ups
DROP INDEX IF EXISTS index_email_address;
CREATE INDEX IF NOT EXISTS index_email_address ON applicants USING gin (email_address gin_trgm_ops);

# --- !Downs
DROP INDEX IF EXISTS index_email_address;
CREATE INDEX IF NOT EXISTS index_email_address ON applicants (email_address);