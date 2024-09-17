package services.export;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import java.time.Instant;
import java.util.Comparator;
import java.util.Locale;
import java.util.Optional;
import junitparams.converters.Nullable;
import models.AccountModel;
import models.ApplicantModel;
import models.ApplicationModel;
import models.LifecycleStage;
import models.ProgramModel;
import models.QuestionModel;
import org.junit.Before;
import repository.ApplicationStatusesRepository;
import repository.ResetPostgres;
import services.LocalizedStrings;
import services.Path;
import services.applicant.ApplicantData;
import services.applicant.RepeatedEntity;
import services.applicant.question.Scalar;
import services.application.ApplicationEventDetails;
import services.application.ApplicationEventDetails.StatusEvent;
import services.applications.ProgramAdminApplicationService;
import services.geo.ServiceAreaInclusion;
import services.program.EligibilityDefinition;
import services.program.IllegalPredicateOrderingException;
import services.program.ProgramDefinition;
import services.program.ProgramNeedsABlockException;
import services.program.ProgramNotFoundException;
import services.program.ProgramService;
import services.program.predicate.LeafOperationExpressionNode;
import services.program.predicate.Operator;
import services.program.predicate.PredicateAction;
import services.program.predicate.PredicateDefinition;
import services.program.predicate.PredicateExpressionNode;
import services.program.predicate.PredicateValue;
import services.question.QuestionAnswerer;
import services.question.types.EnumeratorQuestionDefinition;
import services.question.types.QuestionDefinition;
import services.question.types.QuestionType;
import services.statuses.StatusDefinitions;
import services.statuses.StatusDefinitions.Status;
import support.CfTestHelpers;
import support.ProgramBuilder;

/**
 * Superclass for tests that exercise exporters. Helps with generating programs, questions, and
 * applications.
 */
public abstract class AbstractExporterTest extends ResetPostgres {
  public static final String STATUS_VALUE = "approved";

  // Instant in UTC, the month is chosen so that the create time translates to a PDT time
  // and the creation time to a PST time to test both cases.
  public static final Instant FAKE_CREATE_TIME = Instant.parse("2022-04-09T10:07:02.00Z");
  public static final Instant FAKE_SUBMIT_TIME = Instant.parse("2022-12-09T10:30:30.00Z");

  protected ProgramAdminApplicationService programAdminApplicationService;
  private static ProgramService programService;

  protected ProgramModel fakeProgramWithEnumerator;
  protected ProgramModel fakeProgramWithVisibility;
  protected ProgramModel fakeProgramWithEligibility;
  protected ProgramModel fakeProgram;
  protected ImmutableList<QuestionModel> fakeQuestions;
  protected ApplicantModel applicantOne;
  protected ApplicantModel applicantFive;
  protected ApplicantModel applicantTwo;
  protected ApplicantModel applicantSeven;
  protected ApplicationModel applicationOne;
  protected ApplicationModel applicationTwo;
  protected ApplicationModel applicationThree;
  protected ApplicationModel applicationFour;
  protected ApplicationModel applicationFive;
  protected ApplicationModel applicationSeven;
  private static ApplicationStatusesRepository appStatusRepo;

  @Before
  public void setup() {
    programAdminApplicationService = instanceOf(ProgramAdminApplicationService.class);
    programService = instanceOf(ProgramService.class);
    appStatusRepo = instanceOf(ApplicationStatusesRepository.class);
  }

  protected void answerQuestion(
      QuestionType questionType,
      QuestionModel question,
      ApplicantData applicantDataOne,
      ApplicantData applicantDataTwo) {
    Path answerPath =
        question
            .getQuestionDefinition()
            .getContextualizedPath(Optional.empty(), ApplicantData.APPLICANT_PATH);
    switch (questionType) {
      case ADDRESS:
        QuestionAnswerer.answerAddressQuestion(
            applicantDataOne, answerPath, "street st", "apt 100", "city", "AB", "54321");
        // applicant two did not answer this question.
        break;
      case CHECKBOX:
        QuestionAnswerer.answerMultiSelectQuestion(applicantDataOne, answerPath, 0, 1L);
        QuestionAnswerer.answerMultiSelectQuestion(applicantDataOne, answerPath, 1, 2L);
        // applicant two did not answer this question.
        break;
      case CURRENCY:
        QuestionAnswerer.answerCurrencyQuestion(applicantDataOne, answerPath, "1,234.56");
        break;
      case DATE:
        QuestionAnswerer.answerDateQuestion(applicantDataOne, answerPath, "1980-01-01");
        // applicant two did not answer this question.
        break;
      case DROPDOWN:
        QuestionAnswerer.answerSingleSelectQuestion(applicantDataOne, answerPath, 2L);
        // applicant two did not answer this question.
        break;
      case EMAIL:
        QuestionAnswerer.answerEmailQuestion(applicantDataOne, answerPath, "one@example.com");
        // applicant two did not answer this question.
        break;
      case FILEUPLOAD:
        QuestionAnswerer.answerFileQuestion(applicantDataOne, answerPath, "my-file-key");
        // applicant two did not answer this question.
        break;
      case ID:
        QuestionAnswerer.answerIdQuestion(applicantDataOne, answerPath, "012");
        QuestionAnswerer.answerIdQuestion(applicantDataTwo, answerPath, "123");
        break;
      case NAME:
        QuestionAnswerer.answerNameQuestion(
            applicantDataOne, answerPath, "Alice", "", "Appleton", "");
        QuestionAnswerer.answerNameQuestion(applicantDataTwo, answerPath, "Bob", "", "Baker", "");
        break;
      case NUMBER:
        QuestionAnswerer.answerNumberQuestion(applicantDataOne, answerPath, "123456");
        // applicant two did not answer this question.
        break;
      case RADIO_BUTTON:
        QuestionAnswerer.answerSingleSelectQuestion(applicantDataOne, answerPath, 1L);
        // applicant two did not answer this question.
        break;
      case ENUMERATOR:
        QuestionAnswerer.answerEnumeratorQuestion(
            applicantDataOne, answerPath, ImmutableList.of("item1", "item2"));
        // applicant two did not answer this question.
        break;
      case TEXT:
        QuestionAnswerer.answerTextQuestion(
            applicantDataOne, answerPath, "Some Value \" containing ,,, special characters");
        // applicant two did not answer this question.
        break;
      case PHONE:
        QuestionAnswerer.answerPhoneQuestion(applicantDataOne, answerPath, "US", "(615) 757-1010");
        break;
      case STATIC:
        // Do nothing.
        break;
      case NULL_QUESTION:
        // Do nothing.
    }
  }

