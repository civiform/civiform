package services.question.exceptions;

/**
 * This exception is thrown when an invalid question type is encountered.
 *
 * <p>NOTE: {@link UnsupportedQuestionTypeException} should be thrown if a question type is valid
 * but not fully supported yet.
 */
public class InvalidQuestionTypeException extends Exception {
  public InvalidQuestionTypeException(String questionType) {
    super(String.format("%s is not a valid question type.", questionType));
  }
}
