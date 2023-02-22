# --- !Ups

alter table programs add column eligibility_is_gating boolean default 'true';

# --- !Downs

alter table programs drop column if exists eligibility_is_gating;
