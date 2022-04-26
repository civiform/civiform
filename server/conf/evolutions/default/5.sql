# --- Accounts

# --- !Ups

create table accounts (
  id bigserial primary key
  -- table does not contain a username or password hash yet.
);

alter table applicants add account_id bigint;
alter table applicants add foreign key (account_id) references accounts;

# --- !Downs

alter table applicants drop column account_id;
drop table accounts;