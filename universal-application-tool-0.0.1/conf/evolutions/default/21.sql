# --- Create a table that links programs to the program admin accounts that administer them.

# --- !Ups
create table programs_accounts (
  programs_id bigint not null,
  accounts_id bigint not null,
  constraint fk_program foreign key (programs_id) references programs(id),
  constraint fk_account foreign key (accounts_id) references accounts(id),
  primary key (programs_id, accounts_id)
);

# --- !Downs
drop table if exists programs_accounts;
