# --- Create a table that links programs to the program admin accounts that administer them.

# --- !Ups
create table program_accounts (
  program_id bigint not null,
  account_id bigint not null,
  primary key (program_id, account_id)
);

# --- !Downs
drop table if exists program_accounts;