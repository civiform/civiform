# --- Create a table that links programs to the program admin accounts that administer them.

# --- !Ups
create table programs_accounts (
  programs_id bigint not null,
  accounts_id bigint not null,
  primary key (programs_id, accounts_id)
);

# --- !Downs
drop table if exists programs_accounts;