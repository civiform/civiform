package services.applicant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.catchThrowable;

import auth.CiviFormProfile;
import auth.ProfileFactory;
import auth.ProgramAcls;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import java.time.Instant;
import java.util.HashSet;
import java.util.Locale;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutionException;
import models.Account;
import models.Applicant;
import models.Application;
import models.ApplicationEvent;
import models.DisplayMode;
import models.LifecycleStage;
import models.Program;
import models.Question;
import models.StoredFile;
import models.TrustedIntermediaryGroup;
import org.apache.commons.lang3.reflect.FieldUtils;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import play.i18n.Lang;
import play.i18n.Messages;
import play.i18n.MessagesApi;
import repository.ApplicationRepository;
import repository.ResetPostgres;
import repository.UserRepository;
import repository.VersionRepository;
import services.Address;
import services.LocalizedStrings;
import services.MessageKey;
import services.Path;
import services.applicant.ApplicantPersonalInfo.Representation;
import services.applicant.ApplicantService.ApplicantProgramData;
import services.applicant.exception.ApplicantNotFoundException;
import services.applicant.exception.ApplicationNotEligibleException;
import services.applicant.exception.ApplicationOutOfDateException;
import services.applicant.exception.ApplicationSubmissionException;
import services.applicant.exception.ProgramBlockNotFoundException;
import services.applicant.question.AddressQuestion;
import services.applicant.question.ApplicantQuestion;
import services.applicant.question.Scalar;
import services.application.ApplicationEventDetails;
import services.application.ApplicationEventDetails.StatusEvent;
import services.cloud.aws.SimpleEmail;
import services.geo.AddressLocation;
import services.geo.AddressSuggestion;
import services.geo.AddressSuggestionGroup;
import services.geo.CorrectedAddressState;
import services.program.BlockDefinition;
import services.program.EligibilityDefinition;
import services.program.PathNotInBlockException;
import services.program.ProgramBlockDefinitionNotFoundException;
import services.program.ProgramDefinition;
import services.program.ProgramNotFoundException;
import services.program.ProgramQuestionDefinitionInvalidException;
import services.program.ProgramQuestionDefinitionNotFoundException;
import services.program.ProgramService;
import services.program.ProgramServiceImpl;
import services.program.ProgramType;
import services.program.StatusDefinitions;
import services.program.predicate.LeafAddressServiceAreaExpressionNode;
import services.program.predicate.LeafOperationExpressionNode;
import services.program.predicate.Operator;
import services.program.predicate.PredicateAction;
import services.program.predicate.PredicateDefinition;
import services.program.predicate.PredicateExpressionNode;
import services.program.predicate.PredicateValue;
import services.question.QuestionOption;
import services.question.QuestionService;
import services.question.types.FileUploadQuestionDefinition;
import services.question.types.MultiOptionQuestionDefinition;
import services.question.types.MultiOptionQuestionDefinition.MultiOptionQuestionType;
import services.question.types.NameQuestionDefinition;
import services.question.types.QuestionDefinition;
import services.question.types.QuestionDefinitionConfig;
import support.ProgramBuilder;
import views.applicant.AddressCorrectionBlockView;

public class ApplicantServiceTest extends ResetPostgres {

  private ApplicantService subject;
  private QuestionService questionService;
  private NameQuestionDefinition questionDefinition;
  private ProgramDefinition programDefinition;
  private UserRepository userRepository;
  private ApplicationRepository applicationRepository;
  private VersionRepository versionRepository;
  private CiviFormProfile trustedIntermediaryProfile;
  private ProgramService programService;
  private String baseUrl;
  private SimpleEmail amazonSESClient;
  private MessagesApi messagesApi;
  private CiviFormProfile applicantProfile;
  private ProfileFactory profileFactory;

  @Before
  public void setUp() throws Exception {
    profileFactory = instanceOf(ProfileFactory.class);
    Config config = ConfigFactory.load();
    baseUrl = config.getString("base_url");
    subject = instanceOf(ApplicantService.class);
    questionService = instanceOf(QuestionService.class);
    userRepository = instanceOf(UserRepository.class);
    applicationRepository = instanceOf(ApplicationRepository.class);
    versionRepository = instanceOf(VersionRepository.class);
    createQuestions();
    createProgram();

    trustedIntermediaryProfile = Mockito.mock(CiviFormProfile.class);
    applicantProfile = Mockito.mock(CiviFormProfile.class);
    Account account = new Account();
    account.setEmailAddress("test@example.com");
    Mockito.when(trustedIntermediaryProfile.isTrustedIntermediary()).thenReturn(true);
    Mockito.when(trustedIntermediaryProfile.getAccount())
        .thenReturn(CompletableFuture.completedFuture(account));
    Mockito.when(trustedIntermediaryProfile.getEmailAddress())
        .thenReturn(CompletableFuture.completedFuture("test@example.com"));
    Mockito.when(applicantProfile.isTrustedIntermediary()).thenReturn(false);
    Account applicantAccount = new Account();
    applicantAccount.setEmailAddress("applicant@example.com");
    Mockito.when(applicantProfile.getAccount())
        .thenReturn(CompletableFuture.completedFuture(applicantAccount));
    Mockito.when(applicantProfile.getEmailAddress())
        .thenReturn(CompletableFuture.completedFuture("applicant@example.com"));

    programService = instanceOf(ProgramServiceImpl.class);

    amazonSESClient = Mockito.mock(SimpleEmail.class);
    FieldUtils.writeField(subject, "amazonSESClient", amazonSESClient, true);

    messagesApi = instanceOf(MessagesApi.class);
  }

  private static final StatusDefinitions.Status APPROVED_STATUS =
      StatusDefinitions.Status.builder()
          .setStatusText("Approved")
          .setLocalizedStatusText(LocalizedStrings.of(Locale.US, "Approved"))
          .setLocalizedEmailBodyText(
              Optional.of(
                  LocalizedStrings.of(
                      Locale.US, "I'm a US email!",
                      Locale.KOREA, "I'm a KOREAN email!")))
          .build();

  @Test
  public void stageAndUpdateIfValid_emptySetOfUpdates_leavesQuestionsUnansweredAndUpdatesMetadata()
      throws Exception {
    // We make the question optional since it's not valid to stage empty updates
    // for a required question.
    createProgramWithOptionalQuestion(questionDefinition);
    Applicant applicant = subject.createApplicant().toCompletableFuture().join();
    subject
        .stageAndUpdateIfValid(applicant.id, programDefinition.id(), "1", ImmutableMap.of(), false)
        .toCompletableFuture()
        .join();
    ApplicantData applicantData =
        userRepository.lookupApplicantSync(applicant.id).get().getApplicantData();

    Path questionPath =
        ApplicantData.APPLICANT_PATH.join(questionDefinition.getQuestionPathSegment());
    Scalar.getScalars(questionDefinition.getQuestionType()).stream()
        .map(questionPath::join)
        .forEach(path -> assertThat(applicantData.hasPath(path)).isFalse());
    assertThat(applicantData.readLong(questionPath.join(Scalar.PROGRAM_UPDATED_IN)))
        .contains(programDefinition.id());
  }

  @Test
  public void stageAndUpdateIfValid_withUpdatesWithEmptyStrings_deletesJsonData() {
    // We make the question optional since it's not valid to update a required
    // question with an empty string (done below).
    createProgramWithOptionalQuestion(questionDefinition);
    Applicant applicant = subject.createApplicant().toCompletableFuture().join();
    Path questionPath = Path.create("applicant.name");

    // Put something in there
    ImmutableMap<String, String> updates =
        ImmutableMap.<String, String>builder()
            .put(questionPath.join(Scalar.FIRST_NAME).toString(), "Alice")
            .put(questionPath.join(Scalar.LAST_NAME).toString(), "Doe")
            .build();
    subject
        .stageAndUpdateIfValid(applicant.id, programDefinition.id(), "1", updates, false)
        .toCompletableFuture()
        .join();
    ApplicantData applicantDataMiddle =
        userRepository.lookupApplicantSync(applicant.id).get().getApplicantData();
    assertThat(applicantDataMiddle.asJsonString()).contains("Alice", "Doe");

    // Now put empty updates
    updates =
        ImmutableMap.<String, String>builder()
            .put(questionPath.join(Scalar.FIRST_NAME).toString(), "")
            .put(questionPath.join(Scalar.LAST_NAME).toString(), "")
            .build();
    subject
        .stageAndUpdateIfValid(applicant.id, programDefinition.id(), "1", updates, false)
        .toCompletableFuture()
        .join();

    ApplicantData applicantDataAfter =
        userRepository.lookupApplicantSync(applicant.id).get().getApplicantData();

    assertThat(applicantDataAfter.hasPath(questionPath.join(Scalar.FIRST_NAME))).isFalse();
    assertThat(applicantDataAfter.hasPath(questionPath.join(Scalar.LAST_NAME))).isFalse();
    assertThat(applicantDataAfter.readLong(questionPath.join(Scalar.PROGRAM_UPDATED_IN)))
        .contains(programDefinition.id());
  }

  @Test
  public void stageAndUpdateIfValid_withEmptyUpdatesForMultiSelect_deletesMultiSelectJsonData() {
    // We make the question optional since it's not valid to stage empty updates
    createProgramWithOptionalQuestion(
        testQuestionBank.applicantKitchenTools().getQuestionDefinition());
    Applicant applicant = subject.createApplicant().toCompletableFuture().join();
    Path questionPath = Path.create("applicant.kitchen_tools");

    // Put checkbox answer in
    ImmutableMap<String, String> updates =
        ImmutableMap.<String, String>builder()
            .put(questionPath.join(Scalar.SELECTIONS).asArrayElement().atIndex(0).toString(), "1")
            .put(questionPath.join(Scalar.SELECTIONS).asArrayElement().atIndex(1).toString(), "2")
            .build();
    subject
        .stageAndUpdateIfValid(applicant.id, programDefinition.id(), "1", updates, false)
        .toCompletableFuture()
        .join();
    ApplicantData applicantDataMiddle =
        userRepository.lookupApplicantSync(applicant.id).get().getApplicantData();
    assertThat(applicantDataMiddle.readLongList(questionPath.join(Scalar.SELECTIONS))).isNotEmpty();

    // Now put empty updates
    updates = ImmutableMap.of(questionPath.join(Scalar.SELECTIONS).asArrayElement().toString(), "");
    subject
        .stageAndUpdateIfValid(applicant.id, programDefinition.id(), "1", updates, false)
        .toCompletableFuture()
        .join();

    ApplicantData applicantDataAfter =
        userRepository.lookupApplicantSync(applicant.id).get().getApplicantData();

    assertThat(applicantDataAfter.hasPath(questionPath.join(Scalar.SELECTIONS))).isFalse();
    assertThat(applicantDataAfter.readLong(questionPath.join(Scalar.PROGRAM_UPDATED_IN)))
        .contains(programDefinition.id());
  }

  private Path applicantPathForQuestion(Question question) {
    return ApplicantData.APPLICANT_PATH.join(
        question.getQuestionDefinition().getQuestionPathSegment());
  }

  @Test
  public void stageAndUpdateIfvalid_invalidInputsForQuestionTypes() {
    Question dateQuestion = testQuestionBank.applicantDate();
    Path datePath = applicantPathForQuestion(dateQuestion).join(Scalar.DATE);
    Question currencyQuestion = testQuestionBank.applicantMonthlyIncome();
    Path currencyPath = applicantPathForQuestion(currencyQuestion).join(Scalar.CURRENCY_CENTS);
    Question numberQuestion = testQuestionBank.applicantJugglingNumber();
    Path numberPath = applicantPathForQuestion(numberQuestion).join(Scalar.NUMBER);
    Question phoneQuestion = testQuestionBank.applicantPhone();
    Path phonePath = applicantPathForQuestion(phoneQuestion).join(Scalar.PHONE_NUMBER);
    createProgram(
        dateQuestion.getQuestionDefinition(),
        currencyQuestion.getQuestionDefinition(),
        numberQuestion.getQuestionDefinition(),
        phoneQuestion.getQuestionDefinition());

    Applicant applicant = subject.createApplicant().toCompletableFuture().join();

    // Stage invalid updates for each question.
    ImmutableMap<String, String> updates =
        ImmutableMap.<String, String>builder()
            .put(datePath.toString(), "invalid_date_input")
            .put(currencyPath.toString(), "invalid_currency_input")
            .put(numberPath.toString(), "invalid_number_input")
            .put(phonePath.toString(), "invalid_phone_input")
            .build();

    // We grab the result of the stage call to assert that it contains
    // data suitable for displaying errors downstream.
    ReadOnlyApplicantProgramService resultService =
        subject
            .stageAndUpdateIfValid(applicant.id, programDefinition.id(), "1", updates, false)
            .toCompletableFuture()
            .join();

    // Applicant data is not updated and is considered invalid.
    ApplicantData resultApplicantData = resultService.getApplicantData();
    assertThat(resultApplicantData.hasPath(datePath)).isFalse();
    assertThat(resultApplicantData.hasPath(currencyPath)).isFalse();
    assertThat(resultApplicantData.hasPath(numberPath)).isFalse();
    assertThat(resultApplicantData.hasPath(phonePath)).isFalse();
    assertThat(resultApplicantData.getFailedUpdates())
        .isEqualTo(
            ImmutableMap.of(
                datePath,
                "invalid_date_input",
                currencyPath,
                "invalid_currency_input",
                numberPath,
                "invalid_number_input",
                phonePath,
                "invalid_phone_input"));

    ApplicantData freshApplicantDataAfter =
        userRepository.lookupApplicantSync(applicant.id).get().getApplicantData();
    assertThat(freshApplicantDataAfter.hasPath(datePath)).isFalse();
    assertThat(freshApplicantDataAfter.hasPath(currencyPath)).isFalse();
    assertThat(freshApplicantDataAfter.hasPath(numberPath)).isFalse();
    assertThat(resultApplicantData.hasPath(phonePath)).isFalse();
  }

  @Test
  public void
      stageAndUpdateIfValid_forEnumeratorBlock_putsMetadataWithEmptyUpdate_andCanPutRealRepeatedEntitiesInAfter() {
    // We make the question optional since it's not valid to stage empty updates
    createProgramWithOptionalQuestion(
        testQuestionBank.applicantHouseholdMembers().getQuestionDefinition());
    Applicant applicant = subject.createApplicant().toCompletableFuture().join();
    Path enumeratorPath =
        ApplicantData.APPLICANT_PATH.join(
            testQuestionBank
                .applicantHouseholdMembers()
                .getQuestionDefinition()
                .getQuestionPathSegment());

    // Empty update should put metadata in
    ImmutableMap<String, String> updates = ImmutableMap.of();
    subject
        .stageAndUpdateIfValid(applicant.id, programDefinition.id(), "1", updates, false)
        .toCompletableFuture()
        .join();
    ApplicantData applicantDataMiddle =
        userRepository.lookupApplicantSync(applicant.id).get().getApplicantData();
    assertThat(
            applicantDataMiddle.readLong(
                enumeratorPath.withoutArrayReference().join(Scalar.PROGRAM_UPDATED_IN)))
        .contains(programDefinition.id());

    // Put enumerators in
    updates =
        ImmutableMap.of(
            enumeratorPath.atIndex(0).toString(), "first",
            enumeratorPath.atIndex(1).toString(), "second");
    subject
        .stageAndUpdateIfValid(applicant.id, programDefinition.id(), "1", updates, false)
        .toCompletableFuture()
        .join();
    ApplicantData applicantDataAfter =
        userRepository.lookupApplicantSync(applicant.id).get().getApplicantData();
    assertThat(applicantDataAfter.readRepeatedEntities(enumeratorPath)).hasSize(2);
    assertThat(applicantDataAfter.readString(enumeratorPath.atIndex(0).join(Scalar.ENTITY_NAME)))
        .contains("first");
    assertThat(
            applicantDataAfter.readLong(enumeratorPath.atIndex(0).join(Scalar.PROGRAM_UPDATED_IN)))
        .contains(programDefinition.id());
    assertThat(applicantDataAfter.readString(enumeratorPath.atIndex(1).join(Scalar.ENTITY_NAME)))
        .contains("second");
    assertThat(
            applicantDataAfter.readLong(enumeratorPath.atIndex(1).join(Scalar.PROGRAM_UPDATED_IN)))
        .contains(programDefinition.id());

    // Deleting should result in just having metadata again
    Path deletionPath = Path.empty().join(Scalar.DELETE_ENTITY).asArrayElement();
    updates =
        ImmutableMap.of(
            deletionPath.atIndex(0).toString(), "0",
            deletionPath.atIndex(1).toString(), "1");
    subject
        .stageAndUpdateIfValid(applicant.id, programDefinition.id(), "1", updates, false)
        .toCompletableFuture()
        .join();
    ApplicantData applicantDataAfterDeletion =
        userRepository.lookupApplicantSync(applicant.id).get().getApplicantData();
    assertThat(
            applicantDataAfterDeletion.readLong(
                enumeratorPath.withoutArrayReference().join(Scalar.PROGRAM_UPDATED_IN)))
        .contains(programDefinition.id());
  }

