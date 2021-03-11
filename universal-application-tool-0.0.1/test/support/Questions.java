package support;

import com.google.common.collect.ImmutableMap;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import models.Question;
import services.Path;
import services.question.AddressQuestionDefinition;
import services.question.NameQuestionDefinition;
import services.question.QuestionDefinition;
import services.question.TextQuestionDefinition;

public class Questions {
  private static final long VERSION = 1L;

  private static final Map<String, Question> questionCache = new ConcurrentHashMap<>();

  public static void reset() {
    questionCache.clear();
  }

  public static Question applicantName() {
    return questionCache.computeIfAbsent(
        "applicant name",
        ignore -> {
          QuestionDefinition definition =
              new NameQuestionDefinition(
                  VERSION,
                  "applicant name",
                  Path.create("applicant.name"),
                  "name of applicant",
                  ImmutableMap.of(Locale.ENGLISH, "what is your name?"),
                  ImmutableMap.of(Locale.US, "help text"));
          Question question = new Question(definition);
          question.save();
          return question;
        });
  }

  public static Question applicantAddress() {
    return questionCache.computeIfAbsent(
        "applicant address",
        ignore -> {
          QuestionDefinition definition =
              new AddressQuestionDefinition(
                  VERSION,
                  "applicant address",
                  Path.create("applicant.address"),
                  "address of applicant",
                  ImmutableMap.of(Locale.US, "what is your address?"),
                  ImmutableMap.of(Locale.US, "help text"));
          Question question = new Question(definition);
          question.save();
          return question;
        });
  }

  public static Question applicantFavoriteColor() {
    return questionCache.computeIfAbsent(
        "applicant favorite color",
        ignore -> {
          QuestionDefinition definition =
              new TextQuestionDefinition(
                  VERSION,
                  "applicant favorite color",
                  Path.create("applicant.color"),
                  "favorite color of applicant",
                  ImmutableMap.of(Locale.US, "what is your favorite color?"),
                  ImmutableMap.of(Locale.US, "help text"));
          Question question = new Question(definition);
          question.save();
          return question;
        });
  }
}
