-- These assume a default locale of en_US, which is true for all deployments as of 2023-09-05.

# --- !Ups
UPDATE ONLY programs
SET block_definitions = (
  SELECT jsonb_agg(
           jsonb_set(
             block_definition,
             '{localizedName,translations,en_US}',
             CASE
               WHEN block_definition->'localizedName'->'translations' ? 'en_US'
                 AND jsonb_typeof(block_definition->'localizedName'->'translations'->'en_US') = 'string'
                 AND block_definition->'localizedName'->'translations'->>'en_US' <> ''
               THEN
                 block_definition->'localizedName'->'translations'->'en_US'
               ELSE
                 block_definition->'name'
               END,
             true
             )
           )
  FROM jsonb_array_elements(block_definitions) AS block_definition
)
WHERE block_definitions IS NOT NULL AND jsonb_array_length(block_definitions) <> 0;

# --- !Downs
UPDATE ONLY programs
SET block_definitions = (
  SELECT jsonb_agg((block_definition - 'localizedName'))
  FROM jsonb_array_elements(block_definitions) AS block_definition
)
WHERE block_definitions IS NOT NULL;
