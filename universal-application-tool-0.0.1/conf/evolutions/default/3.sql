# --- JSON-based tables

# --- !Ups
create table if not exists applicants (
  id bigserial primary key,
  applicant jsonb not null
  -- No other constraint on applicant - empty object acceptable.
);

create table if not exists questions (
  id bigserial primary key,
  question jsonb not null,
  check (question ? 'target')
);

create unique index if not exists question_target on questions(
  (question->>'target')
);

create table if not exists programs (
  name varchar,
  version bigint,
  program jsonb not null,
  primary key (name, version)
);

# --- !Downs

drop table if exists applicants cascade;
drop table if exists questions cascade;
drop table if exists programs cascade;
