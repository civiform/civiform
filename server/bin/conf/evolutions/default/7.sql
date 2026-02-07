# --- Add question validation predicates
# --- !Ups

alter table questions add validation_predicates jsonb;

# --- !Downs

alter table questions drop column validation_predicates;
