# --- !Ups

alter table programs add column is_eligibility_gating boolean default 'true';

# --- !Downs

alter table programs drop column if exists is_eligibility_gating;
