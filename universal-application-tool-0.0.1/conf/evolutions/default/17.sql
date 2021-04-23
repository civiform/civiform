# --- Create the versions table and migrate existing actives and drafts.

# --- !Ups
create table versions (
  id bigserial primary key,
  lifecycle_stage varchar not null,
  submit_time timestamp
);

create table versions_questions (
  questions_id bigint not null,
  versions_id bigint not null,
  primary key (questions_id, versions_id)
);

create table versions_programs (
  programs_id bigint not null,
  versions_id bigint not null,
  primary key (programs_id, versions_id)
);

insert into versions (lifecycle_stage, submit_time) values ('active', current_timestamp);

insert into versions_questions (questions_id, versions_id)
  select id, 1 from questions
  where lifecycle_stage = 'active';

insert into versions_programs (programs_id, versions_id)
  select id, 1 from programs
  where lifecycle_stage = 'active';

alter table questions drop constraint if exists nameversion;
alter table programs drop constraint if exists nameversion;
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