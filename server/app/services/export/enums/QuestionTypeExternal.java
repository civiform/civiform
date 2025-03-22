package services.export.enums;

import org.apache.commons.lang3.NotImplementedException;
import services.question.types.QuestionType;

/**
 * The type of the question, as shown to API consumers.
 *
 * <p>Static and Null questions are not exposed via the API.
 *
 * <p>This enum should be treated as append-only.
 */
public enum QuestionTypeExternal {
  ADDRESS,
  MULTI_SELECT,
  CURRENCY,
  DATE,
  SINGLE_SELECT,
  EMAIL,
  ENUMERATOR,
  FILE_UPLOAD,
  ID,
  NAME,
  NUMBER,
  TEXT,
  PHONE;

  public static QuestionTypeExternal fromQuestionType(QuestionType questionTypeInternal) {
    return switch (questionTypeInternal) {
      case ADDRESS -> QuestionTypeExternal.ADDRESS;
      case CHECKBOX -> QuestionTypeExternal.MULTI_SELECT;
      case CURRENCY -> QuestionTypeExternal.CURRENCY;
      case DATE -> QuestionTypeExternal.DATE;
      case DROPDOWN, RADIO_BUTTON -> QuestionTypeExternal.SINGLE_SELECT;
      case EMAIL -> QuestionTypeExternal.EMAIL;
      case ENUMERATOR -> QuestionTypeExternal.ENUMERATOR;
      case FILEUPLOAD -> QuestionTypeExternal.FILE_UPLOAD;
      case ID -> QuestionTypeExternal.ID;
      case NAME -> QuestionTypeExternal.NAME;
      case NUMBER -> QuestionTypeExternal.NUMBER;
      case TEXT -> QuestionTypeExternal.TEXT;
      case PHONE -> QuestionTypeExternal.PHONE;
      default ->
          throw new NotImplementedException(
              "QuestionType." + questionTypeInternal.name() + " is not supported in the API.");
    };
  }
}
