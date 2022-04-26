# --- Add a column to accounts with the names of programs that account administers.
# --- Also add an index using PostgreSQL's GIN index

# --- !Ups
alter table accounts add column admin_of varchar[];

create index idx_admin_of on accounts using gin(admin_of);

# --- !Downs
drop index if exists idx_admin_of;

alter table accounts drop column admin_of;
