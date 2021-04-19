package support;

import com.google.common.collect.ImmutableListMultimap;
import com.google.common.collect.ImmutableMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import javax.persistence.PersistenceException;
import models.LifecycleStage;
import models.Question;
import services.Path;
import services.question.exceptions.InvalidQuestionTypeException;
import services.question.exceptions.UnsupportedQuestionTypeException;
import services.question.types.AddressQuestionDefinition;
import services.question.types.CheckboxQuestionDefinition;
import services.question.types.DropdownQuestionDefinition;
import services.question.types.NameQuestionDefinition;
import services.question.types.NumberQuestionDefinition;
import services.question.types.QuestionDefinition;
import services.question.types.RadioButtonQuestionDefinition;
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
  private static final Map<QuestionEnum, Question> questionCache = new ConcurrentHashMap<>();
  private static final AtomicLong nextId = new AtomicLong(1L);

  public static void reset() {
    questionCache.clear();
    nextId.set(1L);
  }

  // Address
  public static Question applicantAddress() {
    return questionCache.computeIfAbsent(
        QuestionEnum.APPLICANT_ADDRESS, TestQuestionBank::applicantAddress);
  }

  // Checkbox
  public static Question applicantKitchenTools() {
    return questionCache.computeIfAbsent(
        QuestionEnum.APPLICANT_KITCHEN_TOOLS, TestQuestionBank::applicantKitchenTools);
  }

  // Dropdown
  public static Question applicantIceCream() {
    return questionCache.computeIfAbsent(
        QuestionEnum.APPLICANT_ICE_CREAM, TestQuestionBank::applicantIceCream);
  }

  // Name
  public static Question applicantName() {
    return questionCache.computeIfAbsent(
        QuestionEnum.APPLICANT_NAME, TestQuestionBank::applicantName);
  }

  // Number
  public static Question applicantJugglingNumber() {
    return questionCache.computeIfAbsent(
        QuestionEnum.APPLICANT_JUGGLING_NUMBER, TestQuestionBank::applicantJugglingNumber);
  }

  // Radio button
  public static Question applicantSeason() {
    return questionCache.computeIfAbsent(
        QuestionEnum.APPLICANT_SEASON, TestQuestionBank::applicantSeason);
  }

  // Repeater
  public static Question applicantHouseholdMembers() {
    return questionCache.computeIfAbsent(
        QuestionEnum.APPLICANT_HOUSEHOLD_MEMBERS, TestQuestionBank::applicantHouseholdMembers);
  }

  // Repeated name
  public static Question applicantHouseholdMemberName() {
    return questionCache.computeIfAbsent(
        QuestionEnum.APPLICANT_HOUSEHOLD_MEMBER_NAME,
        TestQuestionBank::applicantHouseholdMemberName);
  }

  // Text
  public static Question applicantFavoriteColor() {
    return questionCache.computeIfAbsent(
        QuestionEnum.APPLICANT_FAVORITE_COLOR, TestQuestionBank::applicantFavoriteColor);
  }

  // Address
  private static Question applicantAddress(QuestionEnum ignore) {
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

  // Checkbox
  private static Question applicantKitchenTools(QuestionEnum ignore) {
    QuestionDefinition definition =
        new CheckboxQuestionDefinition(
            1L,
            "kitchen tools",
            Path.create("applicant.kitchen_tools"),
            Optional.empty(),
            "description",
            LifecycleStage.ACTIVE,
            ImmutableMap.of(Locale.US, "Which of the following kitchen instruments do you own?"),
            ImmutableMap.of(Locale.US, "help text"),
            ImmutableListMultimap.of(
                Locale.US, "toaster", Locale.US, "pepper grinder", Locale.US, "garlic press"));
    return maybeSave(definition);
  }

  // Dropdown
  private static Question applicantIceCream(QuestionEnum ignore) {
    QuestionDefinition definition =
        new DropdownQuestionDefinition(
            1L,
            "applicant ice cream",
            Path.create("applicant.applicant_ice_cream"),
            Optional.empty(),
            "select your favorite ice cream flavor",
            LifecycleStage.ACTIVE,
            ImmutableMap.of(Locale.US, "Select your favorite ice cream flavor from the following"),
            ImmutableMap.of(Locale.US, "this is sample help text"),
            ImmutableListMultimap.of(
                Locale.US,
                "chocolate",
                Locale.US,
                "strawberry",
                Locale.US,
                "vanilla",
                Locale.US,
                "coffee"));
    return maybeSave(definition);
  }

  // Name
  private static Question applicantName(QuestionEnum ignore) {
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

  // Number
  private static Question applicantJugglingNumber(QuestionEnum ignore) {
    QuestionDefinition definition =
        new NumberQuestionDefinition(
            VERSION,
            "number of items applicant can juggle",
            Path.create("applicant.juggling_number"),
            Optional.empty(),
            "number of items applicant can juggle at once",
            LifecycleStage.ACTIVE,
            ImmutableMap.of(Locale.US, "How many items can you juggle at one time?"),
            ImmutableMap.of(Locale.US, "help text"));
    return maybeSave(definition);
  }

  // Radio button
  private static Question applicantSeason(QuestionEnum ignore) {
    QuestionDefinition definition =
        new RadioButtonQuestionDefinition(
            1L,
            "radio",
            Path.create("applicant.radio"),
            Optional.empty(),
            "favorite season in the year",
            LifecycleStage.ACTIVE,
            ImmutableMap.of(Locale.US, "What is your favorite season?"),
            ImmutableMap.of(Locale.US, "this is sample help text"),
            ImmutableListMultimap.of(
                Locale.US, "winter", Locale.US, "spring", Locale.US, "summer", Locale.US, "fall"));
    return maybeSave(definition);
  }

  // Repeater
  private static Question applicantHouseholdMembers(QuestionEnum ignore) {
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

  // Repeated name
  private static Question applicantHouseholdMemberName(QuestionEnum ignore) {
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

  // Text
  private static Question applicantFavoriteColor(QuestionEnum ignore) {
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

  private static Question maybeSave(QuestionDefinition questionDefinition) {
    Question question = new Question(questionDefinition);
    try {
      question.save();
    } catch (ExceptionInInitializerError | NoClassDefFoundError | PersistenceException ignore) {
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
    APPLICANT_ADDRESS,
    APPLICANT_KITCHEN_TOOLS,
    APPLICANT_ICE_CREAM,
    APPLICANT_SEASON,
    APPLICANT_NAME,
    APPLICANT_JUGGLING_NUMBER,
    APPLICANT_HOUSEHOLD_MEMBERS,
    APPLICANT_HOUSEHOLD_MEMBER_NAME,
    APPLICANT_FAVORITE_COLOR
  }
}
