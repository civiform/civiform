package services.question.exceptions;

import services.question.types.QuestionType;

/**
 * This exception should be thrown in the `default` case of all `switch(QuestionType)` to ensure
 * that when new question types are added but not fully supported across CiviForm, they fail fast.
 *
 * <p>NOTE: {@link InvalidQuestionTypeException} should be thrown if a question type is not valid.
 */
public class UnsupportedQuestionTypeException extends Exception {
  public UnsupportedQuestionTypeException(QuestionType questionType) {
    super(String.format("QuestionType %s is not supported.", questionType));
  }
}
