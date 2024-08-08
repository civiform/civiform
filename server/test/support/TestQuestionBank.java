package support;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import models.LifecycleStage;
import models.QuestionModel;
import models.VersionModel;
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
import services.question.types.NameQuestionDefinition;
import services.question.types.NullQuestionDefinition;
import services.question.types.NumberQuestionDefinition;
import services.question.types.PhoneQuestionDefinition;
import services.question.types.QuestionDefinition;
import services.question.types.QuestionDefinitionConfig;
import services.question.types.QuestionType;
import services.question.types.StaticContentQuestionDefinition;
import services.question.types.TextQuestionDefinition;

/**
 * A cached {@link QuestionModel} bank for testing.
 *
 * <p>The {@link QuestionModel}s in this question bank should be treated as constants, but they need
 * to be persisted in the database for some tests so they are persisted and cached. When used with
 * tests that do not have a database available (see {@link #maybeSave(QuestionDefinition)}), the
 * question IDs may not be reliable since in production, the IDs are set by the database.
 *
 * <p>The properties of these questions (e.g. question help text) are not canonical and may not be
 * representative of the properties defined by CiviForm administrators.
 *
 * <p>To add a new {@link QuestionModel} to the question bank: create a {@link QuestionEnum} for it,
 * create a private method to construct the question, and create a public method to retrieve the
 * cached question. Add new methods in alphabetical order by {@link QuestionType}, grouping those
 * methods with the same type together.
 */
public class TestQuestionBank {
  private final Map<QuestionEnum, QuestionModel> questionCache = new ConcurrentHashMap<>();
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
  public ImmutableMap<QuestionType, QuestionModel> getSampleQuestionsForAllTypes() {
    return new ImmutableMap.Builder<QuestionType, QuestionModel>()
        .put(QuestionType.ADDRESS, addressApplicantAddress())
        .put(QuestionType.CHECKBOX, checkboxApplicantKitchenTools())
        .put(QuestionType.CURRENCY, currencyApplicantMonthlyIncome())
        .put(QuestionType.DATE, dateApplicantBirthdate())
        .put(QuestionType.DROPDOWN, dropdownApplicantIceCream())
        .put(QuestionType.EMAIL, emailApplicantEmail())
        .put(QuestionType.ENUMERATOR, enumeratorApplicantHouseholdMembers())
        .put(QuestionType.FILEUPLOAD, fileUploadApplicantFile())
        .put(QuestionType.ID, IdApplicantId())
        .put(QuestionType.NAME, nameApplicantName())
        .put(QuestionType.NUMBER, numberApplicantJugglingNumber())
        .put(QuestionType.PHONE, phoneApplicantPhone())
        .put(QuestionType.RADIO_BUTTON, radioApplicantFavoriteSeason())
        .put(QuestionType.STATIC, staticContent())
        .put(QuestionType.TEXT, textApplicantFavoriteColor())
        .build();
  }

  /** Returns a sample PHONE question. */
  public QuestionModel phoneApplicantPhone() {
    return questionCache.computeIfAbsent(
        QuestionEnum.PHONE_APPLICANT_PHONE, this::phoneApplicantPhone);
  }

  /** Returns a sample ADDRESS question. */
  public QuestionModel addressApplicantAddress() {
    return questionCache.computeIfAbsent(
        QuestionEnum.ADDRESS_APPLICANT_ADDRESS, this::addressApplicantAddress);
  }

  /** Returns a sample ADDRESS question for the applicant's secondary address. */
  public QuestionModel addressApplicantSecondaryAddress() {
    return questionCache.computeIfAbsent(
        QuestionEnum.ADDRESS_APPLICANT_SECONDARY_ADDRESS, this::addressAapplicantSecondaryAddress);
  }

  /** Returns a sample CHECKBOX question about kitchen tools the applicant owns. */
  public QuestionModel checkboxApplicantKitchenTools() {
    return questionCache.computeIfAbsent(
        QuestionEnum.CHECKBOX_APPLICANT_KITCHEN_TOOLS, this::checkboxApplicantKitchenTools);
  }

  /** Returns a sample DATE question for the applicant's birthday. */
  public QuestionModel dateApplicantBirthdate() {
    return questionCache.computeIfAbsent(
        QuestionEnum.DATE_APPLICANT_BIRTHDATE, this::DateApplicantBirthdate);
  }

