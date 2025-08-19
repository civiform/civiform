--- Create the Application DeletionRecord and AccountDeletionR table.

# --- !Ups

CREATE TABLE IF NOT EXISTS Application_deletion_records (
                                                id BIGSERIAL PRIMARY KEY NOT NULL,
                                                application_id BIGINT,
                                                program_name VARCHAR NOT NULL,
                                                deletion_trigger VARCHAR NOT NULL,
                                                delete_time TIMESTAMP NOT NULL);
CREATE TABLE IF NOT EXISTS Account_deletion_records (
                                                       id BIGSERIAL PRIMARY KEY NOT NULL,
                                                       account_id BIGINT NOT NULL,
                                                      applicant_ids  ,
                                                       deletion_trigger VARCHAR NOT NULL,
                                                       delete_time TIMESTAMP NOT NULL);

# --- !Downs

DROP TABLE IF EXISTS Application_deletion_records;
DROP TABLE IF EXISTS Account_deletion_records;