  /**
   * Setup application 1-4.
   *
   * <p>1-3 have the same user with each of the three possible states, each with Status approved. 4
   * is a different user in Active state.
   */
  protected void createFakeApplications() throws Exception {
    AccountModel admin = resourceCreator.insertAccount();
    ApplicantModel applicantOne = resourceCreator.insertApplicantWithAccount();
    ApplicantModel applicantTwo = resourceCreator.insertApplicantWithAccount();
    testQuestionBank.getSampleQuestionsForAllTypes().entrySet().stream()
        .forEach(
            entry ->
                answerQuestion(
                    entry.getKey(),
                    entry.getValue(),
                    applicantOne.getApplicantData(),
                    applicantTwo.getApplicantData()));
    applicantOne.save();
    applicantTwo.save();

    applicationOne =
        createFakeApplication(
            applicantOne, admin, fakeProgram, LifecycleStage.ACTIVE, STATUS_VALUE, "Test note");
    applicationTwo =
        createFakeApplication(
            applicantOne, admin, fakeProgram, LifecycleStage.OBSOLETE, STATUS_VALUE, "admin_note");
    applicationThree =
        createFakeApplication(
            applicantOne,
            admin,
            fakeProgram,
            LifecycleStage.DRAFT,
            STATUS_VALUE,
            "test_application");
    applicationFour =
        createFakeApplication(applicantTwo, null, fakeProgram, LifecycleStage.ACTIVE, null, null);
  }

  private ApplicationModel createFakeApplication(
      ApplicantModel applicant,
      @Nullable AccountModel admin,
      ProgramModel program,
      LifecycleStage lifecycleStage,
      @Nullable String status,
      @Nullable String note)
      throws Exception {
    ApplicationModel application = new ApplicationModel(applicant, program, lifecycleStage);
    application.setApplicantData(applicant.getApplicantData());
    application.save();

    // CreateTime of an application is set through @onCreate to Instant.now(). To change
    // the value, manually set createTime and save and refresh the application.
    application.setCreateTimeForTest(FAKE_CREATE_TIME);
    application.setSubmitTimeForTest(FAKE_SUBMIT_TIME);
    application.save();

    if (status != null && admin != null) {
      programAdminApplicationService.setStatus(
          application,
          StatusEvent.builder().setEmailSent(false).setStatusText(STATUS_VALUE).build(),
          admin);
    }
    if (note != null && admin != null) {
      programAdminApplicationService.setNote(
          application, ApplicationEventDetails.NoteEvent.create(note), admin);
    }
    application.refresh();
    return application;
  }

  protected void createFakeQuestions() {
    this.fakeQuestions =
        testQuestionBank.getSampleQuestionsForAllTypes().values().stream()
            .sorted(Comparator.comparing(question -> question.getQuestionDefinition().getName()))
            .collect(ImmutableList.toImmutableList());
  }

  protected ImmutableList<QuestionDefinition> getFakeQuestionDefinitions() {
    return fakeQuestions.stream()
        .map(QuestionModel::getQuestionDefinition)
        .collect(ImmutableList.toImmutableList());
  }

  protected void createFakeProgram() {
    ProgramBuilder fakeProgram = ProgramBuilder.newActiveProgram("Fake Program", "Test program");
    for (int i = 0; i < fakeQuestions.size(); i++) {
      int screenNumber = i + 1;
      fakeProgram
          .withBlock("Screen " + screenNumber, "description for screen " + screenNumber)
          .withRequiredQuestion(fakeQuestions.get(i))
          .build();
    }
    this.fakeProgram = fakeProgram.build();
    appStatusRepo.createOrUpdateStatusDefinitions(
        this.fakeProgram.getProgramDefinition().adminName(),
        new StatusDefinitions()
            .setStatuses(
                ImmutableList.of(
                    Status.builder()
                        .setStatusText(STATUS_VALUE)
                        .setLocalizedStatusText(
                            LocalizedStrings.builder()
                                .setTranslations(ImmutableMap.of(Locale.ENGLISH, STATUS_VALUE))
                                .build())
                        .build())));
  }

  protected void createFakeProgramWithOptionalQuestion(QuestionModel optionalQuestion) {
    QuestionModel nameQuestion = testQuestionBank.nameApplicantName();

    ProgramModel fakeProgramWithOptionalQuestion =
        ProgramBuilder.newActiveProgram()
            .withName("Fake Optional Question Program")
            .withBlock()
            .withRequiredQuestion(nameQuestion)
            .withBlock()
            .withOptionalQuestion(optionalQuestion)
            .build();

    // Applicant five have file uploaded for the optional file upload question
    applicantFive = resourceCreator.insertApplicantWithAccount();
    applicantFive.getApplicantData().setUserName("Example Five");
    QuestionAnswerer.answerNameQuestion(
        applicantFive.getApplicantData(),
        ApplicantData.APPLICANT_PATH.join(
            nameQuestion.getQuestionDefinition().getQuestionPathSegment()),
        "Example",
        "",
        "Five",
        "");

    applicationFive =
        new ApplicationModel(applicantFive, fakeProgramWithOptionalQuestion, LifecycleStage.ACTIVE);
    applicantFive.save();
    CfTestHelpers.withMockedInstantNow(
        "2022-01-01T00:00:00Z", () -> applicationFive.setSubmitTimeToNow());
    applicationFive.setApplicantData(applicantFive.getApplicantData());
    applicationFive.save();
  }

