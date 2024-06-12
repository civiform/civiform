-- These assume a default locale of en_US, which is true for all deployments as of 2023-09-05.

# --- !Ups
UPDATE ONLY questions
SET question_options = (
  SELECT jsonb_agg(
           jsonb_set(
             question_option,
             '{adminName}',
             CASE
               WHEN question_option->'localizedOptionText'->'translations' ? 'en_US'
                 AND jsonb_typeof(question_option->'localizedOptionText'->'translations'->'en_US') = 'string'
                 AND question_option->'localizedOptionText'->'translations'->>'en_US' <> ''
               THEN
                 to_jsonb(question_option->'localizedOptionText'->'translations'->'en_US')
               ELSE
                 to_jsonb(question_option->>'id')
               END,
             true
             )
           )
  FROM jsonb_array_elements(question_options) AS question_option
)
WHERE question_options IS NOT NULL AND jsonb_array_length(question_options) <> 0;

# --- !Downs
UPDATE ONLY questions
SET question_options = (
  SELECT jsonb_agg((question_option - 'adminName'))
  FROM jsonb_array_elements(question_options) AS question_option
)
WHERE question_options IS NOT NULL;
