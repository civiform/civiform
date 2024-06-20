-- These assume a default locale of en_US, which is true for all deployments as of 2023-09-05.

# --- !Ups
UPDATE programs p
SET block_definitions = (
  SELECT jsonb_agg(
    CASE
        WHEN block_definition ? 'name' AND NOT (block_definition ? 'localizedName')
            THEN jsonb_set(
                block_definition,
                '{localizedName}',
                jsonb_build_object('translations', jsonb_build_object('en-US', block_definition->'name'))
            )
        ELSE block_definition -- Keep the block_definition as is
    END
  )
  FROM jsonb_array_elements(p.block_definitions) AS block_definition
)
WHERE block_definitions IS NOT NULL AND jsonb_array_length(block_definitions) <> 0;

UPDATE programs p
SET block_definitions = (
  SELECT jsonb_agg(
    CASE
        WHEN block_definition ? 'description' AND NOT (block_definition ? 'localizedDescription')
            THEN jsonb_set(
                block_definition,
                '{localizedDescription}',
                jsonb_build_object('translations', jsonb_build_object('en-US', block_definition->'description'))
            )
        ELSE block_definition -- Keep the block_definition as is
    END
  )
  FROM jsonb_array_elements(p.block_definitions) AS block_definition
)
WHERE block_definitions IS NOT NULL AND jsonb_array_length(block_definitions) <> 0;


# --- !Downs
UPDATE programs
SET block_definitions = (
  SELECT jsonb_agg((block_definition - 'localizedName'))
  FROM jsonb_array_elements(block_definitions) AS block_definition
)
WHERE block_definitions IS NOT NULL;

UPDATE programs
SET block_definitions = (
  SELECT jsonb_agg((block_definition - 'localizedDescription'))
  FROM jsonb_array_elements(block_definitions) AS block_definition
)
WHERE block_definitions IS NOT NULL;
