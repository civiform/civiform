# --- Migrate from email_address to new authority_id as the unique account ID.
# --- This adds the new ID and makes the two a unique pair until all accounts can be updated to have an authority_id

# --- !Ups
alter table accounts add authority_id varchar;
alter table accounts add unique (authority_id, email_address);

# --- !Downs
alter table accounts drop constraint accounts_email_address_key;
