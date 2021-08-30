# --- Add a column to accounts that determines whether an account is a global admin.
# --- Also ensure that no global admins can be program admins.

# --- !Ups
alter table accounts add column global_admin boolean default false constraint ck_account_admin CHECK(admin_of is null or array_length(admin_of, 1) = 0 or global_admin = false);

# --- !Downs
alter table accounts drop column global_admin;