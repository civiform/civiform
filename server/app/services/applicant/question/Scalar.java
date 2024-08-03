package services.applicant.question;

import com.google.common.collect.ImmutableSet;
import services.question.exceptions.InvalidQuestionTypeException;
import services.question.exceptions.UnsupportedQuestionTypeException;
import services.question.types.QuestionType;
import services.question.types.ScalarType;

/**
 * This enum represents all scalars used in {@link services.applicant.ApplicantData}. The scalars
 * maps are a mapping from the scalar to its {@link ScalarType}.
 *
 * <p>Each {@link QuestionType} has question-specific scalars accessible through {@link
 * Scalar#getScalars(QuestionType)}, and metadata scalars used by all questions are available
 * through {@link Scalar#getMetadataScalars}.
 *
 * <p>EXISTING SCALARS SHOULD NOT BE MODIFIED. The Scalar enum should be treated as append-only.
 */
public enum Scalar {
  CITY("city", ScalarType.STRING),
  CORRECTED("corrected", ScalarType.STRING),
  CORRECTION_SOURCE("correction_source", ScalarType.STRING),
  CURRENCY_CENTS("currency", ScalarType.CURRENCY_CENTS),
  DATE("date", ScalarType.DATE),
  EMAIL("email", ScalarType.STRING),
  FILE_KEY_LIST("file keys", ScalarType.LIST_OF_STRINGS),
  FILE_KEY("file key", ScalarType.STRING),
  FIRST_NAME("first name", ScalarType.STRING),
  ID("id", ScalarType.STRING),
  LAST_NAME("last name", ScalarType.STRING),
  LATITUDE("latitude", ScalarType.DOUBLE),
  LINE2("address line 2", ScalarType.STRING),
  LONGITUDE("longitude", ScalarType.DOUBLE),
  MIDDLE_NAME("middle name", ScalarType.STRING),
  NUMBER("number", ScalarType.LONG),
  ORIGINAL_FILE_NAME("original file name", ScalarType.STRING),
  SELECTION("selection", ScalarType.STRING),
  SELECTIONS("selections", ScalarType.LIST_OF_STRINGS),
  STATE("state", ScalarType.STRING),
  STREET("street", ScalarType.STRING),
  TEXT("text", ScalarType.STRING),
  WELL_KNOWN_ID("well_known_id", ScalarType.LONG),
  ZIP("ZIP code", ScalarType.STRING),

  // Special scalars for Enumerator updates
  DELETE_ENTITY("delete entity", ScalarType.STRING), // This is used for deleting enumerator entries
  ENTITY_NAME(
      "entity name", ScalarType.STRING), // This is used for adding/updating enumerator entries

  // Special scalars for Address questions
  SERVICE_AREA("service_area", ScalarType.SERVICE_AREA),

  // Scalars for Phone Question
  PHONE_NUMBER("phone_number", ScalarType.PHONE_NUMBER),
  COUNTRY_CODE("country_code", ScalarType.STRING),

  // Scalars for Date Question using memorable date (3 different inputs). They are not used
  // for storage or predicates, so they are not included in getScalars.
  DAY("day", ScalarType.LONG),
  MONTH("month", ScalarType.LONG),
  YEAR("year", ScalarType.LONG),

  // Metadata scalars
  UPDATED_AT("updated at", ScalarType.LONG),
  PROGRAM_UPDATED_IN("program updated in", ScalarType.LONG);

  private static final ImmutableSet<Scalar> METADATA_SCALARS =
      ImmutableSet.of(UPDATED_AT, PROGRAM_UPDATED_IN);
  private static final ImmutableSet<String> METADATA_SCALAR_KEYS =
      METADATA_SCALARS.stream()
          .map(Scalar::name)
          .map(String::toLowerCase)
          .collect(ImmutableSet.toImmutableSet());

  private final String displayString;
  private final ScalarType scalarType;

  /** The displayString should only be used in the Admin UI, since it is not localized. */
  Scalar(String displayString, ScalarType scalarType) {
    this.displayString = displayString;
    this.scalarType = scalarType;
  }

  public String toDisplayString() {
    return this.displayString;
  }

  public ScalarType toScalarType() {
    return this.scalarType;
  }

  /**
   * Returns the scalars for a specific {@link QuestionType}.
   *
   * <p>The {@link QuestionType#ENUMERATOR} does not have scalars. Use {@link Scalar#ENTITY_NAME}
   * instead.
   */
  public static ImmutableSet<Scalar> getScalars(QuestionType questionType)
      throws InvalidQuestionTypeException, UnsupportedQuestionTypeException {
    switch (questionType) {
      case ADDRESS:
        return ImmutableSet.of(
            STREET,
            LINE2,
            CITY,
            STATE,
            ZIP,
            CORRECTED,
            LATITUDE,
            LONGITUDE,
            WELL_KNOWN_ID,
            SERVICE_AREA);
      case CURRENCY:
        return ImmutableSet.of(CURRENCY_CENTS);
      case DATE:
        return ImmutableSet.of(DATE);
      case EMAIL:
        return ImmutableSet.of(EMAIL);
      case FILEUPLOAD:
        return ImmutableSet.of(FILE_KEY, FILE_KEY_LIST, ORIGINAL_FILE_NAME);
      case ID:
        return ImmutableSet.of(ID);
      case NAME:
        return ImmutableSet.of(FIRST_NAME, MIDDLE_NAME, LAST_NAME);
      case NUMBER:
        return ImmutableSet.of(NUMBER);
      case TEXT:
        return ImmutableSet.of(TEXT);
      case CHECKBOX: // QuestionTypes with multi-selection
        return ImmutableSet.of(SELECTIONS);
      case DROPDOWN: // QuestionTypes with single-selection
      case RADIO_BUTTON:
        return ImmutableSet.of(SELECTION);
      case STATIC:
        return ImmutableSet.of();
      case PHONE:
        return ImmutableSet.of(PHONE_NUMBER, COUNTRY_CODE);
      case ENUMERATOR: // Enumerator Question does not have scalars like the other question types
        // do.
        throw new InvalidQuestionTypeException("Enumeration questions are handled separately.");
      case NULL_QUESTION: // Fallthrough intended
      default:
        throw new UnsupportedQuestionTypeException(questionType);
    }
  }

  public static ImmutableSet<Scalar> getMetadataScalars() {
    return METADATA_SCALARS;
  }

  /** A set of Scalars as strings that represent keys where metadata is stored. */
  public static ImmutableSet<String> getMetadataScalarKeys() {
    return METADATA_SCALAR_KEYS;
  }
}