  @Test
  public void stageAndUpdateIfValid_forEnumeratorBlock_withEmptyUpdates_doesNotDeleteRealData() {
    programDefinition =
        ProgramBuilder.newActiveProgram("test program", "desc")
            .withBlock()
            .withRequiredQuestion(testQuestionBank.applicantHouseholdMembers())
            .buildDefinition();
    Applicant applicant = subject.createApplicant().toCompletableFuture().join();
    Path enumeratorPath =
        ApplicantData.APPLICANT_PATH.join(
            testQuestionBank
                .applicantHouseholdMembers()
                .getQuestionDefinition()
                .getQuestionPathSegment());

    // Put enumerators in
    ImmutableMap<String, String> updates =
        ImmutableMap.of(
            enumeratorPath.atIndex(0).toString(), "first",
            enumeratorPath.atIndex(1).toString(), "second");
    subject
        .stageAndUpdateIfValid(applicant.id, programDefinition.id(), "1", updates, false)
        .toCompletableFuture()
        .join();
    ApplicantData applicantDataBefore =
        userRepository.lookupApplicantSync(applicant.id).get().getApplicantData();
    assertThat(applicantDataBefore.readRepeatedEntities(enumeratorPath)).hasSize(2);
    assertThat(applicantDataBefore.readString(enumeratorPath.atIndex(0).join(Scalar.ENTITY_NAME)))
        .contains("first");
    assertThat(
            applicantDataBefore.readLong(enumeratorPath.atIndex(0).join(Scalar.PROGRAM_UPDATED_IN)))
        .contains(programDefinition.id());
    assertThat(applicantDataBefore.readString(enumeratorPath.atIndex(1).join(Scalar.ENTITY_NAME)))
        .contains("second");
    assertThat(
            applicantDataBefore.readLong(enumeratorPath.atIndex(1).join(Scalar.PROGRAM_UPDATED_IN)))
        .contains(programDefinition.id());

    // Empty update SHOULD NOT DELETE enumerator data. This case shouldn't normally happen in
    // normal flow of code, but just a sanity check that data for repeated entities won't just
    // get deleted.
    updates = ImmutableMap.of();
    subject
        .stageAndUpdateIfValid(applicant.id, programDefinition.id(), "1", updates, false)
        .toCompletableFuture()
        .join();
    ApplicantData applicantDataAfter =
        userRepository.lookupApplicantSync(applicant.id).get().getApplicantData();
    assertThat(applicantDataAfter.readRepeatedEntities(enumeratorPath)).hasSize(2);
    assertThat(applicantDataAfter.readString(enumeratorPath.atIndex(0).join(Scalar.ENTITY_NAME)))
        .contains("first");
    assertThat(
            applicantDataAfter.readLong(enumeratorPath.atIndex(0).join(Scalar.PROGRAM_UPDATED_IN)))
        .contains(programDefinition.id());
    assertThat(applicantDataAfter.readString(enumeratorPath.atIndex(1).join(Scalar.ENTITY_NAME)))
        .contains("second");
    assertThat(
            applicantDataAfter.readLong(enumeratorPath.atIndex(1).join(Scalar.PROGRAM_UPDATED_IN)))
        .contains(programDefinition.id());
  }

  public ImmutableMap<String, String> applicationUpdates() {
    return applicationUpdates("Alice", "Doe");
  }

  public ImmutableMap<String, String> applicationUpdates(String first, String last) {
    return ImmutableMap.<String, String>builder()
        .put(Path.create("applicant.name").join(Scalar.FIRST_NAME).toString(), first)
        .put(Path.create("applicant.name").join(Scalar.LAST_NAME).toString(), last)
        .build();
  }

  @Test
  public void stageAndUpdateIfValid_withUpdates_isOk() {
    Applicant applicant = subject.createApplicant().toCompletableFuture().join();

    subject
        .stageAndUpdateIfValid(
            applicant.id, programDefinition.id(), "1", applicationUpdates(), false)
        .toCompletableFuture()
        .join();

    ApplicantData applicantDataAfter =
        userRepository.lookupApplicantSync(applicant.id).get().getApplicantData();

    assertThat(applicantDataAfter.asJsonString()).contains("Alice", "Doe");
  }

  @Test
  public void stageAndUpdateIfValid_updatesMetadataForQuestionOnce() {
    Applicant applicant = subject.createApplicant().toCompletableFuture().join();

    subject
        .stageAndUpdateIfValid(
            applicant.id, programDefinition.id(), "1", applicationUpdates(), false)
        .toCompletableFuture()
        .join();

    ApplicantData applicantDataAfter =
        userRepository.lookupApplicantSync(applicant.id).get().getApplicantData();

    Path programIdPath = Path.create("applicant.name").join(Scalar.PROGRAM_UPDATED_IN);
    Path timestampPath = Path.create("applicant.name").join(Scalar.UPDATED_AT);
    assertThat(applicantDataAfter.readLong(programIdPath)).hasValue(programDefinition.id());
    assertThat(applicantDataAfter.readLong(timestampPath)).isPresent();
  }

  @Test
  public void stageAndUpdateIfValid_updatesContainMultiSelectAnswers_isOk() {
    QuestionDefinitionConfig config =
        QuestionDefinitionConfig.builder()
            .setName("checkbox")
            .setDescription("description")
            .setQuestionText(LocalizedStrings.of(Locale.US, "question?"))
            .setQuestionHelpText(LocalizedStrings.of(Locale.US, "help text"))
            .build();

    ImmutableList<QuestionOption> questionOptions =
        ImmutableList.of(
            QuestionOption.create(1L, LocalizedStrings.of(Locale.US, "cat")),
            QuestionOption.create(2L, LocalizedStrings.of(Locale.US, "dog")),
            QuestionOption.create(3L, LocalizedStrings.of(Locale.US, "horse")));
    QuestionDefinition multiSelectQuestion =
        questionService
            .create(
                new MultiOptionQuestionDefinition(
                    config, questionOptions, MultiOptionQuestionType.CHECKBOX))
            .getResult();

    // We make the question optional since it's not valid to stage empty updates
    createProgramWithOptionalQuestion(multiSelectQuestion);

    Applicant applicant = subject.createApplicant().toCompletableFuture().join();

    Path checkboxPath = Path.create("applicant.checkbox").join(Scalar.SELECTIONS).asArrayElement();
    ImmutableMap<String, String> updates =
        ImmutableMap.<String, String>builder()
            .put(checkboxPath.atIndex(0).toString(), "1")
            .put(checkboxPath.atIndex(1).toString(), "2")
            .put(checkboxPath.atIndex(2).toString(), "3")
            .build();

    subject
        .stageAndUpdateIfValid(applicant.id, programDefinition.id(), "1", updates, false)
        .toCompletableFuture()
        .join();

    ApplicantData applicantDataAfter =
        userRepository.lookupApplicantSync(applicant.id).get().getApplicantData();

    assertThat(
            applicantDataAfter.readLongList(
                Path.create("applicant.checkbox").join(Scalar.SELECTIONS)))
        .hasValue(ImmutableList.of(1L, 2L, 3L));

    // Ensure that we can successfully overwrite the array.
    updates =
        ImmutableMap.<String, String>builder()
            .put(checkboxPath.atIndex(0).toString(), "3")
            .put(checkboxPath.atIndex(1).toString(), "1")
            .build();
    subject
        .stageAndUpdateIfValid(applicant.id, programDefinition.id(), "1", updates, false)
        .toCompletableFuture()
        .join();

    applicantDataAfter = userRepository.lookupApplicantSync(applicant.id).get().getApplicantData();

    assertThat(
            applicantDataAfter.readLongList(
                Path.create("applicant.checkbox").join(Scalar.SELECTIONS)))
        .hasValue(ImmutableList.of(3L, 1L));

    // Clear values by sending an empty item.
    updates =
        ImmutableMap.<String, String>builder().put(checkboxPath.atIndex(0).toString(), "").build();
    subject
        .stageAndUpdateIfValid(applicant.id, programDefinition.id(), "1", updates, false)
        .toCompletableFuture()
        .join();

    applicantDataAfter = userRepository.lookupApplicantSync(applicant.id).get().getApplicantData();

    assertThat(
            applicantDataAfter.readLongList(
                Path.create("applicant.checkbox").join(Scalar.SELECTIONS)))
        .isEmpty();
  }

  @Test
  public void stageAndUpdateIfValid_withEnumeratorChangesAndDeletes_isOk() {
    QuestionDefinition enumeratorQuestionDefinition =
        testQuestionBank.applicantHouseholdMembers().getQuestionDefinition();
    createProgram(enumeratorQuestionDefinition);

    Applicant applicant = subject.createApplicant().toCompletableFuture().join();

    Path enumeratorPath =
        ApplicantData.APPLICANT_PATH.join(enumeratorQuestionDefinition.getQuestionPathSegment());
    Path deletionPath = Path.empty().join(Scalar.DELETE_ENTITY).asArrayElement();
    ImmutableMap<String, String> updates =
        ImmutableMap.of(
            enumeratorPath.atIndex(0).toString(), "first",
            enumeratorPath.atIndex(1).toString(), "second",
            enumeratorPath.atIndex(2).toString(), "third",
            deletionPath.atIndex(0).toString(), "2",
            deletionPath.atIndex(1).toString(), "0");

    subject
        .stageAndUpdateIfValid(applicant.id, programDefinition.id(), "1", updates, false)
        .toCompletableFuture()
        .join();

    ApplicantData applicantDataAfter =
        userRepository.lookupApplicantSync(applicant.id).get().getApplicantData();

    assertThat(applicantDataAfter.readRepeatedEntities(enumeratorPath)).containsExactly("second");
  }

  @Test
  public void stageAndUpdateIfValid_enumeratorNotAnswered_stillWritesPathToApplicantData() {
    QuestionDefinition enumeratorQuestionDefinition =
        testQuestionBank.applicantHouseholdMembers().getQuestionDefinition();

    // We make the question optional since it's not valid to stage empty updates
    createProgramWithOptionalQuestion(enumeratorQuestionDefinition);

    Applicant applicant = subject.createApplicant().toCompletableFuture().join();

    Path enumeratorPath =
        ApplicantData.APPLICANT_PATH.join(enumeratorQuestionDefinition.getQuestionPathSegment());
    ImmutableMap<String, String> updates = ImmutableMap.of();

    subject
        .stageAndUpdateIfValid(applicant.id, programDefinition.id(), "1", updates, false)
        .toCompletableFuture()
        .join();

    ApplicantData applicantDataAfter =
        userRepository.lookupApplicantSync(applicant.id).get().getApplicantData();

    assertThat(applicantDataAfter.hasPath(enumeratorPath.withoutArrayReference())).isTrue();
    assertThat(applicantDataAfter.readRepeatedEntities(enumeratorPath)).isEmpty();
  }

  @Test
  public void stageAndUpdateIfValid_hasApplicantNotFoundException() {
    ImmutableMap<String, String> updates = ImmutableMap.of();
    long badApplicantId = 1L;

    assertThatExceptionOfType(CompletionException.class)
        .isThrownBy(
            () ->
                subject
                    .stageAndUpdateIfValid(
                        badApplicantId, programDefinition.id(), "1", updates, false)
                    .toCompletableFuture()
                    .join())
        .withCauseInstanceOf(ApplicantNotFoundException.class)
        .withMessageContaining("Applicant not found for ID 1");
  }

  @Test
  public void stageAndUpdateIfValid_hasProgramNotFoundException() {
    Applicant applicant = subject.createApplicant().toCompletableFuture().join();
    ImmutableMap<String, String> updates = ImmutableMap.of();
    long badProgramId = programDefinition.id() + 1000L;

    Throwable thrown =
        catchThrowable(
            () ->
                subject
                    .stageAndUpdateIfValid(applicant.id, badProgramId, "1", updates, false)
                    .toCompletableFuture()
                    .join());

    assertThat(thrown).isInstanceOf(CompletionException.class);
    assertThat(thrown).hasRootCauseInstanceOf(ProgramNotFoundException.class);
  }

  @Test
  public void stageAndUpdateIfValid_hasProgramBlockNotFoundException() {
    Applicant applicant = subject.createApplicant().toCompletableFuture().join();
    ImmutableMap<String, String> updates = ImmutableMap.of();
    String badBlockId = "100";

    Throwable thrown =
        catchThrowable(
            () ->
                subject
                    .stageAndUpdateIfValid(
                        applicant.id, programDefinition.id(), badBlockId, updates, false)
                    .toCompletableFuture()
                    .join());

    assertThat(thrown).isInstanceOf(CompletionException.class);
    assertThat(thrown).hasCauseInstanceOf(ProgramBlockNotFoundException.class);
  }

  @Test
  public void stageAndUpdateIfValid_hasPathNotInBlockException() {
    Applicant applicant = subject.createApplicant().toCompletableFuture().join();
    ImmutableMap<String, String> updates =
        ImmutableMap.of(
            Path.create("applicant.name.first").toString(), "Alice",
            Path.create("this.is.not.in.block").toString(), "Doe");

    Throwable thrown =
        catchThrowable(
            () ->
                subject
                    .stageAndUpdateIfValid(
                        applicant.id, programDefinition.id(), "1", updates, false)
                    .toCompletableFuture()
                    .join());

    assertThat(thrown).isInstanceOf(CompletionException.class);
    assertThat(thrown).hasCauseInstanceOf(PathNotInBlockException.class);
  }

  @Test
  public void stageAndUpdateIfValid_hasIllegalArgumentExceptionForReservedMetadataScalarKeys() {
    Applicant applicant = subject.createApplicant().toCompletableFuture().join();
    String reservedScalar = Path.create("applicant.name").join(Scalar.UPDATED_AT).toString();
    ImmutableMap<String, String> updates = ImmutableMap.of(reservedScalar, "12345");

    assertThatExceptionOfType(CompletionException.class)
        .isThrownBy(
            () ->
                subject
                    .stageAndUpdateIfValid(
                        applicant.id, programDefinition.id(), "1", updates, false)
                    .toCompletableFuture()
                    .join())
        .withCauseInstanceOf(IllegalArgumentException.class)
        .withMessageContaining("Path contained reserved scalar key");
  }

  @Test
  public void
      stageAndUpdateIfValid_withIllegalArrayElement_hasIllegalArgumentExceptionForReservedMetadataScalarKeys() {
    Applicant applicant = subject.createApplicant().toCompletableFuture().join();
    String reservedScalar =
        Path.create("applicant.name")
            .join(Scalar.UPDATED_AT)
            .asArrayElement()
            .atIndex(0)
            .toString();
    ImmutableMap<String, String> updates = ImmutableMap.of(reservedScalar, "12345");

    assertThatExceptionOfType(CompletionException.class)
        .isThrownBy(
            () ->
                subject
                    .stageAndUpdateIfValid(
                        applicant.id, programDefinition.id(), "1", updates, false)
                    .toCompletableFuture()
                    .join())
        .withCauseInstanceOf(IllegalArgumentException.class)
        .withMessageContaining("Path contained reserved scalar key");
  }

  @Test
  public void createApplicant_createsANewApplicant() {
    Applicant applicant = subject.createApplicant().toCompletableFuture().join();

    assertThat(applicant.id).isNotNull();
  }

  @Test
  public void createApplicant_ApplicantTime() {
    Applicant applicant = subject.createApplicant().toCompletableFuture().join();

    Instant t = Instant.now();

    assertThat(applicant.getWhenCreated()).isNotNull();
    assertThat(applicant.getWhenCreated()).isBefore(t);
  }

  @Test
  public void getReadOnlyApplicantService_getsReadOnlyApplicantServiceForTheApplicantAndProgram() {
    Applicant applicant = subject.createApplicant().toCompletableFuture().join();

    ReadOnlyApplicantProgramService roApplicantProgramService =
        subject
            .getReadOnlyApplicantProgramService(applicant.id, programDefinition.id())
            .toCompletableFuture()
            .join();

    assertThat(roApplicantProgramService).isInstanceOf(ReadOnlyApplicantProgramService.class);
  }

  @Test
  public void submitApplication_returnsSavedApplication() {
    Applicant applicant = subject.createApplicant().toCompletableFuture().join();
    applicant.setAccount(resourceCreator.insertAccount());
    applicant.save();

    subject
        .stageAndUpdateIfValid(
            applicant.id, programDefinition.id(), "1", applicationUpdates(), false)
        .toCompletableFuture()
        .join();

    Application application =
        subject
            .submitApplication(applicant.id, programDefinition.id(), trustedIntermediaryProfile)
            .toCompletableFuture()
            .join();

    assertThat(application.getApplicant()).isEqualTo(applicant);
    assertThat(application.getProgram().getProgramDefinition().id())
        .isEqualTo(programDefinition.id());
    assertThat(application.getLifecycleStage()).isEqualTo(LifecycleStage.ACTIVE);
    assertThat(application.getApplicantData().asJsonString()).contains("Alice", "Doe");
  }

  @Test
  public void submitApplication_addsProgramToStoredFileAcls() {
    Applicant applicant = subject.createApplicant().toCompletableFuture().join();
    applicant.setAccount(resourceCreator.insertAccount());
    applicant.save();

    var fileKey = "test-file-key";

    ImmutableMap<String, String> updates =
        ImmutableMap.<String, String>builder()
            .put(Path.create("applicant.fileupload").join(Scalar.FILE_KEY).toString(), fileKey)
            .build();

    var fileUploadQuestion =
        questionService
            .create(
                new FileUploadQuestionDefinition(
                    QuestionDefinitionConfig.builder()
                        .setName("fileupload")
                        .setDescription("description")
                        .setQuestionText(LocalizedStrings.of(Locale.US, "question?"))
                        .setQuestionHelpText(LocalizedStrings.of(Locale.US, "help text"))
                        .build()))
            .getResult();

    Program firstProgram =
        ProgramBuilder.newActiveProgram("first test program", "desc")
            .withBlock()
            .withRequiredQuestionDefinitions(ImmutableList.of(fileUploadQuestion))
            .build();
    firstProgram.save();

    Program secondProgram =
        ProgramBuilder.newActiveProgram("second test program", "desc")
            .withBlock()
            .withRequiredQuestionDefinitions(ImmutableList.of(fileUploadQuestion))
            .build();
    secondProgram.save();

    subject
        .stageAndUpdateIfValid(applicant.id, firstProgram.id, "1", updates, false)
        .toCompletableFuture()
        .join();

    var storedFile = new StoredFile().setName(fileKey);
    storedFile.save();

    subject
        .submitApplication(applicant.id, firstProgram.id, trustedIntermediaryProfile)
        .toCompletableFuture()
        .join();

    storedFile.refresh();
    assertThat(storedFile.getAcls().getProgramReadAcls())
        .containsOnly(firstProgram.getProgramDefinition().adminName());

    subject
        .submitApplication(applicant.id, secondProgram.id, trustedIntermediaryProfile)
        .toCompletableFuture()
        .join();

    storedFile.refresh();
    assertThat(storedFile.getAcls().getProgramReadAcls())
        .containsOnly(
            firstProgram.getProgramDefinition().adminName(),
            secondProgram.getProgramDefinition().adminName());
  }

  @Test
  public void submitApplication_obsoletesOldApplication() {
    Applicant applicant = subject.createApplicant().toCompletableFuture().join();
    applicant.setAccount(resourceCreator.insertAccount());
    applicant.save();

    subject
        .stageAndUpdateIfValid(
            applicant.id, programDefinition.id(), "1", applicationUpdates(), false)
        .toCompletableFuture()
        .join();

    Application oldApplication =
        subject
            .submitApplication(applicant.id, programDefinition.id(), trustedIntermediaryProfile)
            .toCompletableFuture()
            .join();

    subject
        .stageAndUpdateIfValid(
            applicant.id, programDefinition.id(), "1", applicationUpdates("Bob", "Elisa"), false)
        .toCompletableFuture()
        .join();

    Application newApplication =
        subject
            .submitApplication(applicant.id, programDefinition.id(), trustedIntermediaryProfile)
            .toCompletableFuture()
            .join();

    oldApplication.refresh();
    assertThat(oldApplication.getApplicant()).isEqualTo(applicant);
    assertThat(oldApplication.getProgram().getProgramDefinition().id())
        .isEqualTo(programDefinition.id());
    assertThat(oldApplication.getLifecycleStage()).isEqualTo(LifecycleStage.OBSOLETE);
    assertThat(oldApplication.getApplicantData().asJsonString()).contains("Alice", "Doe");

    assertThat(newApplication.getApplicant()).isEqualTo(applicant);
    assertThat(newApplication.getProgram().getProgramDefinition().id())
        .isEqualTo(programDefinition.id());
    assertThat(newApplication.getLifecycleStage()).isEqualTo(LifecycleStage.ACTIVE);
    assertThat(newApplication.getApplicantData().asJsonString()).contains("Bob", "Elisa");
  }

  @Test
  public void submitApplication_setsStatusToDefault() {
    StatusDefinitions.Status status =
        APPROVED_STATUS.toBuilder().setDefaultStatus(Optional.of(true)).build();
    createProgramWithStatusDefinitions(new StatusDefinitions(ImmutableList.of(status)));

    Applicant applicant = subject.createApplicant().toCompletableFuture().join();
    applicant.setAccount(resourceCreator.insertAccount());
    applicant.save();
    subject
        .stageAndUpdateIfValid(
            applicant.id, programDefinition.id(), "1", applicationUpdates(), false)
        .toCompletableFuture()
        .join();

    Application application =
        subject
            .submitApplication(applicant.id, programDefinition.id(), trustedIntermediaryProfile)
            .toCompletableFuture()
            .join();
    application.refresh();

    assertThat(application.getLatestStatus().get()).isEqualTo("Approved");
    assertThat(application.getApplicationEvents().size()).isEqualTo(1);
    ApplicationEvent event = application.getApplicationEvents().get(0);
    assertThat(event.getEventType().name()).isEqualTo("STATUS_CHANGE");
    assertThat(event.getDetails().statusEvent()).isNotEmpty();
    assertThat(event.getDetails().statusEvent().get().statusText()).isEqualTo("Approved");
    assertThat(event.getDetails().statusEvent().get().emailSent()).isEqualTo(true);
  }

  @Test
  public void submitApplication_sendsEmailsWithoutDefaultStatus() {
    Account admin = resourceCreator.insertAccountWithEmail("admin@example.com");
    admin.addAdministeredProgram(programDefinition);
    admin.save();

    Applicant applicant = subject.createApplicant().toCompletableFuture().join();
    applicant.setAccount(resourceCreator.insertAccountWithEmail("user1@example.com"));
    applicant.save();

    subject
        .stageAndUpdateIfValid(
            applicant.id, programDefinition.id(), "1", applicationUpdates(), false)
        .toCompletableFuture()
        .join();

    Application application =
        subject
            .submitApplication(applicant.id, programDefinition.id(), trustedIntermediaryProfile)
            .toCompletableFuture()
            .join();
    application.refresh();

    Messages messages = getMessages(Locale.US);
    String programName = programDefinition.adminName();

    // Program admin email
    Mockito.verify(amazonSESClient)
        .send(
            ImmutableList.of("admin@example.com"),
            String.format("New application %d submitted", application.id),
            String.format(
                "Applicant %d submitted a new application %d to program %s.\n"
                    + "View the application at %s.",
                applicant.id,
                application.id,
                programName,
                baseUrl
                    + String.format(
                        "/admin/programs/%d/applications/%d",
                        programDefinition.id(), application.id)));
    // TI email
    Mockito.verify(amazonSESClient)
        .send(
            "test@example.com",
            messages.at(
                MessageKey.EMAIL_TI_APPLICATION_SUBMITTED_SUBJECT.getKeyName(),
                programName,
                applicant.id),
            String.format(
                "%s\n%s",
                messages.at(
                    MessageKey.EMAIL_TI_APPLICATION_SUBMITTED_BODY.getKeyName(),
                    programName,
                    applicant.id,
                    application.id),
                messages.at(
                    MessageKey.EMAIL_TI_MANAGE_YOUR_CLIENTS.getKeyName(),
                    baseUrl + "/admin/tiDash?page=1")));

    // Applicant email
    Mockito.verify(amazonSESClient)
        .send(
            "user1@example.com",
            messages.at(MessageKey.EMAIL_APPLICATION_RECEIVED_SUBJECT.getKeyName(), programName),
            String.format(
                "%s\n%s",
                messages.at(
                    MessageKey.EMAIL_APPLICATION_RECEIVED_BODY.getKeyName(),
                    programName,
                    applicant.id,
                    application.id),
                messages.at(MessageKey.EMAIL_LOGIN_TO_CIVIFORM.getKeyName(), baseUrl)));
  }

  @Test
  public void submitApplication_sendsEmailsWithDefaultStatus() {
    StatusDefinitions.Status status =
        APPROVED_STATUS.toBuilder().setDefaultStatus(Optional.of(true)).build();
    createProgramWithStatusDefinitions(new StatusDefinitions(ImmutableList.of(status)));

    Account admin = resourceCreator.insertAccountWithEmail("admin@example.com");
    admin.addAdministeredProgram(programDefinition);
    admin.save();

    Applicant applicant = subject.createApplicant().toCompletableFuture().join();
    applicant.setAccount(resourceCreator.insertAccountWithEmail("user1@example.com"));
    applicant.save();
    subject
        .stageAndUpdateIfValid(
            applicant.id, programDefinition.id(), "1", applicationUpdates(), false)
        .toCompletableFuture()
        .join();

    Application application =
        subject
            .submitApplication(applicant.id, programDefinition.id(), trustedIntermediaryProfile)
            .toCompletableFuture()
            .join();
    application.refresh();

    Messages messages = getMessages(Locale.US);
    String programName = programDefinition.adminName();

    // Program admin email
    Mockito.verify(amazonSESClient)
        .send(
            ImmutableList.of("admin@example.com"),
            String.format("New application %d submitted", application.id),
            String.format(
                "Applicant %d submitted a new application %d to program %s.\n"
                    + "View the application at %s.",
                applicant.id,
                application.id,
                programName,
                baseUrl
                    + String.format(
                        "/admin/programs/%d/applications/%d",
                        programDefinition.id(), application.id)));
    // TI email
    Mockito.verify(amazonSESClient)
        .send(
            "test@example.com",
            messages.at(
                MessageKey.EMAIL_TI_APPLICATION_SUBMITTED_SUBJECT.getKeyName(),
                programName,
                applicant.id),
            String.format(
                "%s\n%s",
                "I'm a US email!",
                messages.at(
                    MessageKey.EMAIL_TI_MANAGE_YOUR_CLIENTS.getKeyName(),
                    baseUrl + "/admin/tiDash?page=1")));

    // Applicant email
    Mockito.verify(amazonSESClient)
        .send(
            "user1@example.com",
            messages.at(MessageKey.EMAIL_APPLICATION_RECEIVED_SUBJECT.getKeyName(), programName),
            "I'm a US email!\n"
                + messages.at(MessageKey.EMAIL_LOGIN_TO_CIVIFORM.getKeyName(), baseUrl));
  }

  @Test
  public void submitApplication_sendsLocalizedTIEmail() {
    StatusDefinitions.Status status =
        APPROVED_STATUS.toBuilder().setDefaultStatus(Optional.of(true)).build();
    createProgramWithStatusDefinitions(new StatusDefinitions(ImmutableList.of(status)));

    Account tiAccount = resourceCreator.insertAccountWithEmail("ti@example.com");
    Applicant tiApplicant = subject.createApplicant().toCompletableFuture().join();
    tiApplicant.setAccount(tiAccount);
    tiApplicant.getApplicantData().setPreferredLocale(Locale.KOREA);
    tiApplicant.save();
    Mockito.when(trustedIntermediaryProfile.getAccount())
        .thenReturn(CompletableFuture.completedFuture(tiAccount));

    Applicant applicant = subject.createApplicant().toCompletableFuture().join();
    applicant.setAccount(resourceCreator.insertAccountWithEmail("user2@example.com"));
    applicant.save();

    subject
        .stageAndUpdateIfValid(
            applicant.id, programDefinition.id(), "1", applicationUpdates(), false)
        .toCompletableFuture()
        .join();

    Application application =
        subject
            .submitApplication(applicant.id, programDefinition.id(), trustedIntermediaryProfile)
            .toCompletableFuture()
            .join();
    application.refresh();

    Messages koMessages = getMessages(Locale.KOREA);
    Messages enMessages = getMessages(Locale.US);
    String programName = programDefinition.adminName();

    // TI email
    Mockito.verify(amazonSESClient)
        .send(
            "ti@example.com",
            koMessages.at(
                MessageKey.EMAIL_TI_APPLICATION_SUBMITTED_SUBJECT.getKeyName(),
                programName,
                applicant.id),
            String.format(
                "%s\n%s",
                "I'm a KOREAN email!",
                koMessages.at(
                    MessageKey.EMAIL_TI_MANAGE_YOUR_CLIENTS.getKeyName(),
                    baseUrl + "/admin/tiDash?page=1")));

    // Applicant email
    Mockito.verify(amazonSESClient)
        .send(
            "user2@example.com",
            enMessages.at(MessageKey.EMAIL_APPLICATION_RECEIVED_SUBJECT.getKeyName(), programName),
            "I'm a US email!\n"
                + enMessages.at(MessageKey.EMAIL_LOGIN_TO_CIVIFORM.getKeyName(), baseUrl));
  }

  @Test
  public void submitApplication_sendsLocalizedDefaultStatusEmail() {
    StatusDefinitions.Status status =
        APPROVED_STATUS.toBuilder().setDefaultStatus(Optional.of(true)).build();
    createProgramWithStatusDefinitions(new StatusDefinitions(ImmutableList.of(status)));

    CiviFormProfile applicantProfile = Mockito.mock(CiviFormProfile.class);
    Account account = resourceCreator.insertAccountWithEmail("user3@example.com");
    Mockito.when(applicantProfile.isTrustedIntermediary()).thenReturn(false);
    Mockito.when(applicantProfile.getAccount())
        .thenReturn(CompletableFuture.completedFuture(account));

    Applicant applicant = subject.createApplicant().toCompletableFuture().join();
    applicant.setAccount(account);
    applicant.getApplicantData().setPreferredLocale(Locale.KOREA);
    applicant.save();

    subject
        .stageAndUpdateIfValid(
            applicant.id, programDefinition.id(), "1", applicationUpdates(), false)
        .toCompletableFuture()
        .join();

    Application application =
        subject
            .submitApplication(applicant.id, programDefinition.id(), applicantProfile)
            .toCompletableFuture()
            .join();
    application.refresh();

    Messages messages = getMessages(Locale.KOREA);
    String programName = programDefinition.adminName();

    // Applicant email
    Mockito.verify(amazonSESClient)
        .send(
            "user3@example.com",
            messages.at(MessageKey.EMAIL_APPLICATION_RECEIVED_SUBJECT.getKeyName(), programName),
            "I'm a KOREAN email!\n"
                + messages.at(MessageKey.EMAIL_LOGIN_TO_CIVIFORM.getKeyName(), baseUrl));
  }

  @Test
  public void submitApplication_doesNotChangeStatusWhenNoDefaultStatus() {
    StatusDefinitions.Status status =
        StatusDefinitions.Status.builder()
            .setStatusText("Waiting")
            .setLocalizedStatusText(LocalizedStrings.withDefaultValue("Waiting"))
            .setDefaultStatus(Optional.of(false))
            .build();
    createProgramWithStatusDefinitions(new StatusDefinitions(ImmutableList.of(status)));

    Applicant applicant = subject.createApplicant().toCompletableFuture().join();
    applicant.setAccount(resourceCreator.insertAccount());
    applicant.save();

    subject
        .stageAndUpdateIfValid(
            applicant.id, programDefinition.id(), "1", applicationUpdates(), false)
        .toCompletableFuture()
        .join();

    Application application =
        subject
            .submitApplication(applicant.id, programDefinition.id(), trustedIntermediaryProfile)
            .toCompletableFuture()
            .join();
    application.refresh();

    assertThat(application.getLatestStatus()).isEmpty();
    assertThat(application.getApplicationEvents().size()).isEqualTo(0);
  }

  @Test
  public void submitApplication_failsWithApplicationSubmissionException() {
    assertThatExceptionOfType(CompletionException.class)
        .isThrownBy(
            () ->
                subject
                    .submitApplication(9999L, 9999L, /* tiSubmitterEmail= */ Optional.empty())
                    .toCompletableFuture()
                    .join())
        .withCauseInstanceOf(ApplicationSubmissionException.class)
        .withMessageContaining("Application", "failed to save");
  }

  @Test
  public void submitApplication_failsWithApplicationNotEligibleException() {
    createProgramWithEligibility(questionDefinition);
    Applicant applicant = subject.createApplicant().toCompletableFuture().join();
    applicant.setAccount(resourceCreator.insertAccount());
    applicant.save();

    // First name is matched for eligibility.
    Path questionPath =
        ApplicantData.APPLICANT_PATH.join(questionDefinition.getQuestionPathSegment());
    ImmutableMap<String, String> updates =
        ImmutableMap.<String, String>builder()
            .put(questionPath.join(Scalar.FIRST_NAME).toString(), "Ineligible answer")
            .put(questionPath.join(Scalar.LAST_NAME).toString(), "irrelevant answer")
            .build();
    subject
        .stageAndUpdateIfValid(applicant.id, programDefinition.id(), "1", updates, false)
        .toCompletableFuture()
        .join();

    assertThatExceptionOfType(CompletionException.class)
        .isThrownBy(
            () ->
                subject
                    .submitApplication(
                        applicant.id, programDefinition.id(), trustedIntermediaryProfile)
                    .toCompletableFuture()
                    .join())
        .withCauseInstanceOf(ApplicationNotEligibleException.class)
        .withMessageContaining("Application", "failed to save");
  }

  @Test
  public void
      submitApplication_allowsIneligibleApplicationToBeSubmittedWhenEligibilityIsNongating() {
    createProgramWithNongatingEligibility(questionDefinition);
    Applicant applicant = subject.createApplicant().toCompletableFuture().join();
    applicant.setAccount(resourceCreator.insertAccount());
    applicant.save();

    // First name is matched for eligibility.
    Path questionPath =
        ApplicantData.APPLICANT_PATH.join(questionDefinition.getQuestionPathSegment());
    ImmutableMap<String, String> updates =
        ImmutableMap.<String, String>builder()
            .put(questionPath.join(Scalar.FIRST_NAME).toString(), "Ineligible answer")
            .put(questionPath.join(Scalar.LAST_NAME).toString(), "irrelevant answer")
            .build();
    subject
        .stageAndUpdateIfValid(applicant.id, programDefinition.id(), "1", updates, false)
        .toCompletableFuture()
        .join();

    Application application =
        subject
            .submitApplication(applicant.id, programDefinition.id(), trustedIntermediaryProfile)
            .toCompletableFuture()
            .join();

    assertThat(application.getApplicant()).isEqualTo(applicant);
    assertThat(application.getProgram().getProgramDefinition().id())
        .isEqualTo(programDefinition.id());
    assertThat(application.getLifecycleStage()).isEqualTo(LifecycleStage.ACTIVE);
  }

  @Test
  public void stageAndUpdateIfValid_with_correctedAddess_and_esriServiceAreaValidation() {
    QuestionDefinition addressQuestion =
        testQuestionBank.applicantAddress().getQuestionDefinition();
    EligibilityDefinition eligibilityDef =
        EligibilityDefinition.builder()
            .setPredicate(
                PredicateDefinition.create(
                    PredicateExpressionNode.create(
                        LeafAddressServiceAreaExpressionNode.create(
                            addressQuestion.getId(), "Seattle")),
                    PredicateAction.ELIGIBLE_BLOCK))
            .build();
    ProgramDefinition programDefinition =
        ProgramBuilder.newActiveProgram("test program", "desc")
            .withBlock()
            .withRequiredCorrectedAddressQuestion(testQuestionBank.applicantAddress())
            .withEligibilityDefinition(eligibilityDef)
            .buildDefinition();

    Applicant applicant = subject.createApplicant().toCompletableFuture().join();

    ImmutableMap<String, String> updates =
        ImmutableMap.<String, String>builder()
            .put(
                Path.create("applicant.applicant_address").join(Scalar.STREET).toString(),
                "Legit Address")
            .put(Path.create("applicant.applicant_address").join(Scalar.CITY).toString(), "City")
            .put(Path.create("applicant.applicant_address").join(Scalar.STATE).toString(), "State")
            .put(Path.create("applicant.applicant_address").join(Scalar.ZIP).toString(), "55555")
            .put(
                Path.create("applicant.applicant_address").join(Scalar.CORRECTED).toString(),
                CorrectedAddressState.CORRECTED.getSerializationFormat())
            .put(
                Path.create("applicant.applicant_address").join(Scalar.LATITUDE).toString(),
                "100.0")
            .put(
                Path.create("applicant.applicant_address").join(Scalar.LONGITUDE).toString(),
                "-100.0")
            .put(
                Path.create("applicant.applicant_address").join(Scalar.WELL_KNOWN_ID).toString(),
                "4326")
            .build();

    subject
        .stageAndUpdateIfValid(applicant.id, programDefinition.id(), "1", updates, true)
        .toCompletableFuture()
        .join();

    ApplicantData applicantDataAfter =
        userRepository.lookupApplicantSync(applicant.id).get().getApplicantData();

    assertThat(applicantDataAfter.asJsonString()).contains("Seattle_InArea_");
  }