  /**
   * Creates a program that has an visibility predicate, one applicant, and one application. The
   * applications have submission times one month apart starting on 2023-01-01.
   */
  protected void createFakeProgramWithVisibilityPredicate() {
    QuestionModel nameQuestion = testQuestionBank.nameApplicantName();
    QuestionModel colorQuestion = testQuestionBank.textApplicantFavoriteColor();

    PredicateDefinition colorPredicate =
        PredicateDefinition.create(
            PredicateExpressionNode.create(
                LeafOperationExpressionNode.create(
                    colorQuestion.id, Scalar.TEXT, Operator.EQUAL_TO, PredicateValue.of("red"))),
            PredicateAction.HIDE_BLOCK);

    fakeProgramWithVisibility =
        ProgramBuilder.newActiveProgram()
            .withName("Fake Program")
            .withBlock("Screen 1")
            .withRequiredQuestion(colorQuestion)
            .withBlock("Screen 2")
            .withRequiredQuestion(nameQuestion)
            .withVisibilityPredicate(colorPredicate)
            .build();

    applicantSeven = resourceCreator.insertApplicantWithAccount();
    QuestionAnswerer.answerNameQuestion(
        applicantSeven.getApplicantData(),
        ApplicantData.APPLICANT_PATH.join(
            nameQuestion.getQuestionDefinition().getQuestionPathSegment()),
        "Jen",
        "",
        "Doe",
        "");
    QuestionAnswerer.answerTextQuestion(
        applicantSeven.getApplicantData(),
        ApplicantData.APPLICANT_PATH.join(
            colorQuestion.getQuestionDefinition().getQuestionPathSegment()),
        "red");
    applicantSeven.save();
    applicationSeven =
        new ApplicationModel(applicantSeven, fakeProgramWithVisibility, LifecycleStage.ACTIVE);
    applicationSeven.setApplicantData(applicantSeven.getApplicantData());
    CfTestHelpers.withMockedInstantNow(
        "2023-01-01T00:00:00Z", () -> applicationSeven.setSubmitTimeToNow());
    applicationSeven.save();
  }

  /**
   * Creates a program that has an eligibility predicate, three applicants, and three applications.
   * The applications have submission times one month apart starting on 2022-01-01.
   */
  protected void createFakeProgramWithEligibilityPredicate() {
    QuestionModel nameQuestion = testQuestionBank.nameApplicantName();
    QuestionModel colorQuestion = testQuestionBank.textApplicantFavoriteColor();

    PredicateDefinition colorPredicate =
        PredicateDefinition.create(
            PredicateExpressionNode.create(
                LeafOperationExpressionNode.create(
                    colorQuestion.id, Scalar.TEXT, Operator.EQUAL_TO, PredicateValue.of("blue"))),
            PredicateAction.ELIGIBLE_BLOCK);
    EligibilityDefinition colorEligibilityDefinition =
        EligibilityDefinition.builder().setPredicate(colorPredicate).build();

    fakeProgramWithEligibility =
        ProgramBuilder.newActiveProgram()
            .withName("Fake Program With Enumerator")
            .withBlock("Screen 1")
            .withRequiredQuestions(nameQuestion, colorQuestion)
            .withEligibilityDefinition(colorEligibilityDefinition)
            .build();

    // First applicant is not eligible.
    applicantOne = resourceCreator.insertApplicantWithAccount();
    QuestionAnswerer.answerNameQuestion(
        applicantOne.getApplicantData(),
        ApplicantData.APPLICANT_PATH.join(
            nameQuestion.getQuestionDefinition().getQuestionPathSegment()),
        "Jane",
        "",
        "Doe",
        "");
    QuestionAnswerer.answerTextQuestion(
        applicantOne.getApplicantData(),
        ApplicantData.APPLICANT_PATH.join(
            colorQuestion.getQuestionDefinition().getQuestionPathSegment()),
        "coquelicot");
    applicantOne.save();
    applicationOne =
        new ApplicationModel(applicantOne, fakeProgramWithEligibility, LifecycleStage.ACTIVE);
    applicationOne.setApplicantData(applicantOne.getApplicantData());

    CfTestHelpers.withMockedInstantNow(
        "2022-01-01T00:00:00Z", () -> applicationOne.setSubmitTimeToNow());
    applicationOne.save();

    // Second applicant is eligible.
    applicantTwo = resourceCreator.insertApplicantWithAccount();
    QuestionAnswerer.answerNameQuestion(
        applicantTwo.getApplicantData(),
        ApplicantData.APPLICANT_PATH.join(
            nameQuestion.getQuestionDefinition().getQuestionPathSegment()),
        "John",
        "",
        "Doe",
        "");
    QuestionAnswerer.answerTextQuestion(
        applicantTwo.getApplicantData(),
        ApplicantData.APPLICANT_PATH.join(
            colorQuestion.getQuestionDefinition().getQuestionPathSegment()),
        "blue");
    applicantTwo.save();
    applicationTwo =
        new ApplicationModel(applicantTwo, fakeProgramWithEligibility, LifecycleStage.ACTIVE);
    applicationTwo.setApplicantData(applicantTwo.getApplicantData());
    CfTestHelpers.withMockedInstantNow(
        "2022-02-01T00:00:00Z", () -> applicationTwo.setSubmitTimeToNow());
    applicationTwo.save();

    applicationThree =
        new ApplicationModel(applicantTwo, fakeProgramWithEligibility, LifecycleStage.OBSOLETE);
    applicationThree.setApplicantData(applicantTwo.getApplicantData());
    CfTestHelpers.withMockedInstantNow(
        "2022-03-01T00:00:00Z", () -> applicationThree.setSubmitTimeToNow());
    applicationThree.save();
  }

