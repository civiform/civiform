package services.applicant.question;

import com.google.common.collect.ImmutableMap;
import services.question.exceptions.InvalidQuestionTypeException;
import services.question.exceptions.UnsupportedQuestionTypeException;
import services.question.types.QuestionType;
import services.question.types.ScalarType;

/**
 * This class represents all scalars used in {@link services.applicant.ApplicantData}. The scalars
 * are a mapping from the string associated with the scalar to the {@link ScalarType}.
 *
 * <p>Each {@link QuestionType} has question-specific scalars accessible through {@link
 * Scalars#getScalars(QuestionType)}, and metadata scalars used by all questions are available
 * through {@link Scalars#getMetadataScalars}.
 */
public class Scalars {
  public static final String ENUMERATION_SCALAR = "entity_name";

  // Address scalars
  public static final String STREET = "street";
  public static final String CITY = "city";
  public static final String STATE = "state";
  public static final String ZIP = "zip";
  private static final ImmutableMap<String, ScalarType> ADDRESS_SCALARS =
      ImmutableMap.of(
          STREET, ScalarType.STRING,
          CITY, ScalarType.STRING,
          STATE, ScalarType.STRING,
          ZIP, ScalarType.STRING);

  // File upload scalars
  public static final String FILE = "filekey";
  private static final ImmutableMap<String, ScalarType> FILE_UPLOAD_SCALARS =
      ImmutableMap.of(FILE, ScalarType.STRING);

  // Selection scalars
  public static final String SELECTION = "selection";
  private static final ImmutableMap<String, ScalarType> MULTI_SELECT_SCALARS =
      ImmutableMap.of(SELECTION, ScalarType.STRING);

  // Name scalars
  public static final String FIRST = "first";
  public static final String MIDDLE = "middle";
  public static final String LAST = "last";
  private static final ImmutableMap<String, ScalarType> NAME_SCALARS =
      ImmutableMap.of(
          FIRST, ScalarType.STRING,
          MIDDLE, ScalarType.STRING,
          LAST, ScalarType.STRING);

  // Number scalars
  public static final String NUMBER = "number";
  private static final ImmutableMap<String, ScalarType> NUMBER_SCALARS =
      ImmutableMap.of(NUMBER, ScalarType.LONG);

  private static final ImmutableMap<String, ScalarType> SINGLE_SELECT_SCALARS =
      ImmutableMap.of(SELECTION, ScalarType.STRING);

  // Text scalars
  public static final String TEXT = "text";
  private static final ImmutableMap<String, ScalarType> TEXT_SCALARS =
      ImmutableMap.of(TEXT, ScalarType.STRING);

  // Metadata scalars
  public static final String METADATA_UPDATED_AT_KEY = "updated_at";
  public static final String METADATA_UPDATED_PROGRAM_ID_KEY = "updated_in_program";
  private static final ImmutableMap<String, ScalarType> METADATA_SCALARS =
      ImmutableMap.of(
          METADATA_UPDATED_AT_KEY, ScalarType.LONG,
          METADATA_UPDATED_PROGRAM_ID_KEY, ScalarType.LONG);

  /**
   * Returns the scalars for a specific {@link QuestionType}.
   *
   * <p>The {@link QuestionType#REPEATER} does not have scalars. Use {@link
   * Scalars#ENUMERATION_SCALAR} instead.
   */
  public static ImmutableMap<String, ScalarType> getScalars(QuestionType questionType)
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

        // QuestionTypes with multi-selection
      case CHECKBOX:
        return MULTI_SELECT_SCALARS;

        // QuestionTypes with single-selection
      case DROPDOWN:
      case RADIO_BUTTON:
        return SINGLE_SELECT_SCALARS;

        // Repeater Question does not have scalars like the other question types do.
      case REPEATER:
        throw new InvalidQuestionTypeException("Enumeration questions are handled separately.");

      default:
        throw new UnsupportedQuestionTypeException(questionType);
    }
  }

  public static ImmutableMap<String, ScalarType> getMetadataScalars() {
    return METADATA_SCALARS;
  }
}
