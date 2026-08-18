-- Update Questions table to support images in question
# --- !Ups
ALTER TABLE questions
ADD COLUMN IF NOT EXISTS localized_image_description jsonb;

ALTER TABLE questions
ADD COLUMN IF NOT EXISTS image_file_keys jsonb;

# --- !Downs
ALTER TABLE questions
DROP COLUMN IF EXISTS localized_image_description;

ALTER TABLE questions
DROP COLUMN IF EXISTS image_file_keys;
