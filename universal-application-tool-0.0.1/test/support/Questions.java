package support;

import com.google.common.collect.ImmutableMap;
import models.Question;
import services.Path;
import services.question.AddressQuestionDefinition;
import services.question.QuestionDefinition;
import services.question.TextQuestionDefinition;

public class Questions {
  private static final long VERSION = 1L;

  public static Question applicantName() {
    QuestionDefinition definition =
        new TextQuestionDefinition(
            VERSION,
            "applicant name",
            Path.create("applicant.name"),
            "name of applicant",
            ImmutableMap.of(),
            ImmutableMap.of());
    Question question = new Question(definition);
    question.save();
    return question;
  }

  public static Question applicantAddress() {
    QuestionDefinition definition =
        new AddressQuestionDefinition(
            VERSION,
            "applicant address",
            Path.create("applicant.address"),
            "address of applicant",
            ImmutableMap.of(),
            ImmutableMap.of());
    Question question = new Question(definition);
    question.save();
    return question;
  }

  public static Question applicantFavoriteColor() {
    QuestionDefinition definition =
        new TextQuestionDefinition(
            VERSION,
            "applicant favorite color",
            Path.create("applicant.color"),
            "favorite color of applicant",
            ImmutableMap.of(),
            ImmutableMap.of());
    Question question = new Question(definition);
    question.save();
    return question;
  }
}