  /** Returns a sample DROPDOWN question about the applicant's favorite ice cream flavor. */
  public QuestionModel dropdownApplicantIceCream() {
    return questionCache.computeIfAbsent(
        QuestionEnum.DROPDOWN_APPLICANT_ICE_CREAM, this::dropdownApplicantIceCream);
  }

  /** Returns a sample EMAIL question about the applicant's email. */
  public QuestionModel emailApplicantEmail() {
    return questionCache.computeIfAbsent(
        QuestionEnum.EMAIL_APPLICANT_EMAIL, this::emailApplicantEmail);
  }

  /**
   * Returns a sample ENUMERATOR question listing the applicant's household members.
   *
   * <p>This enumerator question is used as the parent of many sample repeated questions.
   */
  public QuestionModel enumeratorApplicantHouseholdMembers() {
    return questionCache.computeIfAbsent(
        QuestionEnum.ENUMERATOR_APPLICANT_HOUSEHOLD_MEMBERS,
        this::enumeratorApplicantHouseholdMembers);
  }

  /**
   * Returns a nested sample ENUMERATOR question listing the jobs each of the applicant's household
   * members holds.
   *
   * <p>This enumerator is nested under the household members enumerator, and is the parent of many
   * sample nested repeated questions.
   */
  public QuestionModel enumeratorNestedApplicantHouseholdMemberJobs() {
    return questionCache.computeIfAbsent(
        QuestionEnum.ENUMERATOR_NESTED_APPLICANT_HOUSEHOLD_MEMBER_JOBS,
        this::enumeratorNestedApplicantHouseholdMemberJobs);
  }

  /** Returns a sample FILE_UPLOAD question. */
  public QuestionModel fileUploadApplicantFile() {
    return questionCache.computeIfAbsent(
        QuestionEnum.FILE_UPLOAD_APPLICANT_FILE, this::fileUploadApplicantFile);
  }

  /** Returns a sample CURRENCY question about the applicant's monthly income. */
  public QuestionModel currencyApplicantMonthlyIncome() {
    return questionCache.computeIfAbsent(
        QuestionEnum.CURRENCY_APPLICANT_MONTHLY_INCOME, this::currencyApplicantMonthlyIncome);
  }

  /** Returns a sample ID question. */
  public QuestionModel IdApplicantId() {
    return questionCache.computeIfAbsent(QuestionEnum.ID_APPLICANT_ID, this::IdApplicantId);
  }

  /** Returns a sample NAME question. */
  public QuestionModel nameApplicantName() {
    return questionCache.computeIfAbsent(QuestionEnum.NAME_APPLICANT_NAME, this::nameApplicantName);
  }

  /** Returns a sample NULL question. */
  public QuestionModel nullQuestion() {
    return questionCache.computeIfAbsent(QuestionEnum.NULL_QUESTION, this::nullQuestion);
  }

  /** Returns a sample repeated NAME question about the applicant's household members' names. */
  public QuestionModel nameRepeatedApplicantHouseholdMemberName() {
    // Make sure the next call will have the question ready
    enumeratorApplicantHouseholdMembers();
    return questionCache.computeIfAbsent(
        QuestionEnum.NAME_REPEATED_APPLICANT_HOUSEHOLD_MEMBER_NAME,
        this::nameRepeatedApplicantHouseholdMemberName);
  }

  /**
   * Returns a sample repeated TEXT question about the applicant's household members' favorite
   * shapes.
   */
  public QuestionModel textRepeatedApplicantHouseholdMemberFavoriteShape() {
    // Make sure the next call will have the question ready
    enumeratorApplicantHouseholdMembers();
    return questionCache.computeIfAbsent(
        QuestionEnum.TEXT_REPEATED_APPLICANT_HOUSEHOLD_MEMBER_FAVORITE_SHAPE,
        this::textRepeatedApplicantHouseholdMemberFavoriteShape);
  }

  /** Returns a sample NUMBER question about the number of items the applicant can juggle. */
  public QuestionModel numberApplicantJugglingNumber() {
    return questionCache.computeIfAbsent(
        QuestionEnum.NUMBER_APPLICANT_JUGGLING_NUMBER, this::numberApplicantJugglingNumber);
  }

