package support;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import models.LifecycleStage;
import models.Question;
import models.Version;
import services.LocalizedStrings;
import services.question.QuestionOption;
import services.question.exceptions.InvalidQuestionTypeException;
import services.question.exceptions.UnsupportedQuestionTypeException;
import services.question.types.AddressQuestionDefinition;
import services.question.types.CurrencyQuestionDefinition;
import services.question.types.DateQuestionDefinition;
import services.question.types.EmailQuestionDefinition;
import services.question.types.EnumeratorQuestionDefinition;
import services.question.types.FileUploadQuestionDefinition;
import services.question.types.IdQuestionDefinition;
import services.question.types.MultiOptionQuestionDefinition;
import services.question.types.MultiOptionQuestionDefinition.MultiOptionQuestionType;
import services.question.types.MultiOptionQuestionDefinition.MultiOptionValidationPredicates;
import services.question.types.NameQuestionDefinition;
import services.question.types.NumberQuestionDefinition;
import services.question.types.PhoneQuestionDefinition;
import services.question.types.QuestionDefinition;
import services.question.types.QuestionDefinitionConfig;
import services.question.types.QuestionType;
import services.question.types.StaticContentQuestionDefinition;
import services.question.types.TextQuestionDefinition;

/**
 * A cached {@link Question} bank for testing.
 *
 * <p>The {@link Question}s in this question bank should be treated as constants, but they need to
 * be persisted in the database for some tests so they are persisted and cached. When used with
 * tests that do not have a database available (see {@link #maybeSave(QuestionDefinition)}), the
 * question IDs may not be reliable since in production, the IDs are set by the database.
 *
 * <p>The properties of these questions (e.g. question help text) are not canonical and may not be
 * representative of the properties defined by CiviForm administrators.
 *
 * <p>To add a new {@link Question} to the question bank: create a {@link QuestionEnum} for it,
 * create a private method to construct the question, and create a public method to retrieve the
 * cached question. Add new methods in alphabetical order by {@link QuestionType}, grouping those
 * methods with the same type together.
 */
public class TestQuestionBank {
  private final Map<QuestionEnum, Question> questionCache = new ConcurrentHashMap<>();
  private final AtomicLong nextId = new AtomicLong(1L);

  private final boolean canSave;

  /**
   * Pass `true` if there is a database that comes up with the test (e.g., the test class extends
   * WithPostgresContainer), otherwise false.
   */
  public TestQuestionBank(boolean canSave) {
    this.canSave = canSave;
  }

  public void reset() {
    questionCache.clear();
    nextId.set(1L);
  }

  /**
   * Gets a single sample question for each supported QuestionType. Note that Question contents are
   * arbitrarily chosen and are not canonical.
   *
   * @return an ImmutableMap of QuestionType to Questions
   */
  public ImmutableMap<QuestionType, Question> getSampleQuestionsForAllTypes() {
    return new ImmutableMap.Builder<QuestionType, Question>()
        .put(QuestionType.ADDRESS, applicantAddress())
        .put(QuestionType.CHECKBOX, applicantKitchenTools())
        .put(QuestionType.CURRENCY, applicantMonthlyIncome())
        .put(QuestionType.DATE, applicantDate())
        .put(QuestionType.DROPDOWN, applicantIceCream())
        .put(QuestionType.EMAIL, applicantEmail())
        .put(QuestionType.FILEUPLOAD, applicantFile())
        .put(QuestionType.ID, applicantId())
        .put(QuestionType.NAME, applicantName())
        .put(QuestionType.NUMBER, applicantJugglingNumber())
        .put(QuestionType.RADIO_BUTTON, applicantSeason())
        .put(QuestionType.ENUMERATOR, applicantHouseholdMembers())
        .put(QuestionType.TEXT, applicantFavoriteColor())
        .put(QuestionType.STATIC, staticContent())
        .put(QuestionType.PHONE, applicantPhone())
        .build();
  }

