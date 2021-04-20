package support;

import com.google.common.collect.ImmutableMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import models.LifecycleStage;
import models.Question;
import services.Path;
import services.question.exceptions.InvalidQuestionTypeException;
import services.question.exceptions.UnsupportedQuestionTypeException;
import services.question.types.AddressQuestionDefinition;
import services.question.types.NameQuestionDefinition;
import services.question.types.QuestionDefinition;
import services.question.types.RepeaterQuestionDefinition;
import services.question.types.TextQuestionDefinition;

/**
 * A cached {@link Question} bank for testing.
 *
 * <p>The {@link Question}s in this question bank should be treated as constants, but they need to
 * be persisted in the database for some tests so they are persisted and cached. When used with
 * tests that do not have a database available (see {@link #maybeSave(QuestionDefinition)}), the
 * question IDs may not be reliable since in production, the IDs are set by the database.
 *
 * <p>The properties of these questions (e.g. question path) are not canonical and may not be
 * representative of the properties defined by CiviForm administrators.
 *
 * <p>To add a new {@link Question} to the question bank: create a {@link QuestionEnum} for it,
 * create a private static method to construct the question, and create a public static method to
 * retrieve the cached question.
 */
public class TestQuestionBank {
  private static final long VERSION = 1L;

  private boolean canSave = false;
  private Map<QuestionEnum, Question> questionCache = new ConcurrentHashMap<>();
  private AtomicLong nextId = new AtomicLong(1L);

  public TestQuestionBank(boolean canSave) {
    this.canSave = canSave;
  }

  public void reset() {
    questionCache.clear();
    nextId.set(1L);
  }

  // Address
  public Question applicantAddress() {
    return questionCache.computeIfAbsent(QuestionEnum.APPLICANT_ADDRESS, this::applicantAddress);
  }

  // Name
  public Question applicantName() {
    return questionCache.computeIfAbsent(QuestionEnum.APPLICANT_NAME, this::applicantName);
  }

  // Repeated name
  public Question applicantHouseholdMemberName() {
    // Make sure the next call will have the question ready
    applicantHouseholdMembers();
    return questionCache.computeIfAbsent(
        QuestionEnum.APPLICANT_HOUSEHOLD_MEMBER_NAME, this::applicantHouseholdMemberName);
  }

  // Repeater
  public Question applicantHouseholdMembers() {
    return questionCache.computeIfAbsent(
        QuestionEnum.APPLICANT_HOUSEHOLD_MEMBERS, this::applicantHouseholdMembers);
  }

  // Text
  public Question applicantFavoriteColor() {
    return questionCache.computeIfAbsent(
        QuestionEnum.APPLICANT_FAVORITE_COLOR, this::applicantFavoriteColor);
  }

  // Address
  private Question applicantAddress(QuestionEnum ignore) {
    QuestionDefinition definition =
        new AddressQuestionDefinition(
            VERSION,
            "applicant address",
            Path.create("applicant.applicant_address"),
            Optional.empty(),
            "address of applicant",
            LifecycleStage.ACTIVE,
            ImmutableMap.of(Locale.US, "what is your address?"),
            ImmutableMap.of(Locale.US, "help text"));
    return maybeSave(definition);
  }

  // Name
  private Question applicantName(QuestionEnum ignore) {
    QuestionDefinition definition =
        new NameQuestionDefinition(
            VERSION,
            "applicant name",
            Path.create("applicant.applicant_name"),
            Optional.empty(),
            "name of applicant",
            LifecycleStage.ACTIVE,
            ImmutableMap.of(Locale.US, "what is your name?"),
            ImmutableMap.of(Locale.US, "help text"));
    return maybeSave(definition);
  }

  // Repeated name
  private Question applicantHouseholdMemberName(QuestionEnum ignore) {
    Question householdMembers = applicantHouseholdMembers();
    QuestionDefinition definition =
            new NameQuestionDefinition(
                    VERSION,
                    "household members name",
                    Path.create("applicant.applicant_household_members[].name"),
                    Optional.of(householdMembers.id),
                    "The applicant's household member's name",
                    LifecycleStage.ACTIVE,
                    ImmutableMap.of(Locale.US, "what is the household member's name?"),
                    ImmutableMap.of(Locale.US, "help text"));
    return maybeSave(definition);
  }

  // Repeater
  private Question applicantHouseholdMembers(QuestionEnum ignore) {
    QuestionDefinition definition =
        new RepeaterQuestionDefinition(
            VERSION,
            "applicant household members",
            Path.create("applicant.applicant_household_members[]"),
            Optional.empty(),
            "The applicant's household members",
            LifecycleStage.ACTIVE,
            ImmutableMap.of(Locale.US, "Who are your household members?"),
            ImmutableMap.of(Locale.US, "help text"));
    return maybeSave(definition);
  }

  // Text
  private Question applicantFavoriteColor(QuestionEnum ignore) {
    QuestionDefinition definition =
            new TextQuestionDefinition(
                    VERSION,
                    "applicant favorite color",
                    Path.create("applicant.applicant_favorite_color"),
                    Optional.empty(),
                    "favorite color of applicant",
                    LifecycleStage.ACTIVE,
                    ImmutableMap.of(Locale.US, "what is your favorite color?"),
                    ImmutableMap.of(Locale.US, "help text"));
    return maybeSave(definition);
  }

  private Question maybeSave(QuestionDefinition questionDefinition) {
    Question question = new Question(questionDefinition);
    if (canSave) {
      question.save();
    } else {
      question.id = nextId.getAndIncrement();
      try {
        question.loadQuestionDefinition();
      } catch (InvalidQuestionTypeException | UnsupportedQuestionTypeException e) {
        throw new IllegalArgumentException(
            "Questions in the TestQuestionBank better be supported QuestionTypes.");
      }
    }
    return question;
  }

  private enum QuestionEnum {
    APPLICANT_NAME,
    APPLICANT_ADDRESS,
    APPLICANT_FAVORITE_COLOR,
    APPLICANT_HOUSEHOLD_MEMBERS,
    APPLICANT_HOUSEHOLD_MEMBER_NAME
  }
}