  /**
   * Returns a nested repeated sample NUMBER question about the number of days worked at each of the
   * jobs of each of the applicant's household numbers.
   */
  public QuestionModel numberNestedRepeatedApplicantHouseholdMemberDaysWorked() {
    return questionCache.computeIfAbsent(
        QuestionEnum.NUMBER_NESTED_REPEATED_APPLICANT_HOUSEHOLD_MEMBER_DAYS_WORKED,
        this::numberNestedRepeatedApplicantHouseholdMemberDaysWorked);
  }

  /** Returns a sample RADIO question about the applicant's favorite season. */
  public QuestionModel radioApplicantFavoriteSeason() {
    return questionCache.computeIfAbsent(
        QuestionEnum.RADIO_APPLICANT_FAVORITE_SEASON, this::radioApplicantFavoriteSeason);
  }

  /** Returns a sample TEXT question about the applicant's favorite color. */
  public QuestionModel textApplicantFavoriteColor() {
    return questionCache.computeIfAbsent(
        QuestionEnum.TEXT_APPLICANT_FAVORITE_COLOR, this::textApplicantFavoriteColor);
  }

  /** Returns a sample STATIC_CONTENT question */
  public QuestionModel staticContent() {
    return questionCache.computeIfAbsent(QuestionEnum.STATIC_CONTENT, this::staticContent);
  }

  // Address
  private QuestionModel addressApplicantAddress(QuestionEnum ignore) {
    QuestionDefinition definition =
        new AddressQuestionDefinition(
            QuestionDefinitionConfig.builder()
                .setName("applicant address")
                .setDescription("The address of applicant")
                .setQuestionText(LocalizedStrings.of(Locale.US, "What is your address?"))
                .setQuestionHelpText(LocalizedStrings.of(Locale.US, "This is sample help text."))
                .build());
    return maybeSave(definition);
  }

  private QuestionModel phoneApplicantPhone(QuestionEnum ignore) {
    QuestionDefinition definition =
        new PhoneQuestionDefinition(
            QuestionDefinitionConfig.builder()
                .setName("applicant phone")
                .setDescription("The applicant Phone Number")
                .setQuestionText(LocalizedStrings.of(Locale.US, "What is your phone number?"))
                .setQuestionHelpText(LocalizedStrings.of(Locale.US, "This is sample help text."))
                .build());
    return maybeSave(definition);
  }

  // Address
  private QuestionModel addressAapplicantSecondaryAddress(QuestionEnum ignore) {
    QuestionDefinition definition =
        new AddressQuestionDefinition(
            QuestionDefinitionConfig.builder()
                .setName("applicant secondary address")
                .setDescription("The secondary address of applicant")
                .setQuestionText(LocalizedStrings.of(Locale.US, "What is your secondary address?"))
                .setQuestionHelpText(LocalizedStrings.of(Locale.US, "This is sample help text."))
                .build());
    return maybeSave(definition);
  }

  // Checkbox
  private QuestionModel checkboxApplicantKitchenTools(QuestionEnum ignore) {
    QuestionDefinitionConfig config =
        QuestionDefinitionConfig.builder()
            .setName("kitchen tools")
            .setDescription("Kitchen instruments you own")
            .setQuestionText(
                LocalizedStrings.of(
                    Locale.US, "Which of the following kitchen instruments do you own?"))
            .setQuestionHelpText(LocalizedStrings.of(Locale.US, "This is sample help text."))
            .build();
    ImmutableList<QuestionOption> questionOptions =
        ImmutableList.of(
            QuestionOption.create(1L, 1L, "toaster", LocalizedStrings.of(Locale.US, "Toaster")),
            QuestionOption.create(
                2L, 2L, "pepper_grinder", LocalizedStrings.of(Locale.US, "Pepper Grinder")),
            QuestionOption.create(
                3L, 3L, "garlic_press", LocalizedStrings.of(Locale.US, "Garlic Press")));
    QuestionDefinition definition =
        new MultiOptionQuestionDefinition(
            config, questionOptions, MultiOptionQuestionType.CHECKBOX);
    return maybeSave(definition);
  }

