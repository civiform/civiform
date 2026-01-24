# --- !Ups
alter table applications add column submitter_email varchar(255);

# --- !Downs
alter table applications drop column submitter_email;