  /**
   * Creates a program that has an enumerator question with children, three applicants, and three
   * applications. The applications have submission times one month apart starting on 2022-01-01.
   */
  protected void createFakeProgramWithEnumeratorAndAnswerQuestions() {
    QuestionModel nameQuestion = testQuestionBank.nameApplicantName();
    QuestionModel colorQuestion = testQuestionBank.textApplicantFavoriteColor();
    QuestionModel monthlyIncomeQuestion = testQuestionBank.currencyApplicantMonthlyIncome();
    QuestionModel householdMembersQuestion = testQuestionBank.enumeratorApplicantHouseholdMembers();
    QuestionModel hmNameQuestion = testQuestionBank.nameRepeatedApplicantHouseholdMemberName();
    QuestionModel hmJobsQuestion = testQuestionBank.enumeratorNestedApplicantHouseholdMemberJobs();
    QuestionModel hmNumberDaysWorksQuestion =
        testQuestionBank.numberNestedRepeatedApplicantHouseholdMemberDaysWorked();
    fakeProgramWithEnumerator =
        ProgramBuilder.newActiveProgram()
            .withName("Fake Program With Enumerator")
            .withBlock()
            .withRequiredQuestions(nameQuestion, colorQuestion, monthlyIncomeQuestion)
            .withBlock()
            .withRequiredQuestion(householdMembersQuestion)
            .withRepeatedBlock()
            .withRequiredQuestion(hmNameQuestion)
            .withAnotherRepeatedBlock()
            .withRequiredQuestion(hmJobsQuestion)
            .withRepeatedBlock()
            .withRequiredQuestion(hmNumberDaysWorksQuestion)
            .build();

    // First applicant has two household members, and the second one has one job.
    applicantOne = resourceCreator.insertApplicantWithAccount();
    QuestionAnswerer.answerNameQuestion(
        applicantOne.getApplicantData(),
        ApplicantData.APPLICANT_PATH.join(
            nameQuestion.getQuestionDefinition().getQuestionPathSegment()),
        "Jane",
        "",
        "Doe",
        "");
    QuestionAnswerer.answerTextQuestion(
        applicantOne.getApplicantData(),
        ApplicantData.APPLICANT_PATH.join(
            colorQuestion.getQuestionDefinition().getQuestionPathSegment()),
        "coquelicot");
    Path hmPath =
        ApplicantData.APPLICANT_PATH.join(
            householdMembersQuestion.getQuestionDefinition().getQuestionPathSegment());
    QuestionAnswerer.answerEnumeratorQuestion(
        applicantOne.getApplicantData(), hmPath, ImmutableList.of("Anne", "Bailey"));
    QuestionAnswerer.answerNameQuestion(
        applicantOne.getApplicantData(),
        hmPath.atIndex(0).join(hmNameQuestion.getQuestionDefinition().getQuestionPathSegment()),
        "Anne",
        "",
        "Anderson",
        "");
    QuestionAnswerer.answerNameQuestion(
        applicantOne.getApplicantData(),
        hmPath.atIndex(1).join(hmNameQuestion.getQuestionDefinition().getQuestionPathSegment()),
        "Bailey",
        "",
        "Bailerson",
        "");
    String hmJobPathSegment = hmJobsQuestion.getQuestionDefinition().getQuestionPathSegment();
    QuestionAnswerer.answerEnumeratorQuestion(
        applicantOne.getApplicantData(),
        hmPath.atIndex(1).join(hmJobPathSegment),
        ImmutableList.of("Bailey's job"));
    QuestionAnswerer.answerNumberQuestion(
        applicantOne.getApplicantData(),
        hmPath
            .atIndex(1)
            .join(hmJobPathSegment)
            .atIndex(0)
            .join(hmNumberDaysWorksQuestion.getQuestionDefinition().getQuestionPathSegment()),
        100);
    applicantOne.save();
    applicationOne =
        new ApplicationModel(applicantOne, fakeProgramWithEnumerator, LifecycleStage.ACTIVE);
    applicationOne.setApplicantData(applicantOne.getApplicantData());

    CfTestHelpers.withMockedInstantNow(
        "2022-01-01T00:00:00Z", () -> applicationOne.setSubmitTimeToNow());
    applicationOne.save();

    // Second applicant has one household member that has two jobs.
    applicantTwo = resourceCreator.insertApplicantWithAccount();
    QuestionAnswerer.answerNameQuestion(
        applicantTwo.getApplicantData(),
        ApplicantData.APPLICANT_PATH.join(
            nameQuestion.getQuestionDefinition().getQuestionPathSegment()),
        "John",
        "",
        "Doe",
        "");
    QuestionAnswerer.answerTextQuestion(
        applicantTwo.getApplicantData(),
        ApplicantData.APPLICANT_PATH.join(
            colorQuestion.getQuestionDefinition().getQuestionPathSegment()),
        "brown");
    QuestionAnswerer.answerEnumeratorQuestion(
        applicantTwo.getApplicantData(), hmPath, ImmutableList.of("James"));
    QuestionAnswerer.answerNameQuestion(
        applicantTwo.getApplicantData(),
        hmPath.atIndex(0).join(hmNameQuestion.getQuestionDefinition().getQuestionPathSegment()),
        "James",
        "",
        "Jameson",
        "");
    QuestionAnswerer.answerEnumeratorQuestion(
        applicantTwo.getApplicantData(),
        hmPath.atIndex(0).join(hmJobPathSegment),
        ImmutableList.of("James' first job", "James' second job", "James' third job"));
    QuestionAnswerer.answerNumberQuestion(
        applicantTwo.getApplicantData(),
        hmPath
            .atIndex(0)
            .join(hmJobPathSegment)
            .atIndex(0)
            .join(hmNumberDaysWorksQuestion.getQuestionDefinition().getQuestionPathSegment()),
        111);
    QuestionAnswerer.answerNumberQuestion(
        applicantTwo.getApplicantData(),
        hmPath
            .atIndex(0)
            .join(hmJobPathSegment)
            .atIndex(1)
            .join(hmNumberDaysWorksQuestion.getQuestionDefinition().getQuestionPathSegment()),
        222);
    QuestionAnswerer.answerNumberQuestion(
        applicantTwo.getApplicantData(),
        hmPath
            .atIndex(0)
            .join(hmJobPathSegment)
            .atIndex(2)
            .join(hmNumberDaysWorksQuestion.getQuestionDefinition().getQuestionPathSegment()),
        333);
    applicantTwo.save();
    applicationTwo =
        new ApplicationModel(applicantTwo, fakeProgramWithEnumerator, LifecycleStage.ACTIVE);
    applicationTwo.setApplicantData(applicantTwo.getApplicantData());
    CfTestHelpers.withMockedInstantNow(
        "2022-02-01T00:00:00Z", () -> applicationTwo.setSubmitTimeToNow());
    applicationTwo.save();

    applicationThree =
        new ApplicationModel(applicantTwo, fakeProgramWithEnumerator, LifecycleStage.OBSOLETE);
    applicationThree.setApplicantData(applicantTwo.getApplicantData());
    CfTestHelpers.withMockedInstantNow(
        "2022-03-01T00:00:00Z", () -> applicationThree.setSubmitTimeToNow());
    applicationThree.save();
  }