  // Dropdown
  private QuestionModel dropdownApplicantIceCream(QuestionEnum ignore) {
    QuestionDefinitionConfig config =
        QuestionDefinitionConfig.builder()
            .setName("applicant ice cream")
            .setDescription("Select your favorite ice cream flavor")
            .setQuestionText(
                LocalizedStrings.of(
                    Locale.US, "Select your favorite ice cream flavor from the following"))
            .setQuestionHelpText(LocalizedStrings.of(Locale.US, "This is sample help text."))
            .build();
    ImmutableList<QuestionOption> questionOptions =
        ImmutableList.of(
            QuestionOption.create(1L, 1L, "chocolate", LocalizedStrings.of(Locale.US, "Chocolate")),
            QuestionOption.create(
                2L, 2L, "strawberry", LocalizedStrings.of(Locale.US, "Strawberry")),
            QuestionOption.create(3L, 3L, "vanilla", LocalizedStrings.of(Locale.US, "Vanilla")),
            QuestionOption.create(4L, 4L, "coffee", LocalizedStrings.of(Locale.US, "Coffee")));
    QuestionDefinition definition =
        new MultiOptionQuestionDefinition(
            config, questionOptions, MultiOptionQuestionType.DROPDOWN);
    return maybeSave(definition);
  }

  // Enumerator
  private QuestionModel enumeratorApplicantHouseholdMembers(QuestionEnum ignore) {
    QuestionDefinition definition =
        new EnumeratorQuestionDefinition(
            QuestionDefinitionConfig.builder()
                .setName("applicant household members")
                .setDescription("The applicant's household members")
                .setQuestionText(LocalizedStrings.of(Locale.US, "Who are your household members?"))
                .setQuestionHelpText(LocalizedStrings.of(Locale.US, "This is sample help text."))
                .build(),
            LocalizedStrings.empty());
    return maybeSave(definition);
  }

  // Nested Enumerator
  private QuestionModel enumeratorNestedApplicantHouseholdMemberJobs(QuestionEnum ignore) {
    QuestionModel householdMembers = enumeratorApplicantHouseholdMembers();
    QuestionDefinition definition =
        new EnumeratorQuestionDefinition(
            QuestionDefinitionConfig.builder()
                .setName("household members jobs")
                .setDescription("The applicant's household member's jobs")
                .setQuestionText(LocalizedStrings.of(Locale.US, "What are the $this's jobs?"))
                .setQuestionHelpText(LocalizedStrings.of(Locale.US, "Where does $this work?"))
                .setEnumeratorId(Optional.of(householdMembers.id))
                .build(),
            LocalizedStrings.empty());
    return maybeSave(definition);
  }

  // File upload
  private QuestionModel fileUploadApplicantFile(QuestionEnum ignore) {
    QuestionDefinition definition =
        new FileUploadQuestionDefinition(
            QuestionDefinitionConfig.builder()
                .setName("applicant file")
                .setDescription("The file to be uploaded")
                .setQuestionText(
                    LocalizedStrings.of(Locale.US, "What is the file you want to upload?"))
                .setQuestionHelpText(LocalizedStrings.of(Locale.US, "This is sample help text."))
                .build());
    return maybeSave(definition);
  }

  // Currency
  private QuestionModel currencyApplicantMonthlyIncome(QuestionEnum ignore) {
    QuestionDefinition definition =
        new CurrencyQuestionDefinition(
            QuestionDefinitionConfig.builder()
                .setName("applicant monthly income")
                .setDescription("monthly income of applicant")
                .setQuestionText(LocalizedStrings.of(Locale.US, "what is your monthly income?"))
                .setQuestionHelpText(LocalizedStrings.of(Locale.US, "help text"))
                .build());
    return maybeSave(definition);
  }

  // Id
  private QuestionModel IdApplicantId(QuestionEnum ignore) {
    QuestionDefinition definition =
        new IdQuestionDefinition(
            QuestionDefinitionConfig.builder()
                .setName("applicant id")
                .setDescription("1234")
                .setQuestionText(LocalizedStrings.of(Locale.US, "What is the the id?"))
                .setQuestionHelpText(LocalizedStrings.of(Locale.US, "This is sample help text."))
                .build());
    return maybeSave(definition);
  }

  // Name
  private QuestionModel nameApplicantName(QuestionEnum ignore) {
    QuestionDefinition definition =
        new NameQuestionDefinition(
            QuestionDefinitionConfig.builder()
                .setName("applicant name")
                .setDescription("name of applicant")
                .setQuestionText(LocalizedStrings.of(Locale.US, "what is your name?"))
                .setQuestionHelpText(LocalizedStrings.of(Locale.US, "help text"))
                .build());
    return maybeSave(definition);
  }

