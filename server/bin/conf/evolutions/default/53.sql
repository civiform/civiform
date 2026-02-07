# --- !Ups

alter table applications add column is_admin boolean;

update applications 
set is_admin = case when (accounts.global_admin  or cardinality(accounts.admin_of) > 0) then true else false end
from accounts where applications.applicant_id = accounts.id;

# --- !Downs

alter table applications drop column if exists is_admin;
