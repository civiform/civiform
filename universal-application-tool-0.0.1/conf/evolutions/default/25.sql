# --- Add a column to accounts that determines whether an account is a global admin.

# --- !Ups
alter table accounts add column global_admin boolean default false;

# --- !Downs
alter table accounts drop column global_admin;