  private QuestionModel nullQuestion(QuestionEnum ignore) {
    QuestionDefinition definition = new NullQuestionDefinition(9999L);
    return new QuestionModel(definition);
  }

  // Repeated name
  private QuestionModel nameRepeatedApplicantHouseholdMemberName(QuestionEnum ignore) {
    QuestionModel householdMembers = enumeratorApplicantHouseholdMembers();
    QuestionDefinition definition =
        new NameQuestionDefinition(
            QuestionDefinitionConfig.builder()
                .setName("household members name")
                .setDescription("The applicant's household member's name")
                .setQuestionText(LocalizedStrings.of(Locale.US, "What is the $this's name?"))
                .setQuestionHelpText(
                    LocalizedStrings.of(Locale.US, "Please provide full name for $this."))
                .setEnumeratorId(Optional.of(householdMembers.id))
                .build());

    return maybeSave(definition);
  }

  // Repeated text
  private QuestionModel textRepeatedApplicantHouseholdMemberFavoriteShape(QuestionEnum ignore) {
    QuestionModel householdMembers = enumeratorApplicantHouseholdMembers();
    QuestionDefinition definition =
        new TextQuestionDefinition(
            QuestionDefinitionConfig.builder()
                .setName("household member favorite shape")
                .setDescription("The applicant household member's favorite shape")
                .setQuestionText(LocalizedStrings.of(Locale.US, "What is $this's favorite shape?"))
                .setQuestionHelpText(LocalizedStrings.of(Locale.US, "This is sample help text."))
                .setEnumeratorId(Optional.of(householdMembers.id))
                .build());
    return maybeSave(definition);
  }

  // Number
  private QuestionModel numberApplicantJugglingNumber(QuestionEnum ignore) {
    QuestionDefinition definition =
        new NumberQuestionDefinition(
            QuestionDefinitionConfig.builder()
                .setName("number of items applicant can juggle")
                .setDescription("The number of items applicant can juggle at once")
                .setQuestionText(
                    LocalizedStrings.of(Locale.US, "How many items can you juggle at one time?"))
                .setQuestionHelpText(LocalizedStrings.of(Locale.US, "This is sample help text."))
                .build());
    return maybeSave(definition);
  }

  // Date
  private QuestionModel DateApplicantBirthdate(QuestionEnum ignore) {
    QuestionDefinition definition =
        new DateQuestionDefinition(
            QuestionDefinitionConfig.builder()
                .setName("applicant birth date")
                .setDescription("The applicant birth date")
                .setQuestionText(LocalizedStrings.of(Locale.US, "What is your birthdate?"))
                .setQuestionHelpText(LocalizedStrings.of(Locale.US, "This is sample help text."))
                .build());
    return maybeSave(definition);
  }

  // Email
  private QuestionModel emailApplicantEmail(QuestionEnum ignore) {
    QuestionDefinition definition =
        new EmailQuestionDefinition(
            QuestionDefinitionConfig.builder()
                .setName("applicant Email address")
                .setDescription("The applicant Email address")
                .setQuestionText(LocalizedStrings.of(Locale.US, "What is your Email?"))
                .setQuestionHelpText(LocalizedStrings.of(Locale.US, "This is sample help text."))
                .build());
    return maybeSave(definition);
  }

  // Deeply Nested Number
  private QuestionModel numberNestedRepeatedApplicantHouseholdMemberDaysWorked(
      QuestionEnum ignore) {
    QuestionModel householdMemberJobs = enumeratorNestedApplicantHouseholdMemberJobs();
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
                .setEnumeratorId(Optional.of(householdMemberJobs.id))
                .build());

