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
  CURRENCY_CENTS("currency", ScalarType.CURRENCY_CENTS),
  DATE("date", ScalarType.DATE),
  EMAIL("email", ScalarType.STRING),
  FILE_KEY("file key", ScalarType.STRING),
  FIRST_NAME("first name", ScalarType.STRING),
  ID("id", ScalarType.STRING),
  LAST_NAME("last name", ScalarType.STRING),
  LINE2("address line 2", ScalarType.STRING),
  MIDDLE_NAME("middle name", ScalarType.STRING),
  NUMBER("number", ScalarType.LONG),
  ORIGINAL_FILE_NAME("original file name", ScalarType.STRING),
  SELECTION("selection", ScalarType.STRING),
  SELECTIONS("selections", ScalarType.LIST_OF_STRINGS),
  STATE("state", ScalarType.STRING),
  STREET("street", ScalarType.STRING),
  TEXT("text", ScalarType.STRING),
  ZIP("ZIP code", ScalarType.STRING),

  // Special scalars for Enumerator updates
  DELETE_ENTITY("delete entity", ScalarType.STRING), // This is used for deleting enumerator entries
  ENTITY_NAME(
      "entity name", ScalarType.STRING), // This is used for adding/updating enumerator entries

  // Special scalars for Address questions
  SERVICE_AREA("service area", ScalarType.SERVICE_AREA),

  // Metadata scalars
  UPDATED_AT("updated at", ScalarType.LONG),
  PROGRAM_UPDATED_IN("program updated in", ScalarType.LONG);

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

  private static final ImmutableSet<Scalar> ADDRESS_SCALARS =
      ImmutableSet.of(STREET, LINE2, CITY, STATE, ZIP);

  private static final ImmutableSet<Scalar> CURRENCY_SCALARS = ImmutableSet.of(CURRENCY_CENTS);

  private static final ImmutableSet<Scalar> DATE_SCALARS = ImmutableSet.of(DATE);

  private static final ImmutableSet<Scalar> EMAIL_SCALARS = ImmutableSet.of(EMAIL);

  private static final ImmutableSet<Scalar> FILE_UPLOAD_SCALARS =
      ImmutableSet.of(FILE_KEY, ORIGINAL_FILE_NAME);

  private static final ImmutableSet<Scalar> ID_SCALARS = ImmutableSet.of(ID);

  private static final ImmutableSet<Scalar> MULTI_SELECT_SCALARS = ImmutableSet.of(SELECTIONS);

  private static final ImmutableSet<Scalar> NAME_SCALARS =
      ImmutableSet.of(FIRST_NAME, MIDDLE_NAME, LAST_NAME);

  private static final ImmutableSet<Scalar> NUMBER_SCALARS = ImmutableSet.of(NUMBER);

  private static final ImmutableSet<Scalar> SINGLE_SELECT_SCALARS = ImmutableSet.of(SELECTION);

  private static final ImmutableSet<Scalar> TEXT_SCALARS = ImmutableSet.of(TEXT);

  private static final ImmutableSet<Scalar> STATIC_SCALARS = ImmutableSet.of();

  private static final ImmutableSet<Scalar> METADATA_SCALARS =
      ImmutableSet.of(UPDATED_AT, PROGRAM_UPDATED_IN);

  private static ImmutableSet<String> metadataScalarKeys;

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
        return ADDRESS_SCALARS;
      case CURRENCY:
        return CURRENCY_SCALARS;
      case DATE:
        return DATE_SCALARS;
      case EMAIL:
        return EMAIL_SCALARS;
      case FILEUPLOAD:
        return FILE_UPLOAD_SCALARS;
      case ID:
        return ID_SCALARS;
      case NAME:
        return NAME_SCALARS;
      case NUMBER:
        return NUMBER_SCALARS;
      case TEXT:
        return TEXT_SCALARS;

      case CHECKBOX: // QuestionTypes with multi-selection
        return MULTI_SELECT_SCALARS;

      case DROPDOWN: // QuestionTypes with single-selection
      case RADIO_BUTTON:
        return SINGLE_SELECT_SCALARS;

      case STATIC:
        return STATIC_SCALARS;

      case ENUMERATOR: // Enumerator Question does not have scalars like the other question types
        // do.
        throw new InvalidQuestionTypeException("Enumeration questions are handled separately.");

      default:
        throw new UnsupportedQuestionTypeException(questionType);
    }
  }

  public static ImmutableSet<Scalar> getMetadataScalars() {
    return METADATA_SCALARS;
  }

  /** A set of Scalars as strings that represent keys where metadata is stored. */
  public static ImmutableSet<String> getMetadataScalarKeys() {
    if (metadataScalarKeys == null) {
      metadataScalarKeys =
          METADATA_SCALARS.stream()
              .map(Scalar::name)
              .map(String::toLowerCase)
              .collect(ImmutableSet.toImmutableSet());
    }
    return metadataScalarKeys;
  }
}
