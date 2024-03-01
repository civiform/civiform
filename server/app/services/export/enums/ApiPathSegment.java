package services.export.enums;

/**
 * Path segments used in the API
 *
 * <p>This enum should be treated as append-only.
 */
public enum ApiPathSegment {
  // The name of the field that indicates the question type
  QUESTION_TYPE,
  // Used instead of `currency_cents` in the API
  CURRENCY_DOLLARS,
  // The name of the field holding the array of repeated entities for enumerator questions
  ENTITIES,
  // A metadata field within repeated entities, currently a placeholder to reserve the field name
  // for future use and prevent questions with this name
  ENTITY_METADATA;
}