  /** A Builder to build a fake program */
  static class FakeProgramBuilder {
    ProgramBuilder fakeProgramBuilder;
    boolean addEnumeratorQuestion = false;
    boolean addNestedEnumeratorQuestion = false;
    ImmutableList.Builder<QuestionModel> householdMembersRepeatedQuestions =
        ImmutableList.builder();

    private StatusDefinitions statusDefinitions = new StatusDefinitions();

    private FakeProgramBuilder(String name) {
      fakeProgramBuilder = ProgramBuilder.newActiveProgram(name);
    }

    private FakeProgramBuilder(ProgramBuilder builder) {
      fakeProgramBuilder = builder;
    }

    static FakeProgramBuilder newActiveProgram() {
      return new FakeProgramBuilder("Fake Program");
    }

    static FakeProgramBuilder newActiveProgram(String name) {
      return new FakeProgramBuilder(name);
    }

    static FakeProgramBuilder newDraftOf(ProgramModel program) throws ProgramNotFoundException {
      ProgramDefinition draft = programService.newDraftOf(program.id);
      return new FakeProgramBuilder(ProgramBuilder.newBuilderFor(draft));
    }

    static FakeProgramBuilder removeBlockWithQuestion(
        ProgramModel program, QuestionModel questionToRemove)
        throws ProgramNotFoundException,
            ProgramNeedsABlockException,
            IllegalPredicateOrderingException {
      ProgramDefinition draft = programService.newDraftOf(program.id);
      var blockToDelete =
          draft.blockDefinitions().stream()
              .filter(
                  b ->
                      b.programQuestionDefinitions().stream()
                          .anyMatch(q -> q.id() == questionToRemove.id))
              .findFirst()
              .get()
              .id();

      ProgramDefinition draftWithoutBlock = programService.deleteBlock(draft.id(), blockToDelete);
      return new FakeProgramBuilder(ProgramBuilder.newBuilderFor(draftWithoutBlock));
    }

    FakeProgramBuilder withStatuses(ImmutableList<String> statuses) {
      statusDefinitions =
          new StatusDefinitions()
              .setStatuses(
                  statuses.stream()
                      .map(
                          status ->
                              Status.builder()
                                  .setStatusText(status)
                                  .setLocalizedStatusText(
                                      LocalizedStrings.builder()
                                          .setTranslations(ImmutableMap.of(Locale.ENGLISH, status))
                                          .build())
                                  .build())
                      .collect(ImmutableList.toImmutableList()));
      return this;
    }

    FakeProgramBuilder withQuestion(QuestionModel question) {
      fakeProgramBuilder.withBlock().withRequiredQuestion(question).build();
      return this;
    }

    /**
     * Adds a question with a visibility predicate. If the text question ({@code applicant favorite
     * color}) is answered with "red" then the date question ({@code applicant birth date}) isn't
     * shown to the applicant.
     *
     * @return the fake {@link ProgramBuilder}
     */
    FakeProgramBuilder withDateQuestionWithVisibilityPredicateOnTextQuestion() {
      QuestionModel dateQuestion = testQuestionBank.dateApplicantBirthdate();
      QuestionModel colorQuestion = testQuestionBank.textApplicantFavoriteColor();

      PredicateDefinition colorPredicate =
          PredicateDefinition.create(
              PredicateExpressionNode.create(
                  LeafOperationExpressionNode.create(
                      colorQuestion.id, Scalar.TEXT, Operator.EQUAL_TO, PredicateValue.of("red"))),
              PredicateAction.HIDE_BLOCK);

      fakeProgramBuilder
          .withBlock()
          .withRequiredQuestion(colorQuestion)
          .withBlock()
          .withRequiredQuestions(dateQuestion)
          .withVisibilityPredicate(colorPredicate)
          .build();

      return this;
    }

    FakeProgramBuilder withHouseholdMembersEnumeratorQuestion() {
      addEnumeratorQuestion = true;
      return this;
    }

    FakeProgramBuilder withHouseholdMembersRepeatedQuestion(QuestionModel repeatedQuestion) {
      householdMembersRepeatedQuestions.add(repeatedQuestion);
      return this;
    }

    FakeProgramBuilder withHouseholdMembersJobsNestedEnumeratorQuestion() {
      addNestedEnumeratorQuestion = true;
      return this;
    }

    ProgramModel build() {
      ProgramModel fakeProgram;
      if (addEnumeratorQuestion && addNestedEnumeratorQuestion) {
        fakeProgram =
            fakeProgramBuilder
                .withBlock()
                .withRequiredQuestion(testQuestionBank.enumeratorApplicantHouseholdMembers())
                .withRepeatedBlock()
                .withRequiredQuestions(householdMembersRepeatedQuestions.build())
                .withAnotherRepeatedBlock()
                .withRequiredQuestion(
                    testQuestionBank.enumeratorNestedApplicantHouseholdMemberJobs())
                .withRepeatedBlock()
                .withRequiredQuestion(
                    testQuestionBank.numberNestedRepeatedApplicantHouseholdMemberDaysWorked())
                .build();
      } else if (addEnumeratorQuestion) {
        fakeProgram =
            fakeProgramBuilder
                .withBlock()
                .withRequiredQuestion(testQuestionBank.enumeratorApplicantHouseholdMembers())
                .withRepeatedBlock()
                .withRequiredQuestions(householdMembersRepeatedQuestions.build())
                .build();
      } else {
        fakeProgram = fakeProgramBuilder.build();
      }

      appStatusRepo.createOrUpdateStatusDefinitions(
          fakeProgram.getProgramDefinition().adminName(), statusDefinitions);

      return fakeProgram;
    }
  }

