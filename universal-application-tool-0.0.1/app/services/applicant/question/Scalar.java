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
  CITY,
  DAY,
  FILE_KEY,
  FIRST_NAME,
  LAST_NAME,
  LINE2,
  MIDDLE_NAME,
  MONTH,
  NUMBER,
  SELECTION,
  STATE,
  STREET,
  TEXT,
  YEAR,
  ZIP,

  // Special scalars for Enumerator updates
  DELETE_ENTITY, // This is used for deleting enumerator entries
  ENTITY_NAME, // This is used for adding/updating enumerator entries

  // Metadata scalars
  UPDATED_AT,
  PROGRAM_UPDATED_IN;

  private static final ImmutableMap<Scalar, ScalarType> ADDRESS_SCALARS =
      ImmutableMap.of(
          STREET, ScalarType.STRING,
          LINE2, ScalarType.STRING,
          CITY, ScalarType.STRING,
          STATE, ScalarType.STRING,
          ZIP, ScalarType.STRING);

  private static final ImmutableMap<Scalar, ScalarType> FILE_UPLOAD_SCALARS =
      ImmutableMap.of(FILE_KEY, ScalarType.STRING);

  private static final ImmutableMap<Scalar, ScalarType> MULTI_SELECT_SCALARS =
      ImmutableMap.of(SELECTION, ScalarType.STRING);

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
