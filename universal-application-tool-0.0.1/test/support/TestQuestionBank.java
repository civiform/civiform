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

/**
 * A cached {@link Question} bank for testing.
 *
 * <p>The {@link Question}s in this question bank should be treated as constants, but they need to
 * be persisted in the database for some tests so they are persisted and cached. The properties of
 * these questions (e.g. question path) are not canonical and may not be representative of the
 * properties defined by CiviForm administrators.
 *
 * <p>To add a new {@link Question} to the question bank: create a {@link QuestionEnum} for it,
 * create a private static method to construct the question, and create a public static method to
 * retrieve the cached question.
 */
public class TestQuestionBank {
  private static final long VERSION = 1L;

  private enum QuestionEnum {
    APPLICANT_NAME,
    APPLICANT_ADDRESS,
    APPLICANT_FAVORITE_COLOR
  }

  private static final Map<QuestionEnum, Question> questionCache = new ConcurrentHashMap<>();

  public static void reset() {
    questionCache.clear();
  }

  public static Question applicantName() {
    return questionCache.computeIfAbsent(
        QuestionEnum.APPLICANT_NAME, TestQuestionBank::applicantName);
  }

  public static Question applicantAddress() {
    return questionCache.computeIfAbsent(
        QuestionEnum.APPLICANT_ADDRESS, TestQuestionBank::applicantAddress);
  }

  public static Question applicantFavoriteColor() {
    return questionCache.computeIfAbsent(
        QuestionEnum.APPLICANT_FAVORITE_COLOR, TestQuestionBank::applicantFavoriteColor);
  }

  private static Question applicantName(QuestionEnum ignore) {
    QuestionDefinition definition =
        new NameQuestionDefinition(
            VERSION,
            "applicant name",
            Path.create("applicant.name"),
            "name of applicant",
            ImmutableMap.of(Locale.US, "what is your name?"),
            ImmutableMap.of(Locale.US, "help text"));
    Question question = new Question(definition);
    question.save();
    return question;
  }

  private static Question applicantAddress(QuestionEnum ignore) {
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
  }

  private static Question applicantFavoriteColor(QuestionEnum ignore) {
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
  }
}
