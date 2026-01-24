# --- Create the versions table and migrate existing actives and drafts.

# --- !Ups
create table if not exists ti_organizations (
  id bigserial primary key,
  name varchar not null,
  description varchar
);

alter table accounts add column member_of_group_id bigint constraint fk_member references ti_organizations(id) on delete set null;
alter table accounts add column managed_by_group_id bigint constraint fk_managed references ti_organizations(id) on delete set null;

# --- !Downs
drop table if exists ti_organizations;
alter table accounts drop column member_of_group_id;
alter table accounts drop column managed_by_group_id;
