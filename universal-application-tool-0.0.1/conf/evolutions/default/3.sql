# --- JSON-based tables

# --- !Ups
create table if not exists applicants (
  id bigserial primary key,
  object jsonb not null
  -- No other constraint on applicant - empty object acceptable.
);

create table if not exists questions (
  id bigserial primary key,
  object jsonb not null,
);

create table if not exists programs (
  name varchar,
  version bigint,
  object jsonb not null,
  primary key (name, version)
);

# --- !Downs

drop table if exists applicants cascade;
drop table if exists questions cascade;
drop table if exists programs cascade;
