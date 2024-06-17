 --- Create the categories and programs_categories tables.

# --- !Ups

CREATE TABLE IF NOT EXISTS categories (
  id BIGSERIAL PRIMARY KEY NOT NULL,
  localized_name JSONB NOT NULL,
  create_time TIMESTAMP NOT NULL,
  last_modified_time TIMESTAMP,
  lifecycle_stage VARCHAR NOT NULL
);

CREATE TABLE IF NOT EXISTS programs_categories (
  programs_id BIGINT NOT NULL,
  categories_id BIGINT NOT NULL,
  PRIMARY KEY (programs_id, categories_id)
);

# --- !Downs

DROP TABLE IF EXISTS programs_categories;
DROP TABLE IF EXISTS categories;
>>>>>>> main
