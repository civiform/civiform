# --- Create the versions table and migrate existing actives and drafts.

# --- !Ups
create table versions (
  id bigserial primary key,
  lifecycle_stage varchar not null
);

create table versions_questions (
  questions_id bigint,
  versions_id bigint,
  primary key (questions_id, versions_id)
);

create table versions_programs (
  programs_id bigint,
  versions_id bigint,
  primary key (programs_id, versions_id)
);

insert into versions (lifecycle_stage) values ('active');

insert into versions_questions (questions_id, versions_id)
  select id, 1 from questions
  where lifecycle_stage = 'active';

insert into versions_programs (programs_id, versions_id)
  select id, 1 from programs
  where lifecycle_stage = 'active';

alter table questions drop constraint nameversion;
alter table questions drop column if exists lifecycle_stage;
alter table programs drop column if exists lifecycle_stage;
alter table questions drop column if exists version;
alter table programs drop column if exists version;


# --- !Downs

drop table if exists versions_programs;
drop table if exists versions_questions;
drop table if exists versions;

alter table questions add column lifecycle_stage varchar;
alter table questions add column version bigint;
alter table questions add constraint nameversion unique(name, version);
alter table programs add column lifecycle_stage varchar;
alter table programs add column version bigint;
alter table programs add constraint nameversion unique(name, version);