# --- Application table

# --- !Ups
create table if not exists applications (
  id bigserial primary key,
  applicant_id bigint,
  program_id bigint,
  object jsonb not null,
  constraint fk_applicant foreign key(applicant_id) references applicants(id),
  constraint fk_program foreign key(program_id) references programs(id)
);

# --- !Downs

drop table if exists applications cascade;
