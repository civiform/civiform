# --- Create a table that links programs to the program admin accounts that administer them.

# --- !Ups
alter table accounts add column admin_of varchar[];

create index idx_admin_of on accounts using gin(admin_of);

# --- !Downs
drop index if exists idx_admin_of;

alter table accounts drop column admin_of;
