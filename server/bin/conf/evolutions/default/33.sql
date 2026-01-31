# --- Allow global admins to also be program admins

# --- !Ups
alter table accounts drop constraint ck_account_admin;

# --- !Downs
alter table accounts alter column global_admin boolean default false constraint ck_account_admin CHECK(admin_of is null or array_length(admin_of, 1) = 0 or global_admin = false);