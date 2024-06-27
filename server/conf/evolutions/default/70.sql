-- Lots of casting going on here. validation_predicates are stored as jsonb strings which means
-- we need to turn them into json to insert the new field, then case the whole object back into
-- a jsonb string before re-saving it. 


# --- !Ups
UPDATE questions
SET validation_predicates = CASE
  WHEN question_type IN ('CHECKBOX', 'DROPDOWN', 'RADIO_BUTTON') THEN
    to_jsonb(jsonb_set(
      (validation_predicates#>> '{}')::jsonb,
      '{type}',
      to_jsonb('multioption'::text),
      true
    )::text)
  ELSE
    to_jsonb(jsonb_set(
        (validation_predicates#>> '{}')::jsonb,
        '{type}',
        to_jsonb(LOWER(question_type)),
        true
    )::text)
END;

# --- !Downs
UPDATE questions
SET validation_predicates = 
    to_jsonb(
      ((validation_predicates#>> '{}')::jsonb - 'type')::text);