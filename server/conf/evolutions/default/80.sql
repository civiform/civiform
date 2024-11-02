# --- !Ups
UPDATE questions SET validation_predicates = (validation_predicates#>>'{}')::jsonb;
UPDATE applicants SET object = (object#>>'{}')::jsonb;
UPDATE applications SET object = (object#>>'{}')::jsonb;

# --- !Downs
UPDATE questions SET validation_predicates = to_jsonb(validation_predicates::text);
UPDATE applicants SET object = to_jsonb(object::text);
UPDATE applications SET object = to_jsonb(object::text);