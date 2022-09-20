package services.export;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import java.util.Comparator;
import java.util.Locale;
import java.util.Optional;
import models.Account;
import models.Applicant;
import models.Application;
import models.LifecycleStage;
import models.Program;
import models.Question;
import org.junit.Before;
import repository.ResetPostgres;
import services.LocalizedStrings;
import services.Path;
import services.applicant.ApplicantData;
import services.application.ApplicationEventDetails.StatusEvent;
import services.applications.ProgramAdminApplicationService;
import services.program.StatusDefinitions;
import services.program.StatusDefinitions.Status;
import services.question.types.QuestionType;
import support.CfTestHelpers;
import support.ProgramBuilder;
import support.QuestionAnswerer;

/**
 * Superclass for tests that exercise exporters. Helps with generating programs, questions, and
 * applications.
 */
public abstract class AbstractExporterTest extends ResetPostgres {
  public static final String STATUS_TEXT = "approved";
  private ProgramAdminApplicationService programAdminApplicationService;

  protected Program fakeProgramWithEnumerator;
  protected Program fakeProgramWithOptionalFileUpload;
  protected Program fakeProgram;
  protected ImmutableList<Question> fakeQuestions;
  protected Applicant applicantOne;
  protected Applicant applicantFive;
  protected Applicant applicantSix;
  protected Applicant applicantTwo;
  protected Application applicationOne;
  protected Application applicationTwo;
  protected Application applicationThree;
  protected Application applicationFour;
  protected Application applicationFive;
  protected Application applicationSix;

  @Before
  public void setup() {
    programAdminApplicationService = instanceOf(ProgramAdminApplicationService.class);
  }

  protected void answerQuestion(
      QuestionType questionType,
      Question question,
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
        QuestionAnswerer.answerNameQuestion(applicantDataOne, answerPath, "Alice", "", "Appleton");
        QuestionAnswerer.answerNameQuestion(applicantDataTwo, answerPath, "Bob", "", "Baker");
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
      case STATIC:
        // Do nothing.
        break;
    }
  }

  /**
   * Setup application 1-4.
   *
   * 1-3 have the same user with each of the three possible states, each with
   * Status approved. 4 is a different user in Active state.
   */
  protected void createFakeApplications() throws Exception {
    Account admin = resourceCreator.insertAccount();
    Applicant applicantOne = resourceCreator.insertApplicantWithAccount();
    Applicant applicantTwo = resourceCreator.insertApplicantWithAccount();
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
        new Application(applicantOne, fakeProgram, LifecycleStage.ACTIVE).setSubmitTimeToNow();
    applicationOne.save();
    programAdminApplicationService.setStatus(
        applicationOne,
        StatusEvent.builder().setEmailSent(false).setStatusText(STATUS_TEXT).build(),
        admin);

    applicationTwo =
        new Application(applicantOne, fakeProgram, LifecycleStage.OBSOLETE).setSubmitTimeToNow();
    applicationTwo.save();
    programAdminApplicationService.setStatus(
        applicationTwo,
        StatusEvent.builder().setEmailSent(false).setStatusText(STATUS_TEXT).build(),
        admin);

    applicationThree =
        new Application(applicantOne, fakeProgram, LifecycleStage.DRAFT).setSubmitTimeToNow();
    applicationThree.save();
    programAdminApplicationService.setStatus(
        applicationThree,
        StatusEvent.builder().setEmailSent(false).setStatusText(STATUS_TEXT).build(),
        admin);

    applicationFour =
        new Application(applicantTwo, fakeProgram, LifecycleStage.ACTIVE).setSubmitTimeToNow();
    applicationFour.save();
  }

  protected void createFakeQuestions() {
    this.fakeQuestions =
        testQuestionBank.getSampleQuestionsForAllTypes().values().stream()
            .sorted(Comparator.comparing(question -> question.getQuestionDefinition().getName()))
            .collect(ImmutableList.toImmutableList());
  }

  protected void createFakeProgram() {
    ProgramBuilder fakeProgram = ProgramBuilder.newActiveProgram();
    fakeProgram.withName("Fake Program");
    fakeQuestions.forEach(
        question -> fakeProgram.withBlock().withRequiredQuestion(question).build());
    fakeProgram.withStatusDefinitions(
        new StatusDefinitions()
            .setStatuses(
                ImmutableList.of(
                    Status.builder()
                        .setStatusText(STATUS_TEXT)
                        .setLocalizedStatusText(
                            LocalizedStrings.builder()
                                .setTranslations(ImmutableMap.of(Locale.ENGLISH, STATUS_TEXT))
                                .build())
                        .build())));

    this.fakeProgram = fakeProgram.build();
  }

  protected void createFakeProgramWithOptionalQuestion() {
    Question fileQuestion = testQuestionBank.applicantFile();
    Question nameQuestion = testQuestionBank.applicantName();

    fakeProgramWithOptionalFileUpload =
        ProgramBuilder.newActiveProgram()
            .withName("Fake Optional Question Program")
            .withBlock()
            .withRequiredQuestion(nameQuestion)
            .withBlock()
            .withOptionalQuestion(fileQuestion)
            .build();

    Path answerPath =
        fileQuestion
            .getQuestionDefinition()
            .getContextualizedPath(Optional.empty(), ApplicantData.APPLICANT_PATH);
    // Applicant five have file uploaded for the optional file upload question
    applicantFive = resourceCreator.insertApplicantWithAccount();
    QuestionAnswerer.answerNameQuestion(
        applicantFive.getApplicantData(),
        ApplicantData.APPLICANT_PATH.join(
            nameQuestion.getQuestionDefinition().getQuestionPathSegment()),
        "Example",
        "",
        "Five");
    QuestionAnswerer.answerFileQuestion(
        applicantFive.getApplicantData(), answerPath, "my-file-key");
    applicationFive =
        new Application(applicantFive, fakeProgramWithOptionalFileUpload, LifecycleStage.ACTIVE);
    applicantFive.save();
    CfTestHelpers.withMockedInstantNow(
        "2022-01-01T00:00:00Z", () -> applicationFive.setSubmitTimeToNow());
    applicationFive.save();
    // Applicant six hasn't uploaded a file for the optional file upload question
    applicantSix = resourceCreator.insertApplicantWithAccount();
    QuestionAnswerer.answerNameQuestion(
        applicantSix.getApplicantData(),
        ApplicantData.APPLICANT_PATH.join(
            nameQuestion.getQuestionDefinition().getQuestionPathSegment()),
        "Example",
        "",
        "Six");
    applicationSix =
        new Application(applicantSix, fakeProgramWithOptionalFileUpload, LifecycleStage.ACTIVE);
    applicantSix.save();
    CfTestHelpers.withMockedInstantNow(
        "2022-01-01T00:00:00Z", () -> applicationSix.setSubmitTimeToNow());
    applicationSix.save();
  }
  /**
   * Creates a program that has an enumerator question with children, three applicants, and three
   * applications. The applications have submission times one month apart starting on 2022-01-01.
   */
  protected void createFakeProgramWithEnumerator() {
    Question nameQuestion = testQuestionBank.applicantName();
    Question colorQuestion = testQuestionBank.applicantFavoriteColor();
    Question monthlyIncomeQuestion = testQuestionBank.applicantMonthlyIncome();
    Question householdMembersQuestion = testQuestionBank.applicantHouseholdMembers();
    Question hmNameQuestion = testQuestionBank.applicantHouseholdMemberName();
    Question hmJobsQuestion = testQuestionBank.applicantHouseholdMemberJobs();
    Question hmNumberDaysWorksQuestion = testQuestionBank.applicantHouseholdMemberDaysWorked();
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
        "Doe");
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
        "Anderson");
    QuestionAnswerer.answerNameQuestion(
        applicantOne.getApplicantData(),
        hmPath.atIndex(1).join(hmNameQuestion.getQuestionDefinition().getQuestionPathSegment()),
        "Bailey",
        "",
        "Bailerson");
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
        new Application(applicantOne, fakeProgramWithEnumerator, LifecycleStage.ACTIVE);

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
        "Doe");
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
        "Jameson");
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
        new Application(applicantTwo, fakeProgramWithEnumerator, LifecycleStage.ACTIVE);
    CfTestHelpers.withMockedInstantNow(
        "2022-02-01T00:00:00Z", () -> applicationTwo.setSubmitTimeToNow());
    applicationTwo.save();

    applicationThree =
        new Application(applicantTwo, fakeProgramWithEnumerator, LifecycleStage.OBSOLETE);
    CfTestHelpers.withMockedInstantNow(
        "2022-03-01T00:00:00Z", () -> applicationThree.setSubmitTimeToNow());
    applicationThree.save();
  }
}
