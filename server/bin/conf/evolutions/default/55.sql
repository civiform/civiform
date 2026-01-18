# --- !Ups

with admin_check as 
(
	select 
		applications.id as application_id, 
		case when (accounts.global_admin or cardinality(accounts.admin_of) > 0) then true else false end as is_admin
	from applications 
	inner join applicants
		on applications.applicant_id = applicants.id
	inner join accounts
		on applicants.account_id = accounts.id
)

update applications 
set is_admin = ac.is_admin
from admin_check as ac
where ac.application_id = applications.id;


# --- !Downs

