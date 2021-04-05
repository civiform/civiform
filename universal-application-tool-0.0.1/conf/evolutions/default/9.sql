# --- Add versioning and lifecycle management.

# --- !Ups

alter table questions add constraint nameversion unique(name, version);
alter table questions add column lifecycle_stage varchar;
alter table programs add column lifecycle_stage varchar;
alter table applications add column lifecycle_stage varchar;
alter table applications add column submit_time timestamp;
alter table programs add column version bigint;

update table questions set lifecycle_stage = 'active' where lifecycle_stage = null;
update table programs set lifecycle_stage = 'active' where lifecycle_stage = null;
update table applications set lifecycle_stage = 'active' where lifecycle_stage = null;

# --- !Downs
alter table questions drop constraint if exists nameversion cascade;
alter table questions drop column if exists lifecycle_stage;
alter table programs drop column if exists lifecycle_stage;
alter table applications drop column if exists lifecycle_stage;
alter table applications drop column if exists submit_time;
alter table programs drop column if exists version;