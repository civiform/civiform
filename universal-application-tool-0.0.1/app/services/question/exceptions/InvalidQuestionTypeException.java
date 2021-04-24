package services.question.exceptions;

import services.question.types.QuestionType;

/**
 * This exception is thrown when an invalid question type is encountered.
 *
 * <p>NOTE: {@link UnsupportedQuestionTypeException} should be thrown if a question type is valid
 * but not fully supported yet.
 */
public class InvalidQuestionTypeException extends Exception {
  public InvalidQuestionTypeException(QuestionType questionType) {
    super(String.format("%s is not a valid question type.", questionType));
  }

  public InvalidQuestionTypeException(String message) {
    super(message);
  }
}
