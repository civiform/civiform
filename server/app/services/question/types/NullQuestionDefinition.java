package services.question.types;

import com.google.auto.value.AutoValue;

/**
 * This is an empty QuestionDefinition that is used when the system can't find the question. It
 * works with the NullQuestionDefinitionConfig class to provide a placeholder object that assists in
 * letting the application to continue loading in the event of an error.
 *
 * <p>In normal operation this shouldn't ever be reached, but an occasional gremlin has resulted in
 * the rare instance of a program pointing at an old version of a question.
 */
public class NullQuestionDefinition extends QuestionDefinition {

  @AutoValue
  public static class NullValidationPredicates extends ValidationPredicates {

    public static NullQuestionDefinition.NullValidationPredicates parse(String jsonString) {
      return new NullValidationPredicates() {
        @Override
        public String serializeAsString() {
          return "";
        }
      };
    }

    public static NullQuestionDefinition.NullValidationPredicates create() {
      return new NullValidationPredicates() {
        @Override
        public String serializeAsString() {
          return "";
        }
      };
    }
  }

  public NullQuestionDefinition(long id) {
    super(new NullQuestionDefinitionConfig(id));
  }

  /** Get the type of this question. */
  @Override
  public QuestionType getQuestionType() {
    return QuestionType.NULL_QUESTION;
  }

  /** Get the default validation predicates for this question type. */
  @Override
  ValidationPredicates getDefaultValidationPredicates() {
    return new NullQuestionDefinition.NullValidationPredicates();
  }
}
