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
    switch (questionTypeInternal) {
      case ADDRESS:
        return QuestionTypeExternal.ADDRESS;
      case CHECKBOX:
        return QuestionTypeExternal.MULTI_SELECT;
      case CURRENCY:
        return QuestionTypeExternal.CURRENCY;
      case DATE:
        return QuestionTypeExternal.DATE;
      case DROPDOWN:
      case RADIO_BUTTON:
        return QuestionTypeExternal.SINGLE_SELECT;
      case EMAIL:
        return QuestionTypeExternal.EMAIL;
      case ENUMERATOR:
        return QuestionTypeExternal.ENUMERATOR;
      case FILEUPLOAD:
        return QuestionTypeExternal.FILE_UPLOAD;
      case ID:
        return QuestionTypeExternal.ID;
      case NAME:
        return QuestionTypeExternal.NAME;
      case NUMBER:
        return QuestionTypeExternal.NUMBER;
      case TEXT:
        return QuestionTypeExternal.TEXT;
      case PHONE:
        return QuestionTypeExternal.PHONE;
      default:
        throw new NotImplementedException(
            "QuestionType." + questionTypeInternal.name() + " is not supported in the API.");
    }
  }
}