    return maybeSave(definition);
  }

  // Radio button
  private QuestionModel radioApplicantFavoriteSeason(QuestionEnum ignore) {
    QuestionDefinitionConfig config =
        QuestionDefinitionConfig.builder()
            .setName("applicant favorite season")
            .setDescription("Favorite season in the year")
            .setQuestionText(LocalizedStrings.of(Locale.US, "What is your favorite season?"))
            .setQuestionHelpText(LocalizedStrings.of(Locale.US, "This is sample help text."))
            .build();
    ImmutableList<QuestionOption> questionOptions =
        ImmutableList.of(
            QuestionOption.create(1L, 1L, "winter", LocalizedStrings.of(Locale.US, "Winter")),
            QuestionOption.create(2L, 2L, "spring", LocalizedStrings.of(Locale.US, "Spring")),
            QuestionOption.create(3L, 3L, "summer", LocalizedStrings.of(Locale.US, "Summer")),
            QuestionOption.create(4L, 4L, "fall", LocalizedStrings.of(Locale.US, "Fall")));
    QuestionDefinition definition =
        new MultiOptionQuestionDefinition(
            config, questionOptions, MultiOptionQuestionType.RADIO_BUTTON);
    return maybeSave(definition);
  }

  // Static
  private QuestionModel staticContent(QuestionEnum ignore) {
    QuestionDefinition definition =
        new StaticContentQuestionDefinition(
            QuestionDefinitionConfig.builder()
                .setName("more info about something")
                .setDescription("Shows more info to the applicant")
                .setQuestionText(LocalizedStrings.of(Locale.US, "This is more info"))
                .setQuestionHelpText(LocalizedStrings.of(Locale.US, ""))
                .build());
    return maybeSave(definition);
  }

  // Text
  private QuestionModel textApplicantFavoriteColor(QuestionEnum ignore) {
    QuestionDefinition definition =
        new TextQuestionDefinition(
            QuestionDefinitionConfig.builder()
                .setName("applicant favorite color")
                .setDescription("Favorite color of applicant")
                .setQuestionText(LocalizedStrings.of(Locale.US, "What is your favorite color?"))
                .setQuestionHelpText(LocalizedStrings.of(Locale.US, "This is sample help text."))
                .build());
    return maybeSave(definition);
  }

  private QuestionModel maybeSave(QuestionDefinition questionDefinition) {
    return maybeSave(questionDefinition, LifecycleStage.ACTIVE);
  }

  public QuestionModel maybeSave(
      QuestionDefinition questionDefinition, LifecycleStage desiredStage) {
    QuestionModel question = new QuestionModel(questionDefinition);
    if (canSave) {
      // This odd way of finding the active version is because this class
      // doesn't have access to the Version repository, because it needs to
      // work without the database.
      VersionModel versionToAdd;
      Optional<VersionModel> existingVersion =
          question
              .db()
              .find(VersionModel.class)
              .where()
              .eq("lifecycle_stage", desiredStage)
              .findOneOrEmpty();
      if (existingVersion.isEmpty()) {
        versionToAdd = new VersionModel(desiredStage);
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
            "Questions in the TestQuestionBank better be supported QuestionTypes. "
                + e.getLocalizedMessage());
      }
    }
    return question;
  }

  private enum QuestionEnum {
    ADDRESS_APPLICANT_ADDRESS,
    ADDRESS_APPLICANT_SECONDARY_ADDRESS,
    CHECKBOX_APPLICANT_KITCHEN_TOOLS,
    CURRENCY_APPLICANT_MONTHLY_INCOME,
    DATE_APPLICANT_BIRTHDATE,
    DROPDOWN_APPLICANT_ICE_CREAM,
    EMAIL_APPLICANT_EMAIL,
    ENUMERATOR_APPLICANT_HOUSEHOLD_MEMBERS,
    ENUMERATOR_NESTED_APPLICANT_HOUSEHOLD_MEMBER_JOBS,
    FILE_UPLOAD_APPLICANT_FILE,
    ID_APPLICANT_ID,
    NAME_APPLICANT_NAME,
    NAME_REPEATED_APPLICANT_HOUSEHOLD_MEMBER_NAME,
    NULL_QUESTION,
    NUMBER_APPLICANT_JUGGLING_NUMBER,
    NUMBER_NESTED_REPEATED_APPLICANT_HOUSEHOLD_MEMBER_DAYS_WORKED,
    PHONE_APPLICANT_PHONE,
    RADIO_APPLICANT_FAVORITE_SEASON,
    STATIC_CONTENT,
    TEXT_APPLICANT_FAVORITE_COLOR,
    TEXT_REPEATED_APPLICANT_HOUSEHOLD_MEMBER_FAVORITE_SHAPE
  }
}