  public Question applicantPhone() {
    return questionCache.computeIfAbsent(QuestionEnum.APPLICANT_PHONE, this::applicantPhone);
  }

  // Address
  public Question applicantAddress() {
    return questionCache.computeIfAbsent(QuestionEnum.APPLICANT_ADDRESS, this::applicantAddress);
  }

  // Address
  public Question applicantSecondaryAddress() {
    return questionCache.computeIfAbsent(
        QuestionEnum.APPLICANT_SECONDARY_ADDRESS, this::applicantSecondaryAddress);
  }

  // Checkbox
  public Question applicantKitchenTools() {
    return questionCache.computeIfAbsent(
        QuestionEnum.APPLICANT_KITCHEN_TOOLS, this::applicantKitchenTools);
  }

  // Date
  public Question applicantDate() {
    return questionCache.computeIfAbsent(QuestionEnum.APPLICANT_BIRTHDATE, this::applicantDate);
  }

  // Dropdown
  public Question applicantIceCream() {
    return questionCache.computeIfAbsent(QuestionEnum.APPLICANT_ICE_CREAM, this::applicantIceCream);
  }

  // Email
  public Question applicantEmail() {
    return questionCache.computeIfAbsent(QuestionEnum.APPLICANT_EMAIL, this::applicantEmail);
  }

  // Enumerator
  public Question applicantHouseholdMembers() {
    return questionCache.computeIfAbsent(
        QuestionEnum.APPLICANT_HOUSEHOLD_MEMBERS, this::applicantHouseholdMembers);
  }

  // Nested Enumerator
  public Question applicantHouseholdMemberJobs() {
    return questionCache.computeIfAbsent(
        QuestionEnum.APPLICANT_HOUSEHOLD_MEMBER_JOBS, this::applicantHouseholdMemberJobs);
  }

  // File upload
  public Question applicantFile() {
    return questionCache.computeIfAbsent(QuestionEnum.APPLICANT_FILE, this::applicantFile);
  }

  // Currency
  public Question applicantMonthlyIncome() {
    return questionCache.computeIfAbsent(
        QuestionEnum.APPLICANT_MONTHLY_INCOME, this::applicantMonthlyIncome);
  }

