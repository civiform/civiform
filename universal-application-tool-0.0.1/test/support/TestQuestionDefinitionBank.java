package support;

import com.google.common.collect.ImmutableMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import models.LifecycleStage;
import models.Question;
import services.Path;
import services.question.types.AddressQuestionDefinition;
import services.question.types.NameQuestionDefinition;
import services.question.types.QuestionDefinition;
import services.question.types.QuestionType;
import services.question.types.TextQuestionDefinition;

/**
 * A cached {@link QuestionDefinition} bank for testing.
 *
 * <p>The properties of these questions (e.g. question path) are not canonical and may not be
 * representative of the properties defined by CiviForm administrators.
 *
 * <p>To add a new {@link Question} to the question bank: create a private static method to
 * construct the question, and create a public static method to retrieve the cached question.
 */
public class TestQuestionDefinitionBank {

  private static final long VERSION = 1L;
  private static final Map<QuestionType, QuestionDefinition> questionDefinitionCache =
      new ConcurrentHashMap<>();

  public static void reset() {
    questionDefinitionCache.clear();
  }

  public static QuestionDefinition address() {
    return questionDefinitionCache.computeIfAbsent(
        QuestionType.ADDRESS, TestQuestionDefinitionBank::address);
  }

  public static QuestionDefinition name() {
    return questionDefinitionCache.computeIfAbsent(
        QuestionType.NAME, TestQuestionDefinitionBank::name);
  }

  public static QuestionDefinition text() {
    return questionDefinitionCache.computeIfAbsent(
        QuestionType.TEXT, TestQuestionDefinitionBank::text);
  }

  private static QuestionDefinition name(QuestionType ignore) {
    return new NameQuestionDefinition(
        VERSION,
        "applicant name",
        Path.create("applicant.applicant_name"),
        Optional.empty(),
        "name of applicant",
        LifecycleStage.ACTIVE,
        ImmutableMap.of(Locale.US, "what is your name?"),
        ImmutableMap.of(Locale.US, "help text"));
  }

  private static QuestionDefinition address(QuestionType ignore) {
    return new AddressQuestionDefinition(
        VERSION,
        "applicant address",
        Path.create("applicant.applicant_address"),
        Optional.empty(),
        "address of applicant",
        LifecycleStage.ACTIVE,
        ImmutableMap.of(Locale.US, "what is your address?"),
        ImmutableMap.of(Locale.US, "help text"));
  }

  private static QuestionDefinition text(QuestionType ignore) {
    return new TextQuestionDefinition(
        VERSION,
        "applicant favorite color",
        Path.create("applicant.applicant_favorite_color"),
        Optional.empty(),
        "favorite color of applicant",
        LifecycleStage.ACTIVE,
        ImmutableMap.of(Locale.US, "what is your favorite color?"),
        ImmutableMap.of(Locale.US, "help text"));
  }
}
