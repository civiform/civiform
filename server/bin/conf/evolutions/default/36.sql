# --- Add a new unique identifier for accounts based on the authentication systems unique ID semantics.
# --- Until we fully migrate from email_address to the new authority_id as the unique account ID both will identify
# --- accounts, first with authority_id if present, then email_address.

# --- !Ups
alter table accounts add authority_id varchar;
alter table accounts add unique (authority_id);
