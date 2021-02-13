# --- JSON-based tables

# --- !Ups
create table if not exists applicants (
  id bigserial primary key,
  object jsonb not null
  -- No other constraint on applicant - empty object acceptable.
);

create table if not exists questions (
  id bigserial primary key,
  object jsonb not null
);

create table if not exists programs (
  id bigserial primary key,
  name varchar,
  blocks jsonb not null
);

# --- !Downs

drop table if exists applicants cascade;
drop table if exists questions cascade;
drop table if exists programs cascade;