  // Id
  public Question applicantId() {
    return questionCache.computeIfAbsent(QuestionEnum.APPLICANT_ID, this::applicantId);
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

  // Number
  public Question applicantJugglingNumber() {
    return questionCache.computeIfAbsent(
        QuestionEnum.APPLICANT_JUGGLING_NUMBER, this::applicantJugglingNumber);
  }

  // Deeply nested Number
  public Question applicantHouseholdMemberDaysWorked() {
    return questionCache.computeIfAbsent(
        QuestionEnum.APPLICANT_HOUSEHOLD_MEMBER_DAYS_WORKED,
        this::applicantHouseholdMemberDaysWorked);
  }

  // Radio button
  public Question applicantSeason() {
    return questionCache.computeIfAbsent(QuestionEnum.APPLICANT_SEASON, this::applicantSeason);
  }

  // Text
  public Question applicantFavoriteColor() {
    return questionCache.computeIfAbsent(
        QuestionEnum.APPLICANT_FAVORITE_COLOR, this::applicantFavoriteColor);
  }

  // Text
  public Question staticContent() {
    return questionCache.computeIfAbsent(QuestionEnum.STATIC_CONTENT, this::staticContent);
  }

  // Address
  private Question applicantAddress(QuestionEnum ignore) {
    QuestionDefinition definition =
        new AddressQuestionDefinition(
            QuestionDefinitionConfig.builder()
                .setName("applicant address")
                .setDescription("The address of applicant")
                .setQuestionText(LocalizedStrings.of(Locale.US, "What is your address?"))
                .setQuestionHelpText(LocalizedStrings.of(Locale.US, "This is sample help text."))
                .setValidationPredicates(
                    AddressQuestionDefinition.AddressValidationPredicates.create())
                .build());
    return maybeSave(definition);
  }

  private Question applicantPhone(QuestionEnum ignore) {
    QuestionDefinition definition =
        new PhoneQuestionDefinition(
            QuestionDefinitionConfig.builder()
                .setName("applicant phone")
                .setDescription("The applicant Phone Number")
                .setQuestionText(LocalizedStrings.of(Locale.US, "What is your phone number?"))
                .setQuestionHelpText(LocalizedStrings.of(Locale.US, "This is sample help text."))
                .setValidationPredicates(PhoneQuestionDefinition.PhoneValidationPredicates.create())
                .build());
    return maybeSave(definition);
  }

  // Address
  private Question applicantSecondaryAddress(QuestionEnum ignore) {
    QuestionDefinition definition =
        new AddressQuestionDefinition(
            QuestionDefinitionConfig.builder()
                .setName("applicant secondary address")
                .setDescription("The secondary address of applicant")
                .setQuestionText(LocalizedStrings.of(Locale.US, "What is your secondary address?"))
                .setQuestionHelpText(LocalizedStrings.of(Locale.US, "This is sample help text."))
                .setValidationPredicates(
                    AddressQuestionDefinition.AddressValidationPredicates.create())
                .build());
    return maybeSave(definition);
  }

  // Checkbox
  private Question applicantKitchenTools(QuestionEnum ignore) {
    QuestionDefinitionConfig config =
        QuestionDefinitionConfig.builder()
            .setName("kitchen tools")
            .setDescription("Kitchen instruments you own")
            .setQuestionText(
                LocalizedStrings.of(
                    Locale.US, "Which of the following kitchen instruments do you own?"))
            .setQuestionHelpText(LocalizedStrings.of(Locale.US, "This is sample help text."))
            .setValidationPredicates(MultiOptionValidationPredicates.create())
            .build();
    ImmutableList<QuestionOption> questionOptions =
        ImmutableList.of(
            QuestionOption.create(1L, 1L, LocalizedStrings.of(Locale.US, "toaster")),
            QuestionOption.create(2L, 2L, LocalizedStrings.of(Locale.US, "pepper grinder")),
            QuestionOption.create(3L, 3L, LocalizedStrings.of(Locale.US, "garlic press")));
    QuestionDefinition definition =
        new MultiOptionQuestionDefinition(
            config, questionOptions, MultiOptionQuestionType.CHECKBOX);
    return maybeSave(definition);
  }

  // Dropdown
  private Question applicantIceCream(QuestionEnum ignore) {
    QuestionDefinitionConfig config =
        QuestionDefinitionConfig.builder()
            .setName("applicant ice cream")
            .setDescription("Select your favorite ice cream flavor")
            .setQuestionText(
                LocalizedStrings.of(
                    Locale.US, "Select your favorite ice cream flavor from the following"))
            .setQuestionHelpText(LocalizedStrings.of(Locale.US, "This is sample help text."))
            .setValidationPredicates(MultiOptionValidationPredicates.create())
            .build();
    ImmutableList<QuestionOption> questionOptions =
        ImmutableList.of(
            QuestionOption.create(1L, 1L, LocalizedStrings.of(Locale.US, "chocolate")),
            QuestionOption.create(2L, 2L, LocalizedStrings.of(Locale.US, "strawberry")),
            QuestionOption.create(3L, 3L, LocalizedStrings.of(Locale.US, "vanilla")),
            QuestionOption.create(4L, 4L, LocalizedStrings.of(Locale.US, "coffee")));
    QuestionDefinition definition =
        new MultiOptionQuestionDefinition(
            config, questionOptions, MultiOptionQuestionType.DROPDOWN);
    return maybeSave(definition);
  }

  // Enumerator
  private Question applicantHouseholdMembers(QuestionEnum ignore) {
    QuestionDefinition definition =
        new EnumeratorQuestionDefinition(
            QuestionDefinitionConfig.builder()
                .setName("applicant household members")
                .setDescription("The applicant's household members")
                .setQuestionText(LocalizedStrings.of(Locale.US, "Who are your household members?"))
                .setQuestionHelpText(LocalizedStrings.of(Locale.US, "This is sample help text."))
                .setValidationPredicates(
                    EnumeratorQuestionDefinition.EnumeratorValidationPredicates.create())
                .build(),
            LocalizedStrings.empty());
    return maybeSave(definition);
  }

  // Nested Enumerator
  private Question applicantHouseholdMemberJobs(QuestionEnum ignore) {
    Question householdMembers = applicantHouseholdMembers();
    QuestionDefinition definition =
        new EnumeratorQuestionDefinition(
            QuestionDefinitionConfig.builder()
                .setName("household members jobs")
                .setDescription("The applicant's household member's jobs")
                .setQuestionText(LocalizedStrings.of(Locale.US, "What are the $this's jobs?"))
                .setQuestionHelpText(LocalizedStrings.of(Locale.US, "Where does $this work?"))
                .setValidationPredicates(
                    EnumeratorQuestionDefinition.EnumeratorValidationPredicates.create())
                .setEnumeratorId(Optional.of(householdMembers.id))
                .build(),
            LocalizedStrings.empty());
    return maybeSave(definition);
  }

  // File upload
  private Question applicantFile(QuestionEnum ignore) {
    QuestionDefinition definition =
        new FileUploadQuestionDefinition(
            QuestionDefinitionConfig.builder()
                .setName("applicant file")
                .setDescription("The file to be uploaded")
                .setQuestionText(
                    LocalizedStrings.of(Locale.US, "What is the file you want to upload?"))
                .setQuestionHelpText(LocalizedStrings.of(Locale.US, "This is sample help text."))
                .setValidationPredicates(
                    FileUploadQuestionDefinition.FileUploadValidationPredicates.create())
                .build());
    return maybeSave(definition);
  }

  // Currency
  private Question applicantMonthlyIncome(QuestionEnum ignore) {
    QuestionDefinition definition =
        new CurrencyQuestionDefinition(
            QuestionDefinitionConfig.builder()
                .setName("applicant monthly income")
                .setDescription("monthly income of applicant")
                .setQuestionText(LocalizedStrings.of(Locale.US, "what is your monthly income?"))
                .setQuestionHelpText(LocalizedStrings.of(Locale.US, "help text"))
                .setValidationPredicates(
                    CurrencyQuestionDefinition.CurrencyValidationPredicates.create())
                .build());
    return maybeSave(definition);
  }

  // Id
  private Question applicantId(QuestionEnum ignore) {
    QuestionDefinition definition =
        new IdQuestionDefinition(
            QuestionDefinitionConfig.builder()
                .setName("applicant id")
                .setDescription("1234")
                .setQuestionText(LocalizedStrings.of(Locale.US, "What is the the id?"))
                .setQuestionHelpText(LocalizedStrings.of(Locale.US, "This is sample help text."))
                .setValidationPredicates(IdQuestionDefinition.IdValidationPredicates.create())
                .build());
    return maybeSave(definition);
  }

  // Name
  private Question applicantName(QuestionEnum ignore) {
    QuestionDefinition definition =
        new NameQuestionDefinition(
            QuestionDefinitionConfig.builder()
                .setName("applicant name")
                .setDescription("name of applicant")
                .setQuestionText(LocalizedStrings.of(Locale.US, "what is your name?"))
                .setQuestionHelpText(LocalizedStrings.of(Locale.US, "help text"))
                .setValidationPredicates(NameQuestionDefinition.NameValidationPredicates.create())
                .build());
    return maybeSave(definition);
  }

  // Repeated name
  private Question applicantHouseholdMemberName(QuestionEnum ignore) {
    Question householdMembers = applicantHouseholdMembers();
    QuestionDefinition definition =
        new NameQuestionDefinition(
            QuestionDefinitionConfig.builder()
                .setName("household members name")
                .setDescription("The applicant's household member's name")
                .setQuestionText(LocalizedStrings.of(Locale.US, "What is the $this's name?"))
                .setQuestionHelpText(
                    LocalizedStrings.of(Locale.US, "Please provide full name for $this."))
                .setValidationPredicates(NameQuestionDefinition.NameValidationPredicates.create())
                .setEnumeratorId(Optional.of(householdMembers.id))
                .build());

    return maybeSave(definition);
  }

  // Number
  private Question applicantJugglingNumber(QuestionEnum ignore) {
    QuestionDefinition definition =
        new NumberQuestionDefinition(
            QuestionDefinitionConfig.builder()
                .setName("number of items applicant can juggle")
                .setDescription("The number of items applicant can juggle at once")
                .setQuestionText(
                    LocalizedStrings.of(Locale.US, "How many items can you juggle at one time?"))
                .setQuestionHelpText(LocalizedStrings.of(Locale.US, "This is sample help text."))
                .setValidationPredicates(
                    NumberQuestionDefinition.NumberValidationPredicates.create())
                .build());
    return maybeSave(definition);
  }

  // Date
  private Question applicantDate(QuestionEnum ignore) {
    QuestionDefinition definition =
        new DateQuestionDefinition(
            QuestionDefinitionConfig.builder()
                .setName("applicant birth date")
                .setDescription("The applicant birth date")
                .setQuestionText(LocalizedStrings.of(Locale.US, "What is your birthdate?"))
                .setQuestionHelpText(LocalizedStrings.of(Locale.US, "This is sample help text."))
                .setValidationPredicates(DateQuestionDefinition.DateValidationPredicates.create())
                .build());
    return maybeSave(definition);
  }

  // Email
  private Question applicantEmail(QuestionEnum ignore) {
    QuestionDefinition definition =
        new EmailQuestionDefinition(
            QuestionDefinitionConfig.builder()
                .setName("applicant Email address")
                .setDescription("The applicant Email address")
                .setQuestionText(LocalizedStrings.of(Locale.US, "What is your Email?"))
                .setQuestionHelpText(LocalizedStrings.of(Locale.US, "This is sample help text."))
                .setValidationPredicates(EmailQuestionDefinition.EmailValidationPredicates.create())
                .build());
    return maybeSave(definition);
  }

  // Deeply Nested Number
  private Question applicantHouseholdMemberDaysWorked(QuestionEnum ignore) {
    Question householdMemberJobs = applicantHouseholdMemberJobs();
    QuestionDefinition definition =
        new NumberQuestionDefinition(
            QuestionDefinitionConfig.builder()
                .setName("household members days worked")
                .setDescription("The applicant's household member's number of days worked")
                .setQuestionText(
                    LocalizedStrings.of(
                        Locale.US, "How many days has $this.parent worked at $this?"))
                .setQuestionHelpText(
                    LocalizedStrings.of(
                        Locale.US, "How many days has $this.parent worked at $this?"))
                .setValidationPredicates(
                    NumberQuestionDefinition.NumberValidationPredicates.create())
                .setEnumeratorId(Optional.of(householdMemberJobs.id))
                .build());

    return maybeSave(definition);
  }

  // Radio button
  private Question applicantSeason(QuestionEnum ignore) {
    QuestionDefinitionConfig config =
        QuestionDefinitionConfig.builder()
            .setName("radio")
            .setDescription("Favorite season in the year")
            .setQuestionText(LocalizedStrings.of(Locale.US, "What is your favorite season?"))
            .setQuestionHelpText(LocalizedStrings.of(Locale.US, "This is sample help text."))
            .setValidationPredicates(MultiOptionValidationPredicates.create())
            .build();
    ImmutableList<QuestionOption> questionOptions =
        ImmutableList.of(
            QuestionOption.create(1L, 1L, LocalizedStrings.of(Locale.US, "winter")),
            QuestionOption.create(2L, 2L, LocalizedStrings.of(Locale.US, "spring")),
            QuestionOption.create(3L, 3L, LocalizedStrings.of(Locale.US, "summer")),
            QuestionOption.create(4L, 4L, LocalizedStrings.of(Locale.US, "fall")));
    QuestionDefinition definition =
        new MultiOptionQuestionDefinition(
            config, questionOptions, MultiOptionQuestionType.RADIO_BUTTON);
    return maybeSave(definition);
  }

  // Static
  private Question staticContent(QuestionEnum ignore) {
    QuestionDefinition definition =
        new StaticContentQuestionDefinition(
            QuestionDefinitionConfig.builder()
                .setName("more info about something")
                .setDescription("Shows more info to the applicant")
                .setQuestionText(LocalizedStrings.of(Locale.US, "This is more info"))
                .setQuestionHelpText(LocalizedStrings.of(Locale.US, ""))
                .setValidationPredicates(
                    StaticContentQuestionDefinition.StaticContentValidationPredicates.create())
                .build());
    return maybeSave(definition);
  }
  // Text
  private Question applicantFavoriteColor(QuestionEnum ignore) {
    QuestionDefinition definition =
        new TextQuestionDefinition(
            QuestionDefinitionConfig.builder()
                .setName("applicant favorite color")
                .setDescription("Favorite color of applicant")
                .setQuestionText(LocalizedStrings.of(Locale.US, "What is your favorite color?"))
                .setQuestionHelpText(LocalizedStrings.of(Locale.US, "This is sample help text."))
                .setValidationPredicates(TextQuestionDefinition.TextValidationPredicates.create())
                .setValidationPredicates(TextQuestionDefinition.TextValidationPredicates.create())
                .build());
    return maybeSave(definition);
  }

  private Question maybeSave(QuestionDefinition questionDefinition) {
    return maybeSave(questionDefinition, LifecycleStage.ACTIVE);
  }

  public Question maybeSave(QuestionDefinition questionDefinition, LifecycleStage desiredStage) {
    Question question = new Question(questionDefinition);
    if (canSave) {
      // This odd way of finding the active version is because this class
      // doesn't have access to the Version repository, because it needs to
      // work without the database.
      Version versionToAdd;
      Optional<Version> existingVersion =
          question
              .db()
              .find(Version.class)
              .where()
              .eq("lifecycle_stage", desiredStage)
              .findOneOrEmpty();
      if (existingVersion.isEmpty()) {
        versionToAdd = new Version(desiredStage);
        versionToAdd.save();
      } else {
        versionToAdd = existingVersion.get();
      }
      question.addVersion(versionToAdd);
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
    APPLICANT_ADDRESS,
    APPLICANT_SECONDARY_ADDRESS,
    APPLICANT_FAVORITE_COLOR,
    APPLICANT_FILE,
    APPLICANT_ID,
    APPLICANT_HOUSEHOLD_MEMBERS,
    APPLICANT_HOUSEHOLD_MEMBER_DAYS_WORKED,
    APPLICANT_HOUSEHOLD_MEMBER_NAME,
    APPLICANT_HOUSEHOLD_MEMBER_JOBS,
    APPLICANT_MONTHLY_INCOME,
    APPLICANT_ICE_CREAM,
    APPLICANT_JUGGLING_NUMBER,
    APPLICANT_KITCHEN_TOOLS,
    APPLICANT_NAME,
    APPLICANT_BIRTHDATE,
    APPLICANT_SEASON,
    APPLICANT_EMAIL,
    STATIC_CONTENT,
    APPLICANT_PHONE,
  }
}
