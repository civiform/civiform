# --- JSON-based tables

# --- !Ups
create table applicants (
  id bigserial primary key,
  applicant json not null
  -- No other constraint on applicant - empty object acceptable.
);

create table questions (
  id bigserial primary key,
  question json not null
);

create unique index question_target on questions(
  (question->>'target')
);

alter table questions add constraint has_target check ((question ->> 'target') is not null);

create table programs (
  name varchar,
  version bigint,
  program json not null,
  primary key (name, version)
);

# --- !Downs

drop table if exists applicants cascade;
drop table if exists questions cascade;
drop table if exists programs cascade;