  /** A "Builder" to fill a fake application one question at a time. */
  static class FakeApplicationFiller {
    AccountModel admin;
    ApplicantModel applicant;
    ProgramModel program;
    Optional<AccountModel> trustedIntermediary = Optional.empty();
    ImmutableList<RepeatedEntity> repeatedHouseholdMemberEntities;

    private ApplicationModel application;
    private Instant createTime = FAKE_CREATE_TIME;
    private Instant submitTime = FAKE_SUBMIT_TIME;

    private FakeApplicationFiller(ProgramModel program, ApplicantModel applicant) {
      this.program = program;
      this.applicant = applicant;
      this.admin = resourceCreator.insertAccount();
    }

    static FakeApplicationFiller newFillerFor(ProgramModel program) {
      return new FakeApplicationFiller(program, resourceCreator.insertApplicantWithAccount());
    }

    static FakeApplicationFiller newFillerFor(ProgramModel program, ApplicantModel applicant) {
      return new FakeApplicationFiller(program, applicant);
    }

    FakeApplicationFiller byTrustedIntermediary(String tiEmail, String tiOrganization) {
      var tiGroup = resourceCreator.insertTiGroup(tiOrganization);
      this.trustedIntermediary = Optional.of(resourceCreator.insertAccountWithEmail(tiEmail));
      this.applicant.getAccount().setManagedByGroup(tiGroup).save();
      return this;
    }

    FakeApplicationFiller atCreateTime(Instant createTime) {
      this.createTime = createTime;
      return this;
    }

    FakeApplicationFiller atSubmitTime(Instant submitTime) {
      this.submitTime = submitTime;
      return this;
    }

    FakeApplicationFiller answerAddressQuestion(
        QuestionModel question,
        String street,
        String line2,
        String city,
        String state,
        String zip) {
      return answerAddressQuestion(question, null, street, line2, city, state, zip);
    }

    FakeApplicationFiller answerAddressQuestion(
        QuestionModel question,
        String repeatedEntityName,
        String street,
        String line2,
        String city,
        String state,
        String zip) {
      var repeatedEntity =
          Optional.ofNullable(repeatedEntityName).flatMap(name -> getHouseholdMemberEntity(name));
      Path answerPath =
          question
              .getQuestionDefinition()
              .getContextualizedPath(repeatedEntity, ApplicantData.APPLICANT_PATH);
      QuestionAnswerer.answerAddressQuestion(
          applicant.getApplicantData(), answerPath, street, line2, city, state, zip);
      applicant.save();
      return this;
    }

    FakeApplicationFiller answerCorrectedAddressQuestion(
        QuestionModel question,
        String street,
        String line2,
        String city,
        String state,
        String zip,
        String corrected,
        Double latitude,
        Double longitude,
        Long wellKnownId,
        ImmutableList<ServiceAreaInclusion> serviceAreaInclusions) {
      return answerCorrectedAddressQuestion(
          question,
          null,
          street,
          line2,
          city,
          state,
          zip,
          corrected,
          latitude,
          longitude,
          wellKnownId,
          serviceAreaInclusions);
    }

    FakeApplicationFiller answerCorrectedAddressQuestion(
        QuestionModel question,
        String repeatedEntityName,
        String street,
        String line2,
        String city,
        String state,
        String zip,
        String corrected,
        Double latitude,
        Double longitude,
        Long wellKnownId,
        ImmutableList<ServiceAreaInclusion> serviceAreaInclusions) {
      var repeatedEntity =
          Optional.ofNullable(repeatedEntityName).flatMap(name -> getHouseholdMemberEntity(name));
      Path answerPath =
          question
              .getQuestionDefinition()
              .getContextualizedPath(repeatedEntity, ApplicantData.APPLICANT_PATH);
      QuestionAnswerer.answerAddressQuestion(
          applicant.getApplicantData(),
          answerPath,
          street,
          line2,
          city,
          state,
          zip,
          corrected,
          latitude,
          longitude,
          wellKnownId,
          serviceAreaInclusions);
      applicant.save();
      return this;
    }

    FakeApplicationFiller answerCheckboxQuestion(
        QuestionModel question, ImmutableList<Long> optionIds) {
      return answerCheckboxQuestion(question, null, optionIds);
    }

    FakeApplicationFiller answerCheckboxQuestion(
        QuestionModel question, String repeatedEntityName, ImmutableList<Long> optionIds) {
      var repeatedEntity =
          Optional.ofNullable(repeatedEntityName).flatMap(name -> getHouseholdMemberEntity(name));
      Path answerPath =
          question
              .getQuestionDefinition()
              .getContextualizedPath(repeatedEntity, ApplicantData.APPLICANT_PATH);
      ApplicantData applicantData = applicant.getApplicantData();
      for (int i = 0; i < optionIds.size(); i++) {
        QuestionAnswerer.answerMultiSelectQuestion(applicantData, answerPath, i, optionIds.get(i));
      }
      applicant.save();
      return this;
    }

    FakeApplicationFiller answerCurrencyQuestion(QuestionModel question, String answer) {
      return answerCurrencyQuestion(question, null, answer);
    }

    FakeApplicationFiller answerCurrencyQuestion(
        QuestionModel question, String repeatedEntityName, String answer) {
      var repeatedEntity =
          Optional.ofNullable(repeatedEntityName).flatMap(name -> getHouseholdMemberEntity(name));
      Path answerPath =
          question
              .getQuestionDefinition()
              .getContextualizedPath(repeatedEntity, ApplicantData.APPLICANT_PATH);
      QuestionAnswerer.answerCurrencyQuestion(applicant.getApplicantData(), answerPath, answer);
      applicant.save();
      return this;
    }

    FakeApplicationFiller answerDateQuestion(QuestionModel question, String answer) {
      return answerDateQuestion(question, null, answer);
    }

    FakeApplicationFiller answerDateQuestion(
        QuestionModel question, String repeatedEntityName, String answer) {
      var repeatedEntity =
          Optional.ofNullable(repeatedEntityName).flatMap(name -> getHouseholdMemberEntity(name));
      Path answerPath =
          question
              .getQuestionDefinition()
              .getContextualizedPath(repeatedEntity, ApplicantData.APPLICANT_PATH);
      QuestionAnswerer.answerDateQuestion(applicant.getApplicantData(), answerPath, answer);
      applicant.save();
      return this;
    }

