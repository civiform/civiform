package services.question;

/**
 * This exception should be thrown in the `default` case of all `switch(QuestionType)` to ensure
 * that when new question types are added but not fully supported across the UAT, they fail fast.
 */
public class UnsupportedQuestionTypeException extends Exception {
  public UnsupportedQuestionTypeException(QuestionType questionType) {
    super(String.format("QuestionType %s is not supported.", questionType));
  }
}
