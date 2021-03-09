package services.question;

/**
 * This exception should be thrown in the `default` case of all `switch(QuestionType)` to ensure
 * that when new question types are added but not fully supported across the UAT, they fail fast.
 *
 * <p>NOTE: {@link InvalidQuestionTypeException} should be thrown if a question type is not valid.
 */
public class UnsupportedQuestionTypeException extends Exception {
  public UnsupportedQuestionTypeException(QuestionType questionType) {
    super(String.format("QuestionType %s is not supported.", questionType));
  }
}
