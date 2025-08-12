package services.question;

import java.util.Set;
import services.question.types.QuestionType;

/** Defines types of question settings and how they will be used. */
public interface SettingType {
  /** Which question types can use this setting type */
  default Set<QuestionType> getSupportedQuestionTypes() {
    return Set.of();
  }
}
