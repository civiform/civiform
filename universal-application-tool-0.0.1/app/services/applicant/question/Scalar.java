package services.applicant.question;

import com.google.common.collect.ImmutableMap;
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
  CITY("city"),
  DATE("date"),
  EMAIL("email"),
  FILE_KEY("file key"),
  FIRST_NAME("first name"),
  LAST_NAME("last name"),
  LINE2("address line 2"),
  MIDDLE_NAME("middle name"),
  NUMBER("number"),
  SELECTION("selection"),
  STATE("state"),
  STREET("street"),
  TEXT("text"),
  ZIP("ZIP code"),

  // Special scalars for Enumerator updates
  DELETE_ENTITY("delete entity"), // This is used for deleting enumerator entries
  ENTITY_NAME("entity name"), // This is used for adding/updating enumerator entries

  // Metadata scalars
  UPDATED_AT("updated at"),
  PROGRAM_UPDATED_IN("program updated in");

  private final String displayString;

  /** The displayString should only be used in the Admin UI, since it is not localized. */
  Scalar(String displayString) {
    this.displayString = displayString;
  }

  public String toDisplayString() {
    return this.displayString;
  }

  private static final ImmutableMap<Scalar, ScalarType> ADDRESS_SCALARS =
      ImmutableMap.of(
          STREET, ScalarType.STRING,
          LINE2, ScalarType.STRING,
          CITY, ScalarType.STRING,
          STATE, ScalarType.STRING,
          ZIP, ScalarType.STRING);

  private static final ImmutableMap<Scalar, ScalarType> DATE_SCALARS =
      ImmutableMap.of(DATE, ScalarType.DATE);

  private static final ImmutableMap<Scalar, ScalarType> EMAIL_SCALARS =
      ImmutableMap.of(EMAIL, ScalarType.STRING);

  private static final ImmutableMap<Scalar, ScalarType> FILE_UPLOAD_SCALARS =
      ImmutableMap.of(FILE_KEY, ScalarType.STRING);

  private static final ImmutableMap<Scalar, ScalarType> MULTI_SELECT_SCALARS =
      ImmutableMap.of(SELECTION, ScalarType.LIST_OF_STRINGS);

  private static final ImmutableMap<Scalar, ScalarType> NAME_SCALARS =
      ImmutableMap.of(
          FIRST_NAME, ScalarType.STRING,
          MIDDLE_NAME, ScalarType.STRING,
          LAST_NAME, ScalarType.STRING);

  private static final ImmutableMap<Scalar, ScalarType> NUMBER_SCALARS =
      ImmutableMap.of(NUMBER, ScalarType.LONG);

  private static final ImmutableMap<Scalar, ScalarType> SINGLE_SELECT_SCALARS =
      ImmutableMap.of(SELECTION, ScalarType.STRING);

  private static final ImmutableMap<Scalar, ScalarType> TEXT_SCALARS =
      ImmutableMap.of(TEXT, ScalarType.STRING);

  private static final ImmutableMap<Scalar, ScalarType> METADATA_SCALARS =
      ImmutableMap.of(
          UPDATED_AT, ScalarType.LONG,
          PROGRAM_UPDATED_IN, ScalarType.LONG);

  private static ImmutableSet<String> metadataScalarKeys;

  /**
   * Returns the scalars for a specific {@link QuestionType}.
   *
   * <p>The {@link QuestionType#ENUMERATOR} does not have scalars. Use {@link Scalar#ENTITY_NAME}
   * instead.
   */
  public static ImmutableMap<Scalar, ScalarType> getScalars(QuestionType questionType)
      throws InvalidQuestionTypeException, UnsupportedQuestionTypeException {
    switch (questionType) {
      case ADDRESS:
        return ADDRESS_SCALARS;
      case DATE:
        return DATE_SCALARS;
      case EMAIL:
        return EMAIL_SCALARS;
      case FILEUPLOAD:
        return FILE_UPLOAD_SCALARS;
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

      case ENUMERATOR: // Enumerator Question does not have scalars like the other question types
        // do.
        throw new InvalidQuestionTypeException("Enumeration questions are handled separately.");

      default:
        throw new UnsupportedQuestionTypeException(questionType);
    }
  }

  public static ImmutableMap<Scalar, ScalarType> getMetadataScalars() {
    return METADATA_SCALARS;
  }

  /** A set of Scalars as strings that represent keys where metadata is stored. */
  public static ImmutableSet<String> getMetadataScalarKeys() {
    if (metadataScalarKeys == null) {
      metadataScalarKeys =
          METADATA_SCALARS.keySet().stream()
              .map(Scalar::name)
              .map(String::toLowerCase)
              .collect(ImmutableSet.toImmutableSet());
    }
    return metadataScalarKeys;
  }
}
