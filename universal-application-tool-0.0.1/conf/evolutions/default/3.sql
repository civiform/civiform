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
  -- ensure that all entries include a 'target' key.
  check (object ? 'target')
);

create unique index if not exists question_target on questions(
  -- Create an index on 'target' - ensure that no two entries have
  -- the same 'target' value.  This will allow us to look up what
  -- question can help us gather a piece of necessary information.
  (object ->>'target')
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
