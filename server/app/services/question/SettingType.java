package services.question;

import services.question.types.QuestionType;

import java.util.Set;

/** Defines types of question settings and how they will be used. */
public interface SettingType {
  /**
   * Which question types can use this setting type
   */
  default Set<QuestionType> getSupportedQuestionTypes() {
    return Set.of();
  }
}
