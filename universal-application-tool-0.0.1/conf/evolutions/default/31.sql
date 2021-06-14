# --- Link stored files to applicants
# --- !Ups

alter table files add column if not exists applicant_id bigint;
alter table files add constraint fk_applicant foreign key (applicant_id) references applicants(id);

# --- !Downs

alter table files drop constraint if exists fk_applicant cascade;
alter table files drop column if exists applicant_id;