  @Test
  public void
      stageAndUpdateIfValid_with_correctedAddess_and_esriServiceAreaValidation_with_existing_service_areas() {
    QuestionDefinition addressQuestion =
        testQuestionBank.applicantAddress().getQuestionDefinition();
    EligibilityDefinition eligibilityDef =
        EligibilityDefinition.builder()
            .setPredicate(
                PredicateDefinition.create(
                    PredicateExpressionNode.create(
                        LeafAddressServiceAreaExpressionNode.create(
                            addressQuestion.getId(), "Seattle")),
                    PredicateAction.ELIGIBLE_BLOCK))
            .build();
    ProgramDefinition programDefinition =
        ProgramBuilder.newActiveProgram("test program", "desc")
            .withBlock()
            .withRequiredCorrectedAddressQuestion(testQuestionBank.applicantAddress())
            .withEligibilityDefinition(eligibilityDef)
            .buildDefinition();

    Applicant applicant = subject.createApplicant().toCompletableFuture().join();

    ImmutableMap<String, String> updates =
        ImmutableMap.<String, String>builder()
            .put(
                Path.create("applicant.applicant_address").join(Scalar.STREET).toString(),
                "Legit Address")
            .put(Path.create("applicant.applicant_address").join(Scalar.CITY).toString(), "City")
            .put(Path.create("applicant.applicant_address").join(Scalar.STATE).toString(), "State")
            .put(Path.create("applicant.applicant_address").join(Scalar.ZIP).toString(), "55555")
            .put(
                Path.create("applicant.applicant_address").join(Scalar.CORRECTED).toString(),
                CorrectedAddressState.CORRECTED.getSerializationFormat())
            .put(
                Path.create("applicant.applicant_address").join(Scalar.LATITUDE).toString(),
                "100.0")
            .put(
                Path.create("applicant.applicant_address").join(Scalar.LONGITUDE).toString(),
                "-100.0")
            .put(
                Path.create("applicant.applicant_address").join(Scalar.WELL_KNOWN_ID).toString(),
                "4326")
            .put(
                Path.create("applicant.applicant_address").join(Scalar.SERVICE_AREA).toString(),
                "Bloomington_NotInArea_1234,Seattle_Failed_4567")
            .build();

    subject
        .stageAndUpdateIfValid(applicant.id, programDefinition.id(), "1", updates, true)
        .toCompletableFuture()
        .join();

    ApplicantData applicantDataAfter =
        userRepository.lookupApplicantSync(applicant.id).get().getApplicantData();

    assertThat(applicantDataAfter.asJsonString())
        .contains("Bloomington_NotInArea_1234", "Seattle_InArea_");
    assertThat(applicantDataAfter.asJsonString()).doesNotContain("Seattle_Failed_4567");
  }

  @Test
  public void submitApplication_failsWithApplicationOutOfDateException() {
    Applicant applicant = subject.createApplicant().toCompletableFuture().join();
    applicant.setAccount(resourceCreator.insertAccount());
    applicant.save();

    assertThatExceptionOfType(CompletionException.class)
        .isThrownBy(
            () ->
                subject
                    .submitApplication(
                        applicant.id, programDefinition.id(), trustedIntermediaryProfile)
                    .toCompletableFuture()
                    .join())
        .withCauseInstanceOf(ApplicationOutOfDateException.class)
        .withMessageContaining("Application", "failed to save");
  }

  @Test
  public void getPersonalInfo_applicantWithEmailAndName() {
    Applicant applicant = resourceCreator.insertApplicant();
    Account account = resourceCreator.insertAccountWithEmail("test@example.com");
    applicant.setAccount(account);
    applicant.getApplicantData().setUserName("Hello World");
    applicant.save();

    assertThat(subject.getPersonalInfo(applicant.id).toCompletableFuture().join())
        .isEqualTo(
            ApplicantPersonalInfo.ofLoggedInUser(
                Representation.builder()
                    .setEmail("test@example.com")
                    .setName("World, Hello")
                    .build()));
  }

  @Test
  public void getPersonalInfo_applicantWithEmailNoName() {
    Applicant applicant = resourceCreator.insertApplicant();
    Account account = resourceCreator.insertAccountWithEmail("test@example.com");
    applicant.setAccount(account);
    applicant.save();

    assertThat(subject.getPersonalInfo(applicant.id).toCompletableFuture().join())
        .isEqualTo(
            ApplicantPersonalInfo.ofLoggedInUser(
                Representation.builder().setEmail("test@example.com").build()));
  }

  @Test
  public void getPersonalInfo_applicantWithThreeNames() {
    Applicant applicant = resourceCreator.insertApplicant();
    applicant.getApplicantData().setUserName("First Middle Last");
    Account account = resourceCreator.insertAccountWithEmail("test@example.com");
    applicant.setAccount(account);
    applicant.save();

    assertThat(subject.getPersonalInfo(applicant.id).toCompletableFuture().join())
        .isEqualTo(
            ApplicantPersonalInfo.ofLoggedInUser(
                Representation.builder()
                    .setEmail("test@example.com")
                    .setName("Last, First")
                    .build()));
  }

  @Test
  public void getPersonalInfo_applicantWithManyNames() {
    Applicant applicant = resourceCreator.insertApplicant();
    applicant.getApplicantData().setUserName("First Second Third Fourth");
    Account account = resourceCreator.insertAccountWithEmail("test@example.com");
    applicant.setAccount(account);
    applicant.save();

    assertThat(subject.getPersonalInfo(applicant.id).toCompletableFuture().join())
        .isEqualTo(
            ApplicantPersonalInfo.ofLoggedInUser(
                Representation.builder()
                    .setEmail("test@example.com")
                    .setName("First Second Third Fourth")
                    .build()));
  }

  @Test
  public void getPersonalInfo_applicantNoAuthorityId_isGuest() {
    Applicant applicant = resourceCreator.insertApplicant();
    Account account = resourceCreator.insertAccount();
    applicant.setAccount(account);
    applicant.save();

    assertThat(subject.getPersonalInfo(applicant.id).toCompletableFuture().join())
        .isEqualTo(ApplicantPersonalInfo.ofGuestUser());
  }

  @Test
  public void getPersonalInfo_applicantNoAuthorityIdIsManaged_isTiPartiallyCreated() {
    Applicant applicant = resourceCreator.insertApplicant();
    Account account = resourceCreator.insertAccount();
    TrustedIntermediaryGroup group = resourceCreator.insertTrustedIntermediaryGroup();
    account.setManagedByGroup(group);
    applicant.setAccount(account);
    applicant.save();
    account.save();

    assertThat(subject.getPersonalInfo(applicant.id).toCompletableFuture().join())
        .isEqualTo(ApplicantPersonalInfo.ofTiPartiallyCreated(Representation.builder().build()));
  }

  @Test
  public void getPersonalInfo_invalidApplicantId_defaultsToGuest() {
    assertThat(subject.getPersonalInfo(9999L).toCompletableFuture().join())
        .isEqualTo(ApplicantPersonalInfo.ofGuestUser());
  }

  private Applicant createTestApplicant() {
    Applicant applicant = subject.createApplicant().toCompletableFuture().join();
    applicant.setAccount(resourceCreator.insertAccount());
    applicant.save();
    return applicant;
  }

  @Test
  public void relevantProgramsForApplicant() {
    Applicant applicant = createTestApplicant();
    Program commonIntakeForm =
        ProgramBuilder.newActiveCommonIntakeForm("common_intake_form")
            .withBlock()
            .withRequiredQuestion(testQuestionBank.applicantFavoriteColor())
            .build();
    Program programForDraft =
        ProgramBuilder.newActiveProgram("program_for_draft")
            .withBlock()
            .withRequiredQuestion(testQuestionBank.applicantName())
            .build();
    Program programForSubmitted =
        ProgramBuilder.newActiveProgram("program_for_submitted")
            .withBlock()
            .withRequiredQuestion(testQuestionBank.applicantFavoriteColor())
            .build();
    Program programForUnapplied =
        ProgramBuilder.newActiveProgram("program_for_unapplied").withBlock().build();

    applicationRepository
        .createOrUpdateDraft(applicant.id, programForDraft.id)
        .toCompletableFuture()
        .join();
    applicationRepository
        .submitApplication(applicant.id, programForSubmitted.id, Optional.empty())
        .toCompletableFuture()
        .join();

    ApplicantService.ApplicationPrograms result =
        subject
            .relevantProgramsForApplicant(applicant.id, trustedIntermediaryProfile)
            .toCompletableFuture()
            .join();

    assertThat(result.inProgress().stream().map(p -> p.program().id()))
        .containsExactly(programForDraft.id);
    assertThat(
            result.inProgress().stream()
                .map(ApplicantProgramData::latestSubmittedApplicationStatus))
        .containsExactly(Optional.empty());
    assertThat(result.submitted().stream().map(p -> p.program().id()))
        .containsExactly(programForSubmitted.id);
    assertThat(
            result.submitted().stream().map(ApplicantProgramData::latestSubmittedApplicationStatus))
        .containsExactly(Optional.empty());
    // programDefinition is the program created during test set up.
    assertThat(result.unapplied().stream().map(p -> p.program().id()))
        .containsExactlyInAnyOrder(programForUnapplied.id, programDefinition.id());
    assertThat(
            result.unapplied().stream().map(ApplicantProgramData::latestSubmittedApplicationStatus))
        .containsExactly(Optional.empty(), Optional.empty());
    assertThat(result.commonIntakeForm().isPresent()).isTrue();
    assertThat(result.commonIntakeForm().get().program().id()).isEqualTo(commonIntakeForm.id);
    assertThat(result.allPrograms())
        .containsExactlyInAnyOrder(
            result.commonIntakeForm().get(),
            result.inProgress().get(0),
            result.submitted().get(0),
            result.unapplied().get(0),
            result.unapplied().get(1));
  }

  @Test
  public void relevantProgramsForApplicant_noCommonIntakeForm() {
    Applicant applicant = createTestApplicant();
    Program programForDraft =
        ProgramBuilder.newActiveProgram("program_for_draft")
            .withBlock()
            .withRequiredQuestion(testQuestionBank.applicantName())
            .build();
    Program programForSubmitted =
        ProgramBuilder.newActiveProgram("program_for_submitted")
            .withBlock()
            .withRequiredQuestion(testQuestionBank.applicantFavoriteColor())
            .build();
    Program programForUnapplied =
        ProgramBuilder.newActiveProgram("program_for_unapplied").withBlock().build();

    applicationRepository
        .createOrUpdateDraft(applicant.id, programForDraft.id)
        .toCompletableFuture()
        .join();
    applicationRepository
        .submitApplication(applicant.id, programForSubmitted.id, Optional.empty())
        .toCompletableFuture()
        .join();

    ApplicantService.ApplicationPrograms result =
        subject
            .relevantProgramsForApplicant(applicant.id, trustedIntermediaryProfile)
            .toCompletableFuture()
            .join();

    assertThat(result.inProgress().stream().map(p -> p.program().id()))
        .containsExactly(programForDraft.id);
    assertThat(
            result.inProgress().stream()
                .map(ApplicantProgramData::latestSubmittedApplicationStatus))
        .containsExactly(Optional.empty());
    assertThat(result.submitted().stream().map(p -> p.program().id()))
        .containsExactly(programForSubmitted.id);
    assertThat(
            result.submitted().stream().map(ApplicantProgramData::latestSubmittedApplicationStatus))
        .containsExactly(Optional.empty());
    assertThat(result.unapplied().stream().map(p -> p.program().id()))
        .containsExactlyInAnyOrder(programForUnapplied.id, programDefinition.id());
    assertThat(
            result.unapplied().stream().map(ApplicantProgramData::latestSubmittedApplicationStatus))
        .containsExactly(Optional.empty(), Optional.empty());
    assertThat(result.commonIntakeForm().isPresent()).isFalse();
    assertThat(result.allPrograms())
        .containsExactlyInAnyOrder(
            result.inProgress().get(0),
            result.submitted().get(0),
            result.unapplied().get(0),
            result.unapplied().get(1));
  }

  @Test
  public void relevantProgramsForApplicant_commonIntakeFormHasCorrectLifecycleStage() {
    Applicant applicant = createTestApplicant();
    Program commonIntakeForm =
        ProgramBuilder.newActiveCommonIntakeForm("common_intake_form")
            .withBlock()
            .withRequiredQuestion(testQuestionBank.applicantFavoriteColor())
            .build();

    // No CIF application started.
    ApplicantService.ApplicationPrograms result =
        subject
            .relevantProgramsForApplicant(applicant.id, trustedIntermediaryProfile)
            .toCompletableFuture()
            .join();
    assertThat(result.inProgress()).isEmpty();
    assertThat(result.submitted()).isEmpty();
    assertThat(result.unapplied().stream().map(p -> p.program().id()))
        .containsExactlyInAnyOrder(programDefinition.id());
    assertThat(result.commonIntakeForm().isPresent()).isTrue();
    assertThat(result.commonIntakeForm().get().program().id()).isEqualTo(commonIntakeForm.id);
    assertThat(result.commonIntakeForm().get().latestApplicationLifecycleStage().isPresent())
        .isFalse();

    // CIF application in progress.
    applicationRepository
        .createOrUpdateDraft(applicant.id, commonIntakeForm.id)
        .toCompletableFuture()
        .join();
    result =
        subject
            .relevantProgramsForApplicant(applicant.id, trustedIntermediaryProfile)
            .toCompletableFuture()
            .join();
    assertThat(result.inProgress()).isEmpty();
    assertThat(result.submitted()).isEmpty();
    assertThat(result.unapplied().stream().map(p -> p.program().id()))
        .containsExactlyInAnyOrder(programDefinition.id());
    assertThat(result.commonIntakeForm().isPresent()).isTrue();
    assertThat(result.commonIntakeForm().get().program().id()).isEqualTo(commonIntakeForm.id);
    assertThat(result.commonIntakeForm().get().latestApplicationLifecycleStage().isPresent())
        .isTrue();
    assertThat(result.commonIntakeForm().get().latestApplicationLifecycleStage().get())
        .isEqualTo(LifecycleStage.DRAFT);

    // CIF application submitted.
    applicationRepository
        .submitApplication(applicant.id, commonIntakeForm.id, Optional.empty())
        .toCompletableFuture()
        .join();
    result =
        subject
            .relevantProgramsForApplicant(applicant.id, trustedIntermediaryProfile)
            .toCompletableFuture()
            .join();
    assertThat(result.inProgress()).isEmpty();
    assertThat(result.submitted()).isEmpty();
    assertThat(result.unapplied().stream().map(p -> p.program().id()))
        .containsExactlyInAnyOrder(programDefinition.id());
    assertThat(result.commonIntakeForm().isPresent()).isTrue();
    assertThat(result.commonIntakeForm().get().program().id()).isEqualTo(commonIntakeForm.id);
    assertThat(result.commonIntakeForm().get().latestApplicationLifecycleStage().isPresent())
        .isTrue();
    assertThat(result.commonIntakeForm().get().latestApplicationLifecycleStage().get())
        .isEqualTo(LifecycleStage.ACTIVE);
  }

  @Test
  public void relevantProgramsForApplicant_setsEligibility() {
    Applicant applicant = createTestApplicant();
    EligibilityDefinition eligibilityDef = createEligibilityDefinition(questionDefinition);
    Program programForDraft =
        ProgramBuilder.newDraftProgram("program_for_draft")
            .withBlock()
            .withRequiredQuestionDefinitions(ImmutableList.of(questionDefinition))
            .withEligibilityDefinition(eligibilityDef)
            .build();
    Program programForSubmitted =
        ProgramBuilder.newDraftProgram("program_for_submitted")
            .withBlock()
            .withRequiredQuestion(testQuestionBank.applicantFavoriteColor())
            .build();
    Program programForUnapplied =
        ProgramBuilder.newDraftProgram("program_for_unapplied").withBlock().build();
    versionRepository.publishNewSynchronizedVersion();

    applicationRepository
        .createOrUpdateDraft(applicant.id, programForDraft.id)
        .toCompletableFuture()
        .join();
    applicationRepository
        .submitApplication(applicant.id, programForSubmitted.id, Optional.empty())
        .toCompletableFuture()
        .join();

    ApplicantService.ApplicationPrograms result =
        subject
            .relevantProgramsForApplicant(applicant.id, trustedIntermediaryProfile)
            .toCompletableFuture()
            .join();

    assertThat(result.inProgress().stream().map(p -> p.program().id()))
        .containsExactly(programForDraft.id);
    assertThat(result.inProgress().stream().map(ApplicantProgramData::isProgramMaybeEligible))
        .containsExactly(Optional.of(true));
    assertThat(
            result.inProgress().stream()
                .map(ApplicantProgramData::latestSubmittedApplicationStatus))
        .containsExactly(Optional.empty());
    assertThat(result.submitted().stream().map(ApplicantProgramData::isProgramMaybeEligible))
        .containsExactly(Optional.empty());
    assertThat(result.submitted().stream().map(p -> p.program().id()))
        .containsExactly(programForSubmitted.id);
    assertThat(
            result.submitted().stream().map(ApplicantProgramData::latestSubmittedApplicationStatus))
        .containsExactly(Optional.empty());
    assertThat(result.unapplied().stream().map(p -> p.program().id()))
        .containsExactlyInAnyOrder(programForUnapplied.id, programDefinition.id());
    assertThat(result.unapplied().stream().map(ApplicantProgramData::isProgramMaybeEligible))
        .containsExactly(Optional.empty(), Optional.empty());
    assertThat(
            result.unapplied().stream().map(ApplicantProgramData::latestSubmittedApplicationStatus))
        .containsExactly(Optional.empty(), Optional.empty());
  }

  @Test
  public void relevantProgramsForApplicant_setsEligibilityOnMultipleApps() {
    Applicant applicant = createTestApplicant();
    EligibilityDefinition eligibilityDef = createEligibilityDefinition(questionDefinition);
    Program programForDraft =
        ProgramBuilder.newDraftProgram("program_for_draft")
            .withBlock()
            .withRequiredQuestionDefinitions(ImmutableList.of(questionDefinition))
            .withEligibilityDefinition(eligibilityDef)
            .build();
    Program programForSubmitted =
        ProgramBuilder.newDraftProgram("program_for_submitted")
            .withBlock()
            .withRequiredQuestion(testQuestionBank.applicantFavoriteColor())
            .build();
    Program programForUnapplied =
        ProgramBuilder.newDraftProgram("program_for_unapplied")
            .withBlock()
            .withRequiredQuestionDefinitions(ImmutableList.of(questionDefinition))
            .withEligibilityDefinition(eligibilityDef)
            .build();
    versionRepository.publishNewSynchronizedVersion();

    applicationRepository
        .createOrUpdateDraft(applicant.id, programForDraft.id)
        .toCompletableFuture()
        .join();
    applicationRepository
        .submitApplication(applicant.id, programForSubmitted.id, Optional.empty())
        .toCompletableFuture()
        .join();

    ApplicantService.ApplicationPrograms result =
        subject
            .relevantProgramsForApplicant(applicant.id, trustedIntermediaryProfile)
            .toCompletableFuture()
            .join();

    assertThat(result.inProgress().stream().map(p -> p.program().id()))
        .containsExactly(programForDraft.id);
    assertThat(result.inProgress().stream().map(ApplicantProgramData::isProgramMaybeEligible))
        .containsExactly(Optional.of(true));
    assertThat(
            result.inProgress().stream()
                .map(ApplicantProgramData::latestSubmittedApplicationStatus))
        .containsExactly(Optional.empty());
    assertThat(result.submitted().stream().map(ApplicantProgramData::isProgramMaybeEligible))
        .containsExactly(Optional.empty());
    assertThat(result.submitted().stream().map(p -> p.program().id()))
        .containsExactly(programForSubmitted.id);
    assertThat(
            result.submitted().stream().map(ApplicantProgramData::latestSubmittedApplicationStatus))
        .containsExactly(Optional.empty());
    assertThat(result.unapplied().stream().map(p -> p.program().id()))
        .containsExactlyInAnyOrder(programForUnapplied.id, programDefinition.id());
    assertThat(result.unapplied().stream().map(ApplicantProgramData::isProgramMaybeEligible))
        .containsExactlyInAnyOrder(Optional.of(true), Optional.empty());
    assertThat(
            result.unapplied().stream().map(ApplicantProgramData::latestSubmittedApplicationStatus))
        .containsExactly(Optional.empty(), Optional.empty());
  }

  @Test
  public void relevantProgramsForApplicant_otherApplicant() {
    Applicant primaryApplicant = createTestApplicant();
    Applicant otherApplicant = createTestApplicant();
    Program programForDraft =
        ProgramBuilder.newActiveProgram("program_for_draft")
            .withBlock()
            .withRequiredQuestion(testQuestionBank.applicantName())
            .build();
    Program programForSubmitted =
        ProgramBuilder.newActiveProgram("program_for_submitted")
            .withBlock()
            .withRequiredQuestion(testQuestionBank.applicantFavoriteColor())
            .build();
    Program programForUnapplied =
        ProgramBuilder.newActiveProgram("program_for_unapplied").withBlock().build();

    applicationRepository
        .createOrUpdateDraft(primaryApplicant.id, programForDraft.id)
        .toCompletableFuture()
        .join();
    applicationRepository
        .submitApplication(primaryApplicant.id, programForSubmitted.id, Optional.empty())
        .toCompletableFuture()
        .join();

    ApplicantService.ApplicationPrograms result =
        subject
            .relevantProgramsForApplicant(otherApplicant.id, trustedIntermediaryProfile)
            .toCompletableFuture()
            .join();

    assertThat(result.inProgress()).isEmpty();
    assertThat(result.submitted()).isEmpty();
    assertThat(result.unapplied().stream().map(p -> p.program().id()))
        .containsExactlyInAnyOrder(
            programForDraft.id,
            programForUnapplied.id,
            programForSubmitted.id,
            programDefinition.id());
    assertThat(
            result.unapplied().stream().map(ApplicantProgramData::latestSubmittedApplicationStatus))
        .containsExactly(Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty());
  }

  @Test
  public void relevantProgramsForApplicant_withNewerProgramVersion() {
    Applicant applicant = createTestApplicant();

    // Create a draft based on the original version of a program.
    Program originalProgramForDraft =
        ProgramBuilder.newActiveProgram("program_for_draft")
            .withBlock()
            .withRequiredQuestion(testQuestionBank.applicantName())
            .build();
    applicationRepository
        .createOrUpdateDraft(applicant.id, originalProgramForDraft.id)
        .toCompletableFuture()
        .join();

    // Submit an application based on the original version of a program.
    Program originalProgramForSubmit =
        ProgramBuilder.newActiveProgram("program_for_submit")
            .withBlock()
            .withRequiredQuestion(testQuestionBank.applicantName())
            .build();
    applicationRepository
        .submitApplication(applicant.id, originalProgramForSubmit.id, Optional.empty())
        .toCompletableFuture()
        .join();

    // Create new program versions.
    ProgramBuilder.newDraftProgram("program_for_draft")
        .withBlock()
        .withRequiredQuestion(testQuestionBank.applicantName())
        .build();
    Program updatedProgramForSubmit =
        ProgramBuilder.newDraftProgram("program_for_submit")
            .withBlock()
            .withRequiredQuestion(testQuestionBank.applicantName())
            .build();
    versionRepository.publishNewSynchronizedVersion();

    ApplicantService.ApplicationPrograms result =
        subject
            .relevantProgramsForApplicant(applicant.id, trustedIntermediaryProfile)
            .toCompletableFuture()
            .join();

    // Drafts always use the version of the program they were
    // started on.
    assertThat(result.inProgress().stream().map(p -> p.program().id()))
        .containsExactly(originalProgramForDraft.id);
    // Submitted programs always use the most recent version of the program.
    assertThat(result.submitted().stream().map(p -> p.program().id()))
        .containsExactly(updatedProgramForSubmit.id);
    // As part of test setup, a "test program" is initialized.
    // When calling publish, this will become active. This provides
    // confidence that the draft version created above is actually published.
    assertThat(result.unapplied().stream().map(p -> p.program().id()))
        .containsExactly(programDefinition.id());
  }

  @Test
  public void relevantProgramsForApplicant_hiddenFromIndex() {
    // This ensures that the applicant can always see that draft
    // applications for a given program, even if a newer version of the
    // program is hidden from the index.

    Applicant applicant = createTestApplicant();

    // Create a submitted application based on the original version of a program.
    Program originalProgramForDraftApp =
        ProgramBuilder.newActiveProgram("program_for_draft")
            .withBlock()
            .withRequiredQuestion(testQuestionBank.applicantName())
            .build();
    Program originalProgramForSubmittedApp =
        ProgramBuilder.newActiveProgram("program_for_application")
            .withBlock()
            .withRequiredQuestion(testQuestionBank.applicantName())
            .build();
    applicationRepository
        .createOrUpdateDraft(applicant.id, originalProgramForDraftApp.id)
        .toCompletableFuture()
        .join();
    applicationRepository
        .submitApplication(applicant.id, originalProgramForSubmittedApp.id, Optional.empty())
        .toCompletableFuture()
        .join();

    // Create a new program version.
    Program updatedProgramForDraftApp =
        ProgramBuilder.newDraftProgram("program_for_draft")
            .withBlock()
            .withRequiredQuestion(testQuestionBank.applicantName())
            .build();
    updatedProgramForDraftApp.getProgramDefinition().toBuilder()
        .setDisplayMode(DisplayMode.HIDDEN_IN_INDEX)
        .build()
        .toProgram()
        .update();
    Program updatedProgramForSubmittedApp =
        ProgramBuilder.newDraftProgram("program_for_application")
            .withBlock()
            .withRequiredQuestion(testQuestionBank.applicantName())
            .build();
    updatedProgramForSubmittedApp.getProgramDefinition().toBuilder()
        .setDisplayMode(DisplayMode.HIDDEN_IN_INDEX)
        .build()
        .toProgram()
        .update();
    versionRepository.publishNewSynchronizedVersion();

    ApplicantService.ApplicationPrograms result =
        subject
            .relevantProgramsForApplicant(applicant.id, trustedIntermediaryProfile)
            .toCompletableFuture()
            .join();

    assertThat(result.inProgress().stream().map(p -> p.program().id()))
        .containsExactly(originalProgramForDraftApp.id);
    // TODO(#3477): Determine if already submitted applications for hidden
    // programs should show in the index, similar to draft applications.
    assertThat(result.submitted()).isEmpty();
    // As part of test setup, a "test program" is initialized.
    // When calling publish, this will become active. This provides
    // confidence that the draft version created above is actually published.
    assertThat(result.unapplied().stream().map(p -> p.program().id()))
        .containsExactly(programDefinition.id());
  }

  @Test
  public void relevantProgramsForApplicant_ShowsDraftThatIsDisabled() {
    // This ensures that the applicant can always see that draft
    // applications for a given program, even if a newer version of the
    // program is disabled from the index.
    Applicant applicant = createTestApplicant();

    // Create a draft application based on the original version of a program.
    Program originalProgramForDraftApp =
        ProgramBuilder.newActiveProgram("program_for_draft")
            .withBlock()
            .withRequiredQuestion(testQuestionBank.applicantName())
            .build();
    applicationRepository
        .createOrUpdateDraft(applicant.id, originalProgramForDraftApp.id)
        .toCompletableFuture()
        .join();
    // create a new version of the program that is disabled
    Program updatedProgramForDraftApp =
        ProgramBuilder.newDraftProgram("program_for_draft")
            .withBlock()
            .withRequiredQuestion(testQuestionBank.applicantName())
            .build();
    updatedProgramForDraftApp.getProgramDefinition().toBuilder()
        .setDisplayMode(models.DisplayMode.DISABLED)
        .build()
        .toProgram()
        .update();
    versionRepository.publishNewSynchronizedVersion();

    ApplicantService.ApplicationPrograms result =
        subject
            .relevantProgramsForApplicant(applicant.id, trustedIntermediaryProfile)
            .toCompletableFuture()
            .join();
    assertThat(result.inProgress().stream().map(p -> p.program().id()))
        .containsExactly(originalProgramForDraftApp.id);
    // As part of test setup, a "test program" is initialized.
    // When calling publish, this will become active. This provides
    // confidence that the draft version created above is actually published.
    assertThat(result.unapplied().stream().map(p -> p.program().id()))
        .containsExactly(programDefinition.id());
  }

  @Test
  public void
      relevantProgramsForApplicant_DoesNotShowsPreviouslySubmittedApplicationThatIsDisabled() {
    // This ensures that the applicant can always see that submitted
    // applications for a given program, even if a newer version of the
    // program is disabled from the index.

    Applicant applicant = createTestApplicant();

    // Create a submitted application based on the original version of a program.
    Program originalProgramForSubmittedApp =
        ProgramBuilder.newActiveProgram("program_for_application")
            .withBlock()
            .withRequiredQuestion(testQuestionBank.applicantName())
            .build();
    applicationRepository
        .submitApplication(applicant.id, originalProgramForSubmittedApp.id, Optional.empty())
        .toCompletableFuture()
        .join();

    // Create a new program version that is disabled
    Program updatedProgramForSubmittedApp =
        ProgramBuilder.newDraftProgram("program_for_application")
            .withBlock()
            .withRequiredQuestion(testQuestionBank.applicantName())
            .build();
    updatedProgramForSubmittedApp.getProgramDefinition().toBuilder()
        .setDisplayMode(DisplayMode.DISABLED)
        .build()
        .toProgram()
        .update();
    versionRepository.publishNewSynchronizedVersion();

    ApplicantService.ApplicationPrograms result =
        subject
            .relevantProgramsForApplicant(applicant.id, trustedIntermediaryProfile)
            .toCompletableFuture()
            .join();
    // Disabled Programs, though submitted is still hidden from applicants
    assertThat(result.submitted()).isEmpty();
    // As part of test setup, a "test program" is initialized.
    // When calling publish, this will become active. This provides
    // confidence that the draft version created above is actually published.
    assertThat(result.unapplied().stream().map(p -> p.program().id()))
        .containsExactly(programDefinition.id());
  }

  @Test
  public void relevantProgramsForApplicant_tiOnly() {
    Applicant applicant = createTestApplicant();
    Applicant ti = resourceCreator.insertApplicant();
    Account tiAccount = resourceCreator.insertAccount();
    TrustedIntermediaryGroup tiGroup =
        new TrustedIntermediaryGroup("Super Cool CBO", "Description");
    tiGroup.save();
    tiAccount.setMemberOfGroup(tiGroup);
    tiAccount.save();
    ti.setAccount(tiAccount);
    ti.save();

    // Create a submitted application based on the original version of a program.
    Program originalProgramForDraftApp =
        ProgramBuilder.newActiveProgram("program_for_draft")
            .withBlock()
            .withRequiredQuestion(testQuestionBank.applicantName())
            .build();
    Program originalProgramForSubmittedApp =
        ProgramBuilder.newActiveProgram("program_for_application")
            .withBlock()
            .withRequiredQuestion(testQuestionBank.applicantName())
            .build();
    applicationRepository
        .createOrUpdateDraft(applicant.id, originalProgramForDraftApp.id)
        .toCompletableFuture()
        .join();
    applicationRepository
        .submitApplication(applicant.id, originalProgramForSubmittedApp.id, Optional.empty())
        .toCompletableFuture()
        .join();
    // Create a new program version.
    Program updatedProgramForDraftApp =
        ProgramBuilder.newDraftProgram("program_for_draft")
            .withBlock()
            .withRequiredQuestion(testQuestionBank.applicantName())
            .build();
    updatedProgramForDraftApp.getProgramDefinition().toBuilder()
        .setDisplayMode(DisplayMode.TI_ONLY)
        .build()
        .toProgram()
        .update();
    Program updatedProgramForSubmittedApp =
        ProgramBuilder.newDraftProgram("program_for_application")
            .withBlock()
            .withRequiredQuestion(testQuestionBank.applicantName())
            .build();
    updatedProgramForSubmittedApp.getProgramDefinition().toBuilder()
        .setDisplayMode(DisplayMode.TI_ONLY)
        .build()
        .toProgram()
        .update();
    versionRepository.publishNewSynchronizedVersion();

    ApplicantService.ApplicationPrograms applicantResult =
        subject
            .relevantProgramsForApplicant(applicant.id, applicantProfile)
            .toCompletableFuture()
            .join();
    ApplicantService.ApplicationPrograms tiResult =
        subject
            .relevantProgramsForApplicant(ti.id, trustedIntermediaryProfile)
            .toCompletableFuture()
            .join();

    assertThat(applicantResult.inProgress().stream().map(p -> p.program().id()))
        .containsExactly(originalProgramForDraftApp.id);
    // TODO(#3477): Determine if already submitted applications for hidden
    // programs should show in the index, similar to draft applications.
    assertThat(applicantResult.submitted()).isEmpty();
    // As part of test setup, a "test program" is initialized.
    // When calling publish, this will become active. This provides
    // confidence that the draft version created above is actually published.
    // Additionally, this ensures the applicant can not see the TI-only programs.
    assertThat(applicantResult.unapplied().stream().map(p -> p.program().id()))
        .containsExactly(programDefinition.id());

    assertThat(tiResult.unapplied().stream().map(p -> p.program().id()))
        .containsExactly(
            programDefinition.id(), updatedProgramForDraftApp.id, updatedProgramForSubmittedApp.id);
  }

  @Test
  public void relevantProgramsForApplicant_In_SelectTi_Mode() {
    Applicant applicant = createTestApplicant();
    CiviFormProfile tiProfile =
        profileFactory.wrapProfileData(profileFactory.createFakeTrustedIntermediary());

    // Create a submitted application based on the original version of a program.
    Program originalProgramForDraftApp =
        ProgramBuilder.newActiveProgram("program_for_draft")
            .withBlock()
            .withRequiredQuestion(testQuestionBank.applicantName())
            .build();
    Program originalProgramForSubmittedApp =
        ProgramBuilder.newActiveProgram("program_for_application")
            .withBlock()
            .withRequiredQuestion(testQuestionBank.applicantName())
            .build();
    applicationRepository
        .createOrUpdateDraft(applicant.id, originalProgramForDraftApp.id)
        .toCompletableFuture()
        .join();
    applicationRepository
        .submitApplication(applicant.id, originalProgramForSubmittedApp.id, Optional.empty())
        .toCompletableFuture()
        .join();
    HashSet<Long> tiAcls = new HashSet<>();
    tiAcls.add(tiProfile.getAccount().join().getMemberOfGroup().get().id);
    // Create a new program version.
    Program updatedProgramForDraftApp =
        ProgramBuilder.newDraftProgram("program_for_draft")
            .withBlock()
            .withRequiredQuestion(testQuestionBank.applicantName())
            .build();
    updatedProgramForDraftApp.getProgramDefinition().toBuilder()
        .setDisplayMode(DisplayMode.SELECT_TI)
        .setAcls(new ProgramAcls(tiAcls))
        .build()
        .toProgram()
        .update();
    Program updatedProgramForSubmittedApp =
        ProgramBuilder.newDraftProgram("program_for_application")
            .withBlock()
            .withRequiredQuestion(testQuestionBank.applicantName())
            .build();
    updatedProgramForSubmittedApp.getProgramDefinition().toBuilder()
        .setDisplayMode(DisplayMode.SELECT_TI)
        .setAcls(new ProgramAcls(tiAcls))
        .build()
        .toProgram()
        .update();
    versionRepository.publishNewSynchronizedVersion();

    ApplicantService.ApplicationPrograms applicantResult =
        subject
            .relevantProgramsForApplicant(applicant.id, applicantProfile)
            .toCompletableFuture()
            .join();
    ApplicantService.ApplicationPrograms tiResult =
        subject
            .relevantProgramsForApplicant(tiProfile.getApplicant().join().id, tiProfile)
            .toCompletableFuture()
            .join();

    assertThat(applicantResult.inProgress().stream().map(p -> p.program().id()))
        .containsExactly(originalProgramForDraftApp.id);
    // TODO(#3477): Determine if already submitted applications for hidden
    // programs should show in the index, similar to draft applications.
    assertThat(applicantResult.submitted()).isEmpty();
    // As part of test setup, a "test program" is initialized.
    // When calling publish, this will become active. This provides
    // confidence that the draft version created above is actually published.
    // Additionally, this ensures the applicant can not see the SELECT_TI programs.
    assertThat(applicantResult.unapplied().stream().map(p -> p.program().id()))
        .containsExactly(programDefinition.id());

    assertThat(tiResult.unapplied().stream().map(p -> p.program().id()))
        .containsExactly(
            programDefinition.id(), updatedProgramForDraftApp.id, updatedProgramForSubmittedApp.id);
  }

  @Test
  public void relevantProgramsForApplicant_submittedTimestamp() {
    // Creates an app + draft app for a program as well as
    // an application for another program and ensures that
    // the submitted timestamp is present.
    Applicant applicant = createTestApplicant();

    // Create a submitted application based on the original version of a program.
    Program programForDraftApp =
        ProgramBuilder.newActiveProgram("program_for_draft")
            .withBlock()
            .withRequiredQuestion(testQuestionBank.applicantName())
            .build();
    Program programForSubmittedApp =
        ProgramBuilder.newActiveProgram("program_for_application")
            .withBlock()
            .withRequiredQuestion(testQuestionBank.applicantName())
            .build();
    Optional<Application> firstApp =
        applicationRepository
            .submitApplication(applicant.id, programForDraftApp.id, Optional.empty())
            .toCompletableFuture()
            .join();
    Instant firstAppSubmitTime = firstApp.orElseThrow().getSubmitTime();
    applicationRepository
        .createOrUpdateDraft(applicant.id, programForDraftApp.id)
        .toCompletableFuture()
        .join();
    Optional<Application> secondApp =
        applicationRepository
            .submitApplication(applicant.id, programForSubmittedApp.id, Optional.empty())
            .toCompletableFuture()
            .join();
    Instant secondAppSubmitTime = secondApp.orElseThrow().getSubmitTime();

    ApplicantService.ApplicationPrograms result =
        subject
            .relevantProgramsForApplicant(applicant.id, trustedIntermediaryProfile)
            .toCompletableFuture()
            .join();

    assertThat(result.inProgress().stream().map(p -> p.program().id()))
        .containsExactly(programForDraftApp.id);
    assertThat(
            result.inProgress().stream().map(ApplicantProgramData::latestSubmittedApplicationTime))
        .containsExactly(Optional.of(firstAppSubmitTime));
    assertThat(result.submitted().stream().map(p -> p.program().id()))
        .containsExactly(programForSubmittedApp.id);
    assertThat(
            result.submitted().stream().map(ApplicantProgramData::latestSubmittedApplicationTime))
        .containsExactly(Optional.of(secondAppSubmitTime));
    assertThat(result.unapplied().stream().map(p -> p.program().id()))
        .containsExactly(programDefinition.id());
  }

  @Test
  public void relevantProgramsForApplicant_multipleActiveAndDraftApplications() {
    Applicant applicant = createTestApplicant();
    Program programForDraft =
        ProgramBuilder.newActiveProgram("program_for_draft")
            .withBlock()
            .withRequiredQuestion(testQuestionBank.applicantName())
            .build();
    Program programForSubmitted =
        ProgramBuilder.newActiveProgram("program_for_submitted")
            .withBlock()
            .withRequiredQuestion(testQuestionBank.applicantFavoriteColor())
            .build();

    // Create multiple submitted applications and ensure the most recently submitted
    // version's timestamp is chosen.
    Application firstSubmitted =
        applicationRepository
            .submitApplication(applicant.id, programForSubmitted.id, Optional.empty())
            .toCompletableFuture()
            .join()
            .get();
    applicationRepository
        .submitApplication(applicant.id, programForSubmitted.id, Optional.empty())
        .toCompletableFuture()
        .join();
    // We want to ensure ordering is occurring by submit time, NOT by application ID.
    // Simulate a bad state where the first submission (lower database ID) has a later
    // submit time.
    firstSubmitted.refresh();
    Instant submittedLater = Instant.now().plusSeconds(60 * 60);
    // We have to reset the lifecycle stage since submitting another application will mark
    // the previous as obsolete.
    firstSubmitted
        .setLifecycleStage(LifecycleStage.ACTIVE)
        .setSubmitTimeForTest(submittedLater)
        .save();

    // We submit the application since the system should prevent multiple drafts from
    // being created. Below, we'll manually set the lifecycle stage to DRAFT. This simulates
    // a bad state where we have multiple draft apps and the lower database ID has a later
    // creation time.
    Application firstDraft =
        applicationRepository
            .submitApplication(applicant.id, programForDraft.id, Optional.empty())
            .toCompletableFuture()
            .join()
            .get();
    Program updatedProgramForDraft =
        ProgramBuilder.newDraftProgram("program_for_draft")
            .withBlock()
            .withRequiredQuestion(testQuestionBank.applicantName())
            .build();
    versionRepository.publishNewSynchronizedVersion();
    applicationRepository
        .createOrUpdateDraft(applicant.id, updatedProgramForDraft.id)
        .toCompletableFuture()
        .join();
    Instant draftLater = Instant.now().plusSeconds(60 * 60);
    firstDraft.refresh();
    firstDraft.setLifecycleStage(LifecycleStage.DRAFT).setCreateTimeForTest(draftLater).save();

    ApplicantService.ApplicationPrograms result =
        subject
            .relevantProgramsForApplicant(applicant.id, trustedIntermediaryProfile)
            .toCompletableFuture()
            .join();

    assertThat(result.submitted().stream().map(p -> p.program().id()))
        .containsExactly(programForSubmitted.id);
    assertThat(
            result.submitted().stream().map(ApplicantProgramData::latestSubmittedApplicationTime))
        .containsExactly(Optional.of(submittedLater));
    assertThat(result.inProgress().stream().map(p -> p.program().id()))
        .containsExactly(firstDraft.getProgram().id);
    // As part of test setup, a "test program" is initialized.
    assertThat(result.unapplied().stream().map(p -> p.program().id()))
        .containsExactly(programDefinition.id());
  }

  @Test
  public void relevantProgramsForApplicant_withApplicationStatus() {
    Applicant applicant = createTestApplicant();
    Program program =
        ProgramBuilder.newActiveProgram("program")
            .withStatusDefinitions(new StatusDefinitions(ImmutableList.of(APPROVED_STATUS)))
            .withBlock()
            .withRequiredQuestion(testQuestionBank.applicantFavoriteColor())
            .build();

    Account adminAccount = resourceCreator.insertAccountWithEmail("admin@example.com");
    Application submittedApplication =
        applicationRepository
            .submitApplication(applicant.id, program.id, Optional.empty())
            .toCompletableFuture()
            .join()
            .get();
    addStatusEvent(submittedApplication, APPROVED_STATUS, adminAccount);

    ApplicantService.ApplicationPrograms result =
        subject
            .relevantProgramsForApplicant(applicant.id, trustedIntermediaryProfile)
            .toCompletableFuture()
            .join();

    assertThat(result.inProgress()).isEmpty();
    assertThat(result.submitted().stream().map(p -> p.program().id())).containsExactly(program.id);
    assertThat(
            result.submitted().stream().map(ApplicantProgramData::latestSubmittedApplicationStatus))
        .containsExactly(Optional.of(APPROVED_STATUS));
    assertThat(result.unapplied().stream().map(p -> p.program().id()))
        .containsExactly(programDefinition.id());
  }

  @Test
  public void relevantProgramsForApplicant_withApplicationStatusAndOlderProgramVersion() {
    Applicant applicant = createTestApplicant();
    Program originalProgram =
        ProgramBuilder.newObsoleteProgram("program")
            .withStatusDefinitions(new StatusDefinitions(ImmutableList.of(APPROVED_STATUS)))
            .withBlock()
            .withRequiredQuestion(testQuestionBank.applicantFavoriteColor())
            .build();
    originalProgram.getVersions().stream()
        .findAny()
        .orElseThrow()
        .addQuestion(testQuestionBank.applicantFavoriteColor())
        .save();

    Account adminAccount = resourceCreator.insertAccountWithEmail("admin@example.com");
    Application submittedApplication =
        applicationRepository
            .submitApplication(applicant.id, originalProgram.id, Optional.empty())
            .toCompletableFuture()
            .join()
            .get();
    addStatusEvent(submittedApplication, APPROVED_STATUS, adminAccount);

    // Publish a new program that has an updated set of status configurations that doesn't include
    // the application's status. The displayed status definition configuration should be pulled
    // from the program version associated with the application.
    StatusDefinitions.Status updatedStatus =
        APPROVED_STATUS.toBuilder()
            .setLocalizedStatusText(LocalizedStrings.withDefaultValue("Updated email content"))
            .build();
    assertThat(updatedStatus).isNotEqualTo(APPROVED_STATUS);
    Program updatedProgram =
        ProgramBuilder.newActiveProgram("program")
            .withStatusDefinitions(new StatusDefinitions(ImmutableList.of(updatedStatus)))
            .withBlock()
            .withRequiredQuestion(testQuestionBank.applicantFavoriteColor())
            .build();

    ApplicantService.ApplicationPrograms result =
        subject
            .relevantProgramsForApplicant(applicant.id, trustedIntermediaryProfile)
            .toCompletableFuture()
            .join();

    assertThat(result.inProgress()).isEmpty();
    assertThat(result.submitted().stream().map(p -> p.program().id()))
        .containsExactly(updatedProgram.id);
    assertThat(
            result.submitted().stream().map(ApplicantProgramData::latestSubmittedApplicationStatus))
        .containsExactly(Optional.of(APPROVED_STATUS));
    assertThat(result.unapplied().stream().map(p -> p.program().id()))
        .containsExactly(programDefinition.id());
  }

  @Test
  public void maybeEligibleProgramsForApplicant_includesPartiallyEligiblePrograms() {
    // Set up applicant
    Applicant applicant = subject.createApplicant().toCompletableFuture().join();
    applicant.setAccount(resourceCreator.insertAccount());
    applicant.save();

    // Set up question eligibility
    NameQuestionDefinition eligibleQuestion =
        createNameQuestion("question_with_matching_eligibility");
    NameQuestionDefinition unansweredQuestion = createNameQuestion("unanswered_question");
    EligibilityDefinition eligibleQuestionEligibilityDefinition =
        createEligibilityDefinition(eligibleQuestion, "Taylor");
    EligibilityDefinition unansweredQuestionEligibilityDefinition =
        createEligibilityDefinition(unansweredQuestion, "Sza");

    // Setup program for answering questions (not necessarily a common intake program)
    Program programForAnsweringQuestions =
        ProgramBuilder.newDraftProgram("other program")
            .withBlock()
            .withRequiredQuestionDefinition(eligibleQuestion)
            .build();

    // Set up program for draft application.
    Program programForDraftApp =
        ProgramBuilder.newDraftProgram("program_for_draft_app")
            .withBlock()
            .withRequiredQuestionDefinition(eligibleQuestion)
            .withEligibilityDefinition(eligibleQuestionEligibilityDefinition)
            .withBlock()
            .withRequiredQuestionDefinition(unansweredQuestion)
            .withEligibilityDefinition(unansweredQuestionEligibilityDefinition)
            .build();

    // Set up program for submitted application.
    Program programForSubmittedApp =
        ProgramBuilder.newDraftProgram("program_for_submitted_app")
            .withBlock()
            .withRequiredQuestionDefinition(eligibleQuestion)
            .withEligibilityDefinition(eligibleQuestionEligibilityDefinition)
            .withBlock()
            .withRequiredQuestionDefinition(unansweredQuestion)
            .withEligibilityDefinition(unansweredQuestionEligibilityDefinition)
            .build();

    // Set up unapplied program.
    Program programForUnappliedApp =
        ProgramBuilder.newDraftProgram("program_for_unapplied_app")
            .withBlock()
            .withRequiredQuestionDefinition(eligibleQuestion)
            .withEligibilityDefinition(eligibleQuestionEligibilityDefinition)
            .withBlock()
            .withRequiredQuestionDefinition(unansweredQuestion)
            .withEligibilityDefinition(unansweredQuestionEligibilityDefinition)
            .build();

    versionRepository.publishNewSynchronizedVersion();

    // Answer questions.
    answerNameQuestion(
        eligibleQuestion,
        "Taylor",
        "Allison",
        "Swift",
        programForAnsweringQuestions
            .getProgramDefinition()
            .getBlockDefinitionByIndex(0)
            .orElseThrow()
            .id(),
        applicant.id,
        programForAnsweringQuestions.id);

    // Start a draft application.
    applicationRepository
        .createOrUpdateDraft(applicant.id, programForDraftApp.id)
        .toCompletableFuture()
        .join();

    // Submit an application.
    applicationRepository
        .submitApplication(applicant.id, programForSubmittedApp.id, Optional.empty())
        .toCompletableFuture()
        .join();

    var result =
        subject
            .maybeEligibleProgramsForApplicant(applicant.id, trustedIntermediaryProfile)
            .toCompletableFuture()
            .join();

    // Asset results contained expected program IDs
    var matchingProgramIds =
        result.stream().map(pd -> pd.program().id()).collect(ImmutableList.toImmutableList());

    assertThat(matchingProgramIds).contains(programForDraftApp.id);
    assertThat(matchingProgramIds).contains(programForUnappliedApp.id);
    assertThat(matchingProgramIds).contains(programForSubmittedApp.id);
  }

  @Test
  public void maybeEligibleProgramsForApplicant_doesNotIncludeIneligiblePrograms() {
    // Set up applicant
    Applicant applicant = subject.createApplicant().toCompletableFuture().join();
    applicant.setAccount(resourceCreator.insertAccount());
    applicant.save();

    // Set up program and questions
    NameQuestionDefinition eligibleQuestion =
        createNameQuestion("question_with_matching_eligibility");
    NameQuestionDefinition ineligibleQuestion =
        createNameQuestion("question_with_non_matching_eligibility");
    EligibilityDefinition eligibleQuestionEligibilityDefinition =
        createEligibilityDefinition(eligibleQuestion, "Taylor");
    EligibilityDefinition ineligibleQuestionEligibilityDefinition =
        createEligibilityDefinition(ineligibleQuestion, "Sza");
    var programWithEligibleAndIneligibleAnswers =
        ProgramBuilder.newDraftProgram("program_with_eligible_and_ineligible_answers")
            .withBlock()
            .withRequiredQuestionDefinition(eligibleQuestion)
            .withEligibilityDefinition(eligibleQuestionEligibilityDefinition)
            .withBlock()
            .withRequiredQuestionDefinition(ineligibleQuestion)
            .withEligibilityDefinition(ineligibleQuestionEligibilityDefinition)
            .build();
    versionRepository.publishNewSynchronizedVersion();

    // Fill out application
    answerNameQuestion(
        eligibleQuestion,
        "Taylor",
        "Allison",
        "Swift",
        programWithEligibleAndIneligibleAnswers
            .getProgramDefinition()
            .getBlockDefinitionByIndex(0)
            .orElseThrow()
            .id(),
        applicant.id,
        programWithEligibleAndIneligibleAnswers.id);
    answerNameQuestion(
        ineligibleQuestion,
        "Solána",
        "Imani",
        "Rowe",
        programWithEligibleAndIneligibleAnswers
            .getProgramDefinition()
            .getBlockDefinitionByIndex(1)
            .orElseThrow()
            .id(),
        applicant.id,
        programWithEligibleAndIneligibleAnswers.id);

    // We need at least one application for the ApplicantService to bother filling eligibility
    // statuses. It doesn't have to be the same one we're filling out.
    applicationRepository
        .createOrUpdateDraft(applicant.id, ProgramBuilder.newActiveProgram("throwaway").build().id)
        .toCompletableFuture()
        .join();

    // Publish version and fetch results
    versionRepository.publishNewSynchronizedVersion();
    var result =
        subject
            .maybeEligibleProgramsForApplicant(applicant.id, trustedIntermediaryProfile)
            .toCompletableFuture()
            .join();

    var matchingProgramIds =
        result.stream().map(pd -> pd.program().id()).collect(ImmutableList.toImmutableList());
    assertThat(matchingProgramIds).doesNotContain(programWithEligibleAndIneligibleAnswers.id);
  }

  @Test
  public void maybeEligibleProgramsForApplicant_doesNotIncludeIneligibleSubmittedApplications() {
    // Set up applicant
    Applicant applicant = subject.createApplicant().toCompletableFuture().join();
    applicant.setAccount(resourceCreator.insertAccount());
    applicant.save();

    System.out.println("reemax setting up pqs");
    // Set up program and questions
    NameQuestionDefinition eligibleQuestion =
        createNameQuestion("question_with_matching_eligibility");
    NameQuestionDefinition ineligibleQuestion =
        createNameQuestion("question_with_non_matching_eligibility");
    EligibilityDefinition eligibleQuestionEligibilityDefinition =
        createEligibilityDefinition(eligibleQuestion, "Taylor");
    EligibilityDefinition ineligibleQuestionEligibilityDefinition =
        createEligibilityDefinition(ineligibleQuestion, "Sza");
    var programWithEligibleAndIneligibleAnswers =
        ProgramBuilder.newDraftProgram(
                ProgramDefinition.builder()
                    .setId(123)
                    .setAdminName("name")
                    .setAdminDescription("desc")
                    .setExternalLink("https://usa.gov")
                    .setDisplayMode(DisplayMode.PUBLIC)
                    .setProgramType(ProgramType.DEFAULT)
                    .setEligibilityIsGating(false)
                    .setStatusDefinitions(new StatusDefinitions())
                    .setAcls(new ProgramAcls())
                    .build())
            .withBlock()
            .withRequiredQuestionDefinition(eligibleQuestion)
            .withEligibilityDefinition(eligibleQuestionEligibilityDefinition)
            .withBlock()
            .withRequiredQuestionDefinition(ineligibleQuestion)
            .withEligibilityDefinition(ineligibleQuestionEligibilityDefinition)
            .build();
    programWithEligibleAndIneligibleAnswers.save();
    versionRepository.publishNewSynchronizedVersion();
    System.out.println("published 1");

    // Fill out application
    answerNameQuestion(
        eligibleQuestion,
        "Taylor",
        "Allison",
        "Swift",
        programWithEligibleAndIneligibleAnswers
            .getProgramDefinition()
            .getBlockDefinitionByIndex(0)
            .orElseThrow()
            .id(),
        applicant.id,
        programWithEligibleAndIneligibleAnswers.id);
    System.out.println("answered 1");
    answerNameQuestion(
        ineligibleQuestion,
        "Solána",
        "Imani",
        "Rowe",
        programWithEligibleAndIneligibleAnswers
            .getProgramDefinition()
            .getBlockDefinitionByIndex(1)
            .orElseThrow()
            .id(),
        applicant.id,
        programWithEligibleAndIneligibleAnswers.id);

    System.out.println("answered 2");

    applicationRepository
        .submitApplication(
            applicant.id, programWithEligibleAndIneligibleAnswers.id, Optional.empty())
        .toCompletableFuture()
        .join();
    System.out.println("submitted ");

    // Publish version and fetch results
    versionRepository.publishNewSynchronizedVersion();
    System.out.println("publish2");
    var result =
        subject
            .maybeEligibleProgramsForApplicant(applicant.id, trustedIntermediaryProfile)
            .toCompletableFuture()
            .join();

    var matchingProgramIds =
        result.stream().map(pd -> pd.program().id()).collect(ImmutableList.toImmutableList());
    assertThat(matchingProgramIds).doesNotContain(programWithEligibleAndIneligibleAnswers.id);
  }

  @Test
  public void maybeEligibleProgramsForApplicant_doesNotIncludeCommonIntake() {
    // Set up applicant
    Applicant applicant = subject.createApplicant().toCompletableFuture().join();
    applicant.setAccount(resourceCreator.insertAccount());
    applicant.save();

    // Set up common intake form
    NameQuestionDefinition question = createNameQuestion("question");
    Program commonIntakeForm =
        ProgramBuilder.newDraftProgram(
                ProgramDefinition.builder()
                    .setId(123)
                    .setAdminName("common_intake_form")
                    .setAdminDescription("common_intake_form")
                    .setExternalLink("https://usa.gov")
                    .setDisplayMode(DisplayMode.PUBLIC)
                    .setProgramType(ProgramType.COMMON_INTAKE_FORM)
                    .setEligibilityIsGating(false)
                    .setStatusDefinitions(new StatusDefinitions())
                    .setAcls(new ProgramAcls())
                    .build())
            .withBlock()
            .withRequiredQuestionDefinition(question)
            .build();
    versionRepository.publishNewSynchronizedVersion();

    answerNameQuestion(
        question,
        "Taylor",
        "Allison",
        "Swift",
        commonIntakeForm.getProgramDefinition().getBlockDefinitionByIndex(0).orElseThrow().id(),
        applicant.id,
        commonIntakeForm.id);

    // We need at least one application for the ApplicantService to bother filling eligibility
    // statuses. It doesn't have to be the same one we're filling out.
    applicationRepository
        .createOrUpdateDraft(applicant.id, ProgramBuilder.newActiveProgram("throwaway").build().id)
        .toCompletableFuture()
        .join();

    // Publish version and fetch results
    versionRepository.publishNewSynchronizedVersion();
    var result =
        subject
            .maybeEligibleProgramsForApplicant(applicant.id, trustedIntermediaryProfile)
            .toCompletableFuture()
            .join();

    var matchingProgramIds =
        result.stream().map(pd -> pd.program().id()).collect(ImmutableList.toImmutableList());
    assertThat(matchingProgramIds).doesNotContain(commonIntakeForm.id);
  }

  @Test
  public void maybeEligibleProgramsForApplicant_includesProgramsWithoutEligibilityConditions() {
    // Set up applicant
    Applicant applicant = subject.createApplicant().toCompletableFuture().join();
    applicant.setAccount(resourceCreator.insertAccount());
    applicant.save();

    // Set up program and answer question
    NameQuestionDefinition question = createNameQuestion("question");
    Program testProgramWithNoEligibilityConditions =
        ProgramBuilder.newDraftProgram("test_program_with_no_eligibility_conditions")
            .withBlock()
            .withRequiredQuestionDefinition(question)
            .build();
    versionRepository.publishNewSynchronizedVersion();

    answerNameQuestion(
        question,
        "Taylor",
        "Allison",
        "Swift",
        testProgramWithNoEligibilityConditions
            .getProgramDefinition()
            .getBlockDefinitionByIndex(0)
            .orElseThrow()
            .id(),
        applicant.id,
        testProgramWithNoEligibilityConditions.id);

    // We need at least one application for the ApplicantService to bother filling eligibility
    // statuses. It doesn't have to be the same one we're filling out.
    applicationRepository
        .createOrUpdateDraft(applicant.id, ProgramBuilder.newActiveProgram("throwaway").build().id)
        .toCompletableFuture()
        .join();

    // Publish version and fetch results
    versionRepository.publishNewSynchronizedVersion();
    var result =
        subject
            .maybeEligibleProgramsForApplicant(applicant.id, trustedIntermediaryProfile)
            .toCompletableFuture()
            .join();

    var matchingProgramIds =
        result.stream().map(pd -> pd.program().id()).collect(ImmutableList.toImmutableList());
    assertThat(matchingProgramIds).contains(testProgramWithNoEligibilityConditions.id);
  }

  @Test
  public void maybeEligibleProgramsForApplicant_includesProgramsWithNoAnsweredQuestions() {
    // Set up applicant
    Applicant applicant = subject.createApplicant().toCompletableFuture().join();
    applicant.setAccount(resourceCreator.insertAccount());
    applicant.save();

    // Set up program and don't answer question
    NameQuestionDefinition question = createNameQuestion("question");
    EligibilityDefinition questionEligibilityDefinition =
        createEligibilityDefinition(question, "Taylor");
    Program testProgramWithNoEligibilityConditions =
        ProgramBuilder.newDraftProgram("test_program_with_no_eligibility_conditions")
            .withBlock()
            .withRequiredQuestionDefinition(question)
            .withEligibilityDefinition(questionEligibilityDefinition)
            .build();

    // We need at least one application for the ApplicantService to bother filling eligibility
    // statuses. It doesn't have to be the same one we're filling out.
    applicationRepository
        .createOrUpdateDraft(applicant.id, ProgramBuilder.newActiveProgram("throwaway").build().id)
        .toCompletableFuture()
        .join();

    // Publish version and fetch results
    versionRepository.publishNewSynchronizedVersion();
    var result =
        subject
            .maybeEligibleProgramsForApplicant(applicant.id, trustedIntermediaryProfile)
            .toCompletableFuture()
            .join();

    var matchingProgramIds =
        result.stream().map(pd -> pd.program().id()).collect(ImmutableList.toImmutableList());
    assertThat(matchingProgramIds).contains(testProgramWithNoEligibilityConditions.id);
  }

  private static void addStatusEvent(
      Application application, StatusDefinitions.Status status, Account actorAccount) {
    ApplicationEventDetails details =
        ApplicationEventDetails.builder()
            .setEventType(ApplicationEventDetails.Type.STATUS_CHANGE)
            .setStatusEvent(
                StatusEvent.builder()
                    .setStatusText(status.statusText())
                    .setEmailSent(false)
                    .build())
            .build();
    ApplicationEvent event = new ApplicationEvent(application, Optional.of(actorAccount), details);
    event.save();
    application.refresh();
  }

  private void answerNameQuestion(
      NameQuestionDefinition questionDefinition,
      String firstName,
      String middleName,
      String lastName,
      Long blockId,
      long applicantId,
      long programId) {
    Path questionPath =
        ApplicantData.APPLICANT_PATH.join(questionDefinition.getQuestionPathSegment());
    ImmutableMap<String, String> updates =
        ImmutableMap.<String, String>builder()
            .put(questionPath.join(Scalar.FIRST_NAME).toString(), firstName)
            .put(questionPath.join(Scalar.MIDDLE_NAME).toString(), middleName)
            .put(questionPath.join(Scalar.LAST_NAME).toString(), lastName)
            .build();
    subject
        .stageAndUpdateIfValid(applicantId, programId, Long.toString(blockId), updates, false)
        .toCompletableFuture()
        .join();
  }

  private NameQuestionDefinition createNameQuestion(String name) {
    return (NameQuestionDefinition)
        questionService
            .create(
                new NameQuestionDefinition(
                    QuestionDefinitionConfig.builder()
                        .setName(name)
                        .setDescription("description")
                        .setQuestionText(LocalizedStrings.of(Locale.US, "question?"))
                        .setQuestionHelpText(LocalizedStrings.of(Locale.US, "help text"))
                        .build()))
            .getResult();
  }

  private void createQuestions() {
    questionDefinition = createNameQuestion("name");
  }

  private void createProgram() {
    createProgram(questionDefinition);
  }

  private void createProgram(QuestionDefinition... questions) {
    programDefinition =
        ProgramBuilder.newDraftProgram("test program", "desc")
            .withBlock()
            .withRequiredQuestionDefinitions(ImmutableList.copyOf(questions))
            .buildDefinition();
    versionRepository.publishNewSynchronizedVersion();
  }

  private void createProgramWithStatusDefinitions(StatusDefinitions statuses) {
    programDefinition =
        ProgramBuilder.newDraftProgram("test program", "desc")
            .withStatusDefinitions(statuses)
            .withBlock()
            .withRequiredQuestionDefinitions(ImmutableList.of(questionDefinition))
            .buildDefinition();
    versionRepository.publishNewSynchronizedVersion();
  }

  private void createProgramWithOptionalQuestion(QuestionDefinition question) {
    programDefinition =
        ProgramBuilder.newDraftProgram("test program", "desc")
            .withBlock()
            .withOptionalQuestion(question)
            .buildDefinition();
    versionRepository.publishNewSynchronizedVersion();
  }

  /**
   * @param question Question to use for eligibility definition
   * @param eligibleFirstName Value to use as the eligible answer
   * @return An eligibility definition with a {@link NameQuestionDefinition} and an eligibility
   *     condition requiring the question's {@link Scalar.FIRST_NAME} be the provided value.
   */
  private EligibilityDefinition createEligibilityDefinition(
      NameQuestionDefinition question, String eligibleFirstName) {
    return EligibilityDefinition.builder()
        .setPredicate(
            PredicateDefinition.create(
                PredicateExpressionNode.create(
                    LeafOperationExpressionNode.create(
                        question.getId(),
                        Scalar.FIRST_NAME,
                        Operator.EQUAL_TO,
                        PredicateValue.of(eligibleFirstName))),
                PredicateAction.ELIGIBLE_BLOCK))
        .build();
  }

  /**
   * @param question Question to use for the eligibility definition
   * @return An eligibility definition with a {@link NameQuestionDefinition} and an eligibility
   *     condition requiring the question's {@link Scalar.FIRST_NAME} be "eligible name".
   */
  private EligibilityDefinition createEligibilityDefinition(NameQuestionDefinition question) {
    return createEligibilityDefinition(question, "eligible name");
  }

  /**
   * Makes a program with a {@link NameQuestionDefinition} and an eligibility condition that the
   * question's {@link Scalar.FIRST_NAME} be "eligible name"
   */
  private void createProgramWithEligibility(NameQuestionDefinition question) {
    EligibilityDefinition eligibilityDef =
        EligibilityDefinition.builder()
            .setPredicate(
                PredicateDefinition.create(
                    PredicateExpressionNode.create(
                        LeafOperationExpressionNode.create(
                            question.getId(),
                            Scalar.FIRST_NAME,
                            Operator.EQUAL_TO,
                            PredicateValue.of("eligible name"))),
                    PredicateAction.ELIGIBLE_BLOCK))
            .build();
    programDefinition =
        ProgramBuilder.newActiveProgram("test program", "desc")
            .withBlock()
            .withRequiredQuestionDefinitions(ImmutableList.of(question))
            .withEligibilityDefinition(eligibilityDef)
            .buildDefinition();
  }

  private void createProgramWithNongatingEligibility(NameQuestionDefinition question) {
    EligibilityDefinition eligibilityDef =
        EligibilityDefinition.builder()
            .setPredicate(
                PredicateDefinition.create(
                    PredicateExpressionNode.create(
                        LeafOperationExpressionNode.create(
                            question.getId(),
                            Scalar.FIRST_NAME,
                            Operator.EQUAL_TO,
                            PredicateValue.of("eligible name"))),
                    PredicateAction.ELIGIBLE_BLOCK))
            .build();
    programDefinition =
        ProgramBuilder.newDraftProgram(
                ProgramDefinition.builder()
                    .setId(123)
                    .setAdminName("name")
                    .setAdminDescription("desc")
                    .setExternalLink("https://usa.gov")
                    .setDisplayMode(DisplayMode.PUBLIC)
                    .setProgramType(ProgramType.DEFAULT)
                    .setEligibilityIsGating(false)
                    .setStatusDefinitions(new StatusDefinitions())
                    .setAcls(new ProgramAcls())
                    .build())
            .withBlock()
            .withRequiredQuestionDefinitions(ImmutableList.of(question))
            .withEligibilityDefinition(eligibilityDef)
            .buildDefinition();
    versionRepository.publishNewSynchronizedVersion();
  }

  private Messages getMessages(Locale locale) {
    return messagesApi.preferred(ImmutableSet.of(Lang.forCode(locale.toLanguageTag())));
  }

  @Test
  public void getCorrectedAddress_whenSuccessfullyApplyingACorrectedAddress()
      throws ProgramBlockDefinitionNotFoundException, ProgramNotFoundException,
          ProgramQuestionDefinitionNotFoundException, ProgramQuestionDefinitionInvalidException,
          ExecutionException, InterruptedException {
    // Arrange
    Applicant applicant = subject.createApplicant().toCompletableFuture().join();
    ApplicantData applicantData =
        userRepository.lookupApplicantSync(applicant.id).get().getApplicantData();
    Question question = testQuestionBank.applicantAddress();

    Program program =
        ProgramBuilder.newActiveProgram("program")
            .withStatusDefinitions(new StatusDefinitions(ImmutableList.of(APPROVED_STATUS)))
            .withBlock()
            .withRequiredQuestion(question)
            .build();

    BlockDefinition blockDefinition =
        program.getProgramDefinition().blockDefinitions().stream().findFirst().get();
    QuestionDefinition questionDefinition = blockDefinition.getQuestionDefinition(0);

    programService.setProgramQuestionDefinitionAddressCorrectionEnabled(
        program.id, blockDefinition.id(), questionDefinition.getId(), true);

    ApplicantQuestion applicantQuestion =
        new ApplicantQuestion(questionDefinition, applicantData, Optional.empty());
    AddressQuestion addressQuestion = applicantQuestion.createAddressQuestion();

    AddressSuggestion addressSuggestion1 =
        AddressSuggestion.builder()
            .setAddress(
                Address.builder()
                    .setStreet("123 Some St")
                    .setLine2("")
                    .setCity("Seattle")
                    .setState("WA")
                    .setZip("99999")
                    .build())
            .setScore(100)
            .setLocation(
                AddressLocation.builder()
                    .setLatitude(1.0)
                    .setLongitude(1.1)
                    .setWellKnownId(2)
                    .build())
            .setSingleLineAddress("123 Some St Seattle, WA 99999")
            .build();

    AddressSuggestion addressSuggestion2 =
        AddressSuggestion.builder()
            .setAddress(
                Address.builder()
                    .setStreet("456 Any Ave")
                    .setLine2("")
                    .setCity("Seattle")
                    .setState("WA")
                    .setZip("99999")
                    .build())
            .setScore(90)
            .setLocation(
                AddressLocation.builder()
                    .setLatitude(3.0)
                    .setLongitude(3.1)
                    .setWellKnownId(4)
                    .build())
            .setSingleLineAddress("456 Any Ave Seattle, WA 99999")
            .build();

    ImmutableList<AddressSuggestion> addressSuggestionList =
        ImmutableList.of(addressSuggestion1, addressSuggestion2);
    Address address = addressSuggestion1.getAddress();
    AddressLocation addressLocation = addressSuggestion1.getLocation();

    // Act
    ImmutableMap<String, String> correctedAddress =
        subject
            .getCorrectedAddress(
                applicant.id,
                program.id,
                String.valueOf(blockDefinition.id()),
                addressSuggestion1.getSingleLineAddress(),
                addressSuggestionList)
            .toCompletableFuture()
            .get();

    // Assert
    assertThat(correctedAddress.get(addressQuestion.getStreetPath().toString()))
        .isEqualTo(address.getStreet());
    assertThat(correctedAddress.get(addressQuestion.getLine2Path().toString()))
        .isEqualTo(address.getLine2());
    assertThat(correctedAddress.get(addressQuestion.getCityPath().toString()))
        .isEqualTo(address.getCity());
    assertThat(correctedAddress.get(addressQuestion.getStatePath().toString()))
        .isEqualTo(address.getState());
    assertThat(correctedAddress.get(addressQuestion.getZipPath().toString()))
        .isEqualTo(address.getZip());
    assertThat(correctedAddress.get(addressQuestion.getLatitudePath().toString()))
        .isEqualTo(addressLocation.getLatitude().toString());
    assertThat(correctedAddress.get(addressQuestion.getLongitudePath().toString()))
        .isEqualTo(addressLocation.getLongitude().toString());
    assertThat(correctedAddress.get(addressQuestion.getWellKnownIdPath().toString()))
        .isEqualTo(addressLocation.getWellKnownId().toString());
    assertThat(correctedAddress.get(addressQuestion.getCorrectedPath().toString()))
        .isEqualTo(CorrectedAddressState.CORRECTED.getSerializationFormat());
  }

  @Test
  public void getCorrectedAddress_whenUserChooseToKeepTheAddressOriginallyEntered()
      throws ProgramBlockDefinitionNotFoundException, ProgramNotFoundException,
          ProgramQuestionDefinitionNotFoundException, ProgramQuestionDefinitionInvalidException,
          ExecutionException, InterruptedException {
    // Arrange
    Applicant applicant = subject.createApplicant().toCompletableFuture().join();
    ApplicantData applicantData =
        userRepository.lookupApplicantSync(applicant.id).get().getApplicantData();
    Question question = testQuestionBank.applicantAddress();

    Program program =
        ProgramBuilder.newActiveProgram("program")
            .withStatusDefinitions(new StatusDefinitions(ImmutableList.of(APPROVED_STATUS)))
            .withBlock()
            .withRequiredQuestion(question)
            .build();

    BlockDefinition blockDefinition =
        program.getProgramDefinition().blockDefinitions().stream().findFirst().get();
    QuestionDefinition questionDefinition = blockDefinition.getQuestionDefinition(0);

    programService.setProgramQuestionDefinitionAddressCorrectionEnabled(
        program.id, blockDefinition.id(), questionDefinition.getId(), true);

    ApplicantQuestion applicantQuestion =
        new ApplicantQuestion(questionDefinition, applicantData, Optional.empty());
    AddressQuestion addressQuestion = applicantQuestion.createAddressQuestion();

    AddressSuggestion addressSuggestion1 =
        AddressSuggestion.builder()
            .setAddress(
                Address.builder()
                    .setStreet("123 Some St")
                    .setLine2("")
                    .setCity("Seattle")
                    .setState("WA")
                    .setZip("99999")
                    .build())
            .setScore(100)
            .setLocation(
                AddressLocation.builder()
                    .setLatitude(1.0)
                    .setLongitude(1.1)
                    .setWellKnownId(2)
                    .build())
            .setSingleLineAddress("123 Some St Seattle, WA 99999")
            .build();

    AddressSuggestion addressSuggestion2 =
        AddressSuggestion.builder()
            .setAddress(
                Address.builder()
                    .setStreet("456 Any Ave")
                    .setLine2("")
                    .setCity("Seattle")
                    .setState("WA")
                    .setZip("99999")
                    .build())
            .setScore(90)
            .setLocation(
                AddressLocation.builder()
                    .setLatitude(3.0)
                    .setLongitude(3.1)
                    .setWellKnownId(4)
                    .build())
            .setSingleLineAddress("456 Any Ave Seattle, WA 99999")
            .build();

    ImmutableList<AddressSuggestion> addressSuggestionList =
        ImmutableList.of(addressSuggestion1, addressSuggestion2);

    // Act
    ImmutableMap<String, String> correctedAddress =
        subject
            .getCorrectedAddress(
                applicant.id,
                program.id,
                String.valueOf(blockDefinition.id()),
                AddressCorrectionBlockView.USER_KEEPING_ADDRESS_VALUE,
                addressSuggestionList)
            .toCompletableFuture()
            .get();

    // Assert
    assertThat(correctedAddress.get(addressQuestion.getCorrectedPath().toString()))
        .isEqualTo(CorrectedAddressState.AS_ENTERED_BY_USER.getSerializationFormat());
  }

  @Test
  public void getCorrectedAddress_whenExternalCorrectionFails()
      throws ProgramBlockDefinitionNotFoundException, ProgramNotFoundException,
          ProgramQuestionDefinitionNotFoundException, ProgramQuestionDefinitionInvalidException,
          ExecutionException, InterruptedException {
    // Arrange
    Applicant applicant = subject.createApplicant().toCompletableFuture().join();
    ApplicantData applicantData =
        userRepository.lookupApplicantSync(applicant.id).get().getApplicantData();
    Question question = testQuestionBank.applicantAddress();

    Program program =
        ProgramBuilder.newActiveProgram("program")
            .withStatusDefinitions(new StatusDefinitions(ImmutableList.of(APPROVED_STATUS)))
            .withBlock()
            .withRequiredQuestion(question)
            .build();

    BlockDefinition blockDefinition =
        program.getProgramDefinition().blockDefinitions().stream().findFirst().get();
    QuestionDefinition questionDefinition = blockDefinition.getQuestionDefinition(0);

    programService.setProgramQuestionDefinitionAddressCorrectionEnabled(
        program.id, blockDefinition.id(), questionDefinition.getId(), true);

    ApplicantQuestion applicantQuestion =
        new ApplicantQuestion(questionDefinition, applicantData, Optional.empty());
    AddressQuestion addressQuestion = applicantQuestion.createAddressQuestion();

    AddressSuggestion addressSuggestion1 =
        AddressSuggestion.builder()
            .setAddress(
                Address.builder()
                    .setStreet("123 Some St")
                    .setLine2("")
                    .setCity("Seattle")
                    .setState("WA")
                    .setZip("99999")
                    .build())
            .setScore(100)
            .setLocation(
                AddressLocation.builder()
                    .setLatitude(1.0)
                    .setLongitude(1.1)
                    .setWellKnownId(2)
                    .build())
            .setSingleLineAddress("123 Some St Seattle, WA 99999")
            .build();

    AddressSuggestion addressSuggestion2 =
        AddressSuggestion.builder()
            .setAddress(
                Address.builder()
                    .setStreet("456 Any Ave")
                    .setLine2("")
                    .setCity("Seattle")
                    .setState("WA")
                    .setZip("99999")
                    .build())
            .setScore(90)
            .setLocation(
                AddressLocation.builder()
                    .setLatitude(3.0)
                    .setLongitude(3.1)
                    .setWellKnownId(4)
                    .build())
            .setSingleLineAddress("456 Any Ave Seattle, WA 99999")
            .build();

    ImmutableList<AddressSuggestion> addressSuggestionList =
        ImmutableList.of(addressSuggestion1, addressSuggestion2);

    // Act
    ImmutableMap<String, String> correctedAddress =
        subject
            .getCorrectedAddress(
                applicant.id,
                program.id,
                String.valueOf(blockDefinition.id()),
                "asdf",
                addressSuggestionList)
            .toCompletableFuture()
            .get();

    // Assert
    assertThat(correctedAddress.get(addressQuestion.getCorrectedPath().toString()))
        .isEqualTo(CorrectedAddressState.FAILED.getSerializationFormat());
  }

  @Test
  public void getFirstAddressCorrectionEnabledApplicantQuestion_isSuccessful()
      throws ProgramBlockDefinitionNotFoundException, ProgramNotFoundException,
          ProgramQuestionDefinitionNotFoundException, ProgramQuestionDefinitionInvalidException {
    // Arrange
    Applicant applicant = subject.createApplicant().toCompletableFuture().join();
    ApplicantData applicantData =
        userRepository.lookupApplicantSync(applicant.id).get().getApplicantData();
    Question question = testQuestionBank.applicantAddress();

    Program program =
        ProgramBuilder.newActiveProgram("program")
            .withStatusDefinitions(new StatusDefinitions(ImmutableList.of(APPROVED_STATUS)))
            .withBlock()
            .withRequiredQuestion(question)
            .build();

    BlockDefinition blockDefinition =
        program.getProgramDefinition().blockDefinitions().stream().findFirst().get();
    QuestionDefinition questionDefinition = blockDefinition.getQuestionDefinition(0);

    programService.setProgramQuestionDefinitionAddressCorrectionEnabled(
        program.id, blockDefinition.id(), questionDefinition.getId(), true);

    programDefinition = programService.getProgramDefinition(program.id);
    blockDefinition = programDefinition.getBlockDefinition(blockDefinition.id());

    Block block =
        new Block(
            String.valueOf(blockDefinition.id()), blockDefinition, applicantData, Optional.empty());

    // Act
    ApplicantQuestion applicantQuestionNew =
        subject.getFirstAddressCorrectionEnabledApplicantQuestion(block);

    // Assert
    assertThat(applicantQuestionNew.isAddressCorrectionEnabled()).isTrue();
  }

  @Test
  public void getFirstAddressCorrectionEnabledApplicantQuestion_fails() {
    // Arrange
    Applicant applicant = subject.createApplicant().toCompletableFuture().join();
    ApplicantData applicantData =
        userRepository.lookupApplicantSync(applicant.id).get().getApplicantData();
    Question question = testQuestionBank.applicantAddress();

    Program program =
        ProgramBuilder.newActiveProgram("program")
            .withStatusDefinitions(new StatusDefinitions(ImmutableList.of(APPROVED_STATUS)))
            .withBlock()
            .withRequiredQuestion(question)
            .build();

    BlockDefinition blockDefinition =
        program.getProgramDefinition().blockDefinitions().stream().findFirst().get();

    Block block =
        new Block(
            String.valueOf(blockDefinition.id()), blockDefinition, applicantData, Optional.empty());

    // Act & Assert
    assertThatExceptionOfType(RuntimeException.class)
        .isThrownBy(() -> subject.getFirstAddressCorrectionEnabledApplicantQuestion(block))
        .withMessageContaining(
            "Expected to find an address with address correction enabled in block");
  }

  /* Creates a program with an address question with address correction enabled,
   * creates an applicant, creates a block with the given address, then returns
   * the block for use in getAddressSuggestionGroup.
   */
  public Block createProgramAndBlockWithAddress(String address)
      throws ProgramBlockDefinitionNotFoundException, ProgramNotFoundException,
          ProgramQuestionDefinitionNotFoundException, ProgramQuestionDefinitionInvalidException {
    Applicant applicant = subject.createApplicant().toCompletableFuture().join();
    ApplicantData applicantData =
        userRepository.lookupApplicantSync(applicant.id).get().getApplicantData();
    Question question = testQuestionBank.applicantAddress();

    Program program =
        ProgramBuilder.newActiveProgram("program")
            .withStatusDefinitions(new StatusDefinitions(ImmutableList.of(APPROVED_STATUS)))
            .withBlock()
            .withRequiredQuestion(question)
            .build();

    BlockDefinition blockDefinition =
        program.getProgramDefinition().blockDefinitions().stream().findFirst().get();
    QuestionDefinition questionDefinition = blockDefinition.getQuestionDefinition(0);

    programService.setProgramQuestionDefinitionAddressCorrectionEnabled(
        program.id, blockDefinition.id(), questionDefinition.getId(), true);

    programDefinition = programService.getProgramDefinition(program.id);
    blockDefinition = programDefinition.getBlockDefinition(blockDefinition.id());

    Block block =
        new Block(
            String.valueOf(blockDefinition.id()), blockDefinition, applicantData, Optional.empty());

    // update address so values aren't empty
    ImmutableMap<String, String> updates =
        ImmutableMap.<String, String>builder()
            .put(Path.create("applicant.applicant_address").join(Scalar.STREET).toString(), address)
            .put(Path.create("applicant.applicant_address").join(Scalar.CITY).toString(), "City")
            .put(Path.create("applicant.applicant_address").join(Scalar.STATE).toString(), "State")
            .put(Path.create("applicant.applicant_address").join(Scalar.ZIP).toString(), "55555")
            .build();

    subject
        .stageAndUpdateIfValid(applicant.id, programDefinition.id(), block.getId(), updates, true)
        .toCompletableFuture()
        .join();

    ApplicantData applicantDataAfter =
        userRepository.lookupApplicantSync(applicant.id).get().getApplicantData();

    return new Block(
        String.valueOf(blockDefinition.id()),
        blockDefinition,
        applicantDataAfter,
        Optional.empty());
  }

  @Test
  public void getAddressSuggestionGroup_isSuccessful()
      throws ProgramBlockDefinitionNotFoundException, ProgramNotFoundException,
          ProgramQuestionDefinitionNotFoundException, ProgramQuestionDefinitionInvalidException {
    Block block = createProgramAndBlockWithAddress("Legit Address");
    AddressSuggestionGroup addressSuggestionGroup =
        subject.getAddressSuggestionGroup(block).toCompletableFuture().join();
    assertThat(addressSuggestionGroup.getAddressSuggestions().size()).isEqualTo(4);
  }

  @Test
  public void getAddressSuggestionGroup_noSuggestions()
      throws ProgramBlockDefinitionNotFoundException, ProgramNotFoundException,
          ProgramQuestionDefinitionNotFoundException, ProgramQuestionDefinitionInvalidException {
    Block block = createProgramAndBlockWithAddress("Bogus Address");
    AddressSuggestionGroup addressSuggestionGroup =
        subject.getAddressSuggestionGroup(block).toCompletableFuture().join();
    assertThat(addressSuggestionGroup.getAddressSuggestions().size()).isEqualTo(0);
  }

  @Test
  public void getAddressSuggestionGroup_errorFromService()
      throws ProgramBlockDefinitionNotFoundException, ProgramNotFoundException,
          ProgramQuestionDefinitionNotFoundException, ProgramQuestionDefinitionInvalidException {
    Block block = createProgramAndBlockWithAddress("Error Address");
    AddressSuggestionGroup addressSuggestionGroup =
        subject.getAddressSuggestionGroup(block).toCompletableFuture().join();
    assertThat(addressSuggestionGroup.getAddressSuggestions().size()).isEqualTo(0);
  }

  @Test
  public void getApplicantMayBeEligibleStatus() {
    createProgramWithNongatingEligibility(questionDefinition);
    Applicant applicant = subject.createApplicant().toCompletableFuture().join();
    applicant.setAccount(resourceCreator.insertAccount());
    applicant.save();

    // Applicant's answer is ineligible.
    Path questionPath =
        ApplicantData.APPLICANT_PATH.join(questionDefinition.getQuestionPathSegment());
    ImmutableMap<String, String> updates =
        ImmutableMap.<String, String>builder()
            .put(questionPath.join(Scalar.FIRST_NAME).toString(), "Ineligible answer")
            .put(questionPath.join(Scalar.LAST_NAME).toString(), "irrelevant answer")
            .build();
    subject
        .stageAndUpdateIfValid(applicant.id, programDefinition.id(), "1", updates, false)
        .toCompletableFuture()
        .join();
    applicant = userRepository.lookupApplicantSync(applicant.id).get();

    assertThat(subject.getApplicantMayBeEligibleStatus(applicant, programDefinition).get())
        .isFalse();

    // Applicant' answer gets changed to an eligible answer.
    questionPath = ApplicantData.APPLICANT_PATH.join(questionDefinition.getQuestionPathSegment());
    updates =
        ImmutableMap.<String, String>builder()
            .put(questionPath.join(Scalar.FIRST_NAME).toString(), "eligible name")
            .build();
    subject
        .stageAndUpdateIfValid(applicant.id, programDefinition.id(), "1", updates, false)
        .toCompletableFuture()
        .join();
    applicant = userRepository.lookupApplicantSync(applicant.id).get();

    assertThat(subject.getApplicantMayBeEligibleStatus(applicant, programDefinition).get())
        .isTrue();
  }

  @Test
  public void getApplicationEligibilityStatus() {
    createProgramWithNongatingEligibility(questionDefinition);
    Applicant applicant = subject.createApplicant().toCompletableFuture().join();
    applicant.setAccount(resourceCreator.insertAccount());
    applicant.save();

    // Application is submitted with an ineligible answer.
    Path questionPath =
        ApplicantData.APPLICANT_PATH.join(questionDefinition.getQuestionPathSegment());
    ImmutableMap<String, String> updates =
        ImmutableMap.<String, String>builder()
            .put(questionPath.join(Scalar.FIRST_NAME).toString(), "Ineligible answer")
            .put(questionPath.join(Scalar.LAST_NAME).toString(), "irrelevant answer")
            .build();
    subject
        .stageAndUpdateIfValid(applicant.id, programDefinition.id(), "1", updates, false)
        .toCompletableFuture()
        .join();
    Application ineligibleApplication =
        subject
            .submitApplication(applicant.id, programDefinition.id(), trustedIntermediaryProfile)
            .toCompletableFuture()
            .join();

    // Application is re-submitted with eligible answers.
    questionPath = ApplicantData.APPLICANT_PATH.join(questionDefinition.getQuestionPathSegment());
    updates =
        ImmutableMap.<String, String>builder()
            .put(questionPath.join(Scalar.FIRST_NAME).toString(), "eligible name")
            .build();
    subject
        .stageAndUpdateIfValid(applicant.id, programDefinition.id(), "1", updates, false)
        .toCompletableFuture()
        .join();
    Application eligibleApplication =
        subject
            .submitApplication(applicant.id, programDefinition.id(), trustedIntermediaryProfile)
            .toCompletableFuture()
            .join();

    // First application still evaluates to ineligible.
    assertThat(
            subject.getApplicationEligibilityStatus(ineligibleApplication, programDefinition).get())
        .isFalse();
    // Re-submission evaluates to eligible.
    assertThat(
            subject.getApplicationEligibilityStatus(eligibleApplication, programDefinition).get())
        .isTrue();
  }
}
