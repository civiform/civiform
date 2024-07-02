-- A note about the type casting going on here: validation_predicates are stored as jsonb strings which means we need to turn them into json to insert the new field. 
-- To save it back in it's original format, we have to cast it back to text and then wrap it in to_jsonb again. 


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