# --- Accounts

# --- !Ups

create table accounts (
  id bigserial primary key
  -- table does not contain a username or password hash yet.
);

alter table applicants add account bigint;
alter table applicants add foreign key (account) references accounts;

# --- !Downs

alter table applicants drop column account;
drop table accounts;