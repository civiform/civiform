# --- JSON-based tables

# --- !Ups
create table if not exists applicants (
  id bigserial primary key,
  preferred_locale varchar,
  object jsonb not null
  -- No other constraint on applicant - empty object acceptable.
);

create table if not exists questions (
  id bigserial primary key,
  version bigserial,
  path varchar,
  name varchar,
  description varchar,
  question_type varchar,
  question_text jsonb,
  question_help_text jsonb
);

create table if not exists programs (
  id bigserial primary key,
  name varchar,
  description varchar,
  block_definitions jsonb not null
);

# --- !Downs

drop table if exists applicants cascade;
drop table if exists questions cascade;
drop table if exists programs cascade;