    FakeApplicationFiller answerDropdownQuestion(QuestionModel question, Long optionId) {
      return answerDropdownQuestion(question, null, optionId);
    }

    FakeApplicationFiller answerDropdownQuestion(
        QuestionModel question, String repeatedEntityName, Long optionId) {
      var repeatedEntity =
          Optional.ofNullable(repeatedEntityName).flatMap(name -> getHouseholdMemberEntity(name));
      Path answerPath =
          question
              .getQuestionDefinition()
              .getContextualizedPath(repeatedEntity, ApplicantData.APPLICANT_PATH);
      ApplicantData applicantData = applicant.getApplicantData();
      QuestionAnswerer.answerSingleSelectQuestion(applicantData, answerPath, optionId);
      applicant.save();
      return this;
    }

    FakeApplicationFiller answerEmailQuestion(QuestionModel question, String answer) {
      return answerEmailQuestion(question, null, answer);
    }

    FakeApplicationFiller answerEmailQuestion(
        QuestionModel question, String repeatedEntityName, String answer) {
      var repeatedEntity =
          Optional.ofNullable(repeatedEntityName).flatMap(name -> getHouseholdMemberEntity(name));
      Path answerPath =
          question
              .getQuestionDefinition()
              .getContextualizedPath(repeatedEntity, ApplicantData.APPLICANT_PATH);
      QuestionAnswerer.answerEmailQuestion(applicant.getApplicantData(), answerPath, answer);
      applicant.save();
      return this;
    }

    FakeApplicationFiller answerFileUploadQuestion(QuestionModel question, String fileKey) {
      return answerFileUploadQuestion(question, null, fileKey);
    }

    FakeApplicationFiller answerFileUploadQuestion(
        QuestionModel question, String repeatedEntityName, String fileKey) {
      var repeatedEntity =
          Optional.ofNullable(repeatedEntityName).flatMap(name -> getHouseholdMemberEntity(name));
      Path answerPath =
          question
              .getQuestionDefinition()
              .getContextualizedPath(repeatedEntity, ApplicantData.APPLICANT_PATH);

      QuestionAnswerer.answerFileQuestion(applicant.getApplicantData(), answerPath, fileKey);
      applicant.save();
      return this;
    }

    FakeApplicationFiller answerFileQuestionWithMultipleUpload(
        QuestionModel question, ImmutableList<String> fileKeys) {
      return answerFileQuestionWithMultipleUpload(question, null, fileKeys);
    }

    FakeApplicationFiller answerFileQuestionWithMultipleUpload(
        QuestionModel question, String repeatedEntityName, ImmutableList<String> fileKeys) {
      var repeatedEntity =
          Optional.ofNullable(repeatedEntityName).flatMap(name -> getHouseholdMemberEntity(name));
      Path answerPath =
          question
              .getQuestionDefinition()
              .getContextualizedPath(repeatedEntity, ApplicantData.APPLICANT_PATH);

      QuestionAnswerer.answerFileQuestionWithMultipleUpload(
          applicant.getApplicantData(), answerPath, fileKeys);
      applicant.save();
      return this;
    }

    FakeApplicationFiller answerIdQuestion(QuestionModel question, String answer) {
      return answerIdQuestion(question, null, answer);
    }

    FakeApplicationFiller answerIdQuestion(
        QuestionModel question, String repeatedEntityName, String answer) {
      var repeatedEntity =
          Optional.ofNullable(repeatedEntityName).flatMap(name -> getHouseholdMemberEntity(name));
      Path answerPath =
          question
              .getQuestionDefinition()
              .getContextualizedPath(repeatedEntity, ApplicantData.APPLICANT_PATH);

      QuestionAnswerer.answerIdQuestion(applicant.getApplicantData(), answerPath, answer);
      applicant.save();
      return this;
    }

    FakeApplicationFiller answerNameQuestion(
        QuestionModel question,
        String firstName,
        String middleName,
        String lastName,
        String suffix) {
      return answerNameQuestion(question, null, firstName, middleName, lastName, suffix);
    }

    FakeApplicationFiller answerNameQuestion(
        QuestionModel question,
        String repeatedEntityName,
        String firstName,
        String middleName,
        String lastName,
        String suffix) {
      var repeatedEntity =
          Optional.ofNullable(repeatedEntityName).flatMap(name -> getHouseholdMemberEntity(name));
      Path answerPath =
          question
              .getQuestionDefinition()
              .getContextualizedPath(repeatedEntity, ApplicantData.APPLICANT_PATH);
      QuestionAnswerer.answerNameQuestion(
          applicant.getApplicantData(), answerPath, firstName, middleName, lastName, suffix);
      applicant.save();
      return this;
    }

    FakeApplicationFiller answerNumberQuestion(QuestionModel question, long answer) {
      return answerNumberQuestion(question, null, answer);
    }

    FakeApplicationFiller answerNumberQuestion(
        QuestionModel question, String repeatedEntityName, long answer) {
      var repeatedEntity =
          Optional.ofNullable(repeatedEntityName).flatMap(name -> getHouseholdMemberEntity(name));
      Path answerPath =
          question
              .getQuestionDefinition()
              .getContextualizedPath(repeatedEntity, ApplicantData.APPLICANT_PATH);
      QuestionAnswerer.answerNumberQuestion(applicant.getApplicantData(), answerPath, answer);
      applicant.save();
      return this;
    }

    FakeApplicationFiller answerPhoneQuestion(
        QuestionModel question, String countryCode, String phoneNumber) {
      return answerPhoneQuestion(question, null, countryCode, phoneNumber);
    }

    FakeApplicationFiller answerPhoneQuestion(
        QuestionModel question, String repeatedEntityName, String countryCode, String phoneNumber) {
      var repeatedEntity =
          Optional.ofNullable(repeatedEntityName).flatMap(name -> getHouseholdMemberEntity(name));
      Path answerPath =
          question
              .getQuestionDefinition()
              .getContextualizedPath(repeatedEntity, ApplicantData.APPLICANT_PATH);
      QuestionAnswerer.answerPhoneQuestion(
          applicant.getApplicantData(), answerPath, countryCode, phoneNumber);
      applicant.save();
      return this;
    }

    FakeApplicationFiller answerRadioButtonQuestion(QuestionModel question, Long optionId) {
      return answerRadioButtonQuestion(question, null, optionId);
    }

    FakeApplicationFiller answerRadioButtonQuestion(
        QuestionModel question, String repeatedEntityName, Long optionId) {
      var repeatedEntity =
          Optional.ofNullable(repeatedEntityName).flatMap(name -> getHouseholdMemberEntity(name));
      Path answerPath =
          question
              .getQuestionDefinition()
              .getContextualizedPath(repeatedEntity, ApplicantData.APPLICANT_PATH);
      ApplicantData applicantData = applicant.getApplicantData();
      QuestionAnswerer.answerSingleSelectQuestion(applicantData, answerPath, optionId);
      applicant.save();
      return this;
    }

    FakeApplicationFiller answerTextQuestion(QuestionModel question, String answer) {
      return answerTextQuestion(question, null, answer);
    }

    FakeApplicationFiller answerTextQuestion(
        QuestionModel question, String repeatedEntityName, String answer) {
      var repeatedEntity =
          Optional.ofNullable(repeatedEntityName).flatMap(name -> getHouseholdMemberEntity(name));
      Path answerPath =
          question
              .getQuestionDefinition()
              .getContextualizedPath(repeatedEntity, ApplicantData.APPLICANT_PATH);
      QuestionAnswerer.answerTextQuestion(applicant.getApplicantData(), answerPath, answer);
      applicant.save();
      return this;
    }

    FakeApplicationFiller answerEnumeratorQuestion(ImmutableList<String> householdMembers) {
      Path answerPath =
          testQuestionBank
              .enumeratorApplicantHouseholdMembers()
              .getQuestionDefinition()
              .getContextualizedPath(
                  /* repeatedEntity= */ Optional.empty(), ApplicantData.APPLICANT_PATH);
      QuestionAnswerer.answerEnumeratorQuestion(
          applicant.getApplicantData(), answerPath, householdMembers);
      applicant.save();

      // Store repeated entities so we can answer other questions easily
      this.repeatedHouseholdMemberEntities =
          RepeatedEntity.createRepeatedEntities(
              (EnumeratorQuestionDefinition)
                  testQuestionBank.enumeratorApplicantHouseholdMembers().getQuestionDefinition(),
              /* visibility= */ Optional.empty(),
              applicant.getApplicantData());

      return this;
    }

    FakeApplicationFiller answerNestedEnumeratorQuestion(
        String parentEntityName, ImmutableList<String> jobNames) {
      var repeatedEntities =
          RepeatedEntity.createRepeatedEntities(
              (EnumeratorQuestionDefinition)
                  testQuestionBank.enumeratorApplicantHouseholdMembers().getQuestionDefinition(),
              /* visibility= */ Optional.empty(),
              applicant.getApplicantData());
      var parentRepeatedEntity =
          repeatedEntities.stream()
              .filter(e -> e.entityName().equals(parentEntityName))
              .findFirst();
      Path answerPath =
          testQuestionBank
              .enumeratorNestedApplicantHouseholdMemberJobs()
              .getQuestionDefinition()
              .getContextualizedPath(parentRepeatedEntity, ApplicantData.APPLICANT_PATH);
      QuestionAnswerer.answerEnumeratorQuestion(applicant.getApplicantData(), answerPath, jobNames);
      applicant.save();
      return this;
    }

    FakeApplicationFiller answerNestedRepeatedNumberQuestion(
        String parentEntityName, String entityName, long answer) {
      var repeatedEntities =
          RepeatedEntity.createRepeatedEntities(
              (EnumeratorQuestionDefinition)
                  testQuestionBank.enumeratorApplicantHouseholdMembers().getQuestionDefinition(),
              /* visibility= */ Optional.empty(),
              applicant.getApplicantData());
      var nestedRepeatedEntities =
          repeatedEntities.stream()
              .filter(e -> e.entityName().equals(parentEntityName))
              .findFirst()
              .get()
              .createNestedRepeatedEntities(
                  (EnumeratorQuestionDefinition)
                      testQuestionBank
                          .enumeratorNestedApplicantHouseholdMemberJobs()
                          .getQuestionDefinition(),
                  /* visibility= */ Optional.empty(),
                  applicant.getApplicantData());

      var nestedRepeatedEntity =
          nestedRepeatedEntities.stream()
              .filter(e -> e.entityName().equals(entityName))
              .findFirst();
      Path answerPath =
          testQuestionBank
              .numberNestedRepeatedApplicantHouseholdMemberDaysWorked()
              .getQuestionDefinition()
              .getContextualizedPath(nestedRepeatedEntity, ApplicantData.APPLICANT_PATH);
      QuestionAnswerer.answerNumberQuestion(applicant.getApplicantData(), answerPath, answer);
      applicant.save();
      return this;
    }

    FakeApplicationFiller submit() {
      application = new ApplicationModel(applicant, program, LifecycleStage.ACTIVE);
      application.setApplicantData(applicant.getApplicantData());
      trustedIntermediary.ifPresent(
          account -> application.setSubmitterEmail(account.getEmailAddress()));
      application.save();

      // CreateTime of an application is set through @onCreate to Instant.now(). To change
      // the value, manually set createTime and save and refresh the application.
      application.setCreateTimeForTest(this.createTime);
      application.setSubmitTimeForTest(this.submitTime);
      application.save();

      return this;
    }

    FakeApplicationFiller markObsolete() {
      if (application == null) {
        throw new IllegalStateException(
            "Cannot mark an application as obsolete unless it has been submitted.");
      }
      application.setLifecycleStage(LifecycleStage.OBSOLETE);
      application.save();

      return this;
    }

    ApplicationModel getApplication() {
      return application;
    }

    private Optional<RepeatedEntity> getHouseholdMemberEntity(String entityName) {
      return repeatedHouseholdMemberEntities.stream()
          .filter(e -> e.entityName().equals(entityName))
          .findFirst();
    }
  }
}
