package services.applicant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.catchThrowable;

import auth.CiviFormProfile;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import java.time.Instant;
import java.util.Locale;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import models.Account;
import models.Applicant;
import models.Application;
import models.DisplayMode;
import models.LifecycleStage;
import models.Program;
import models.Question;
import models.StoredFile;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import repository.ApplicationRepository;
import repository.ResetPostgres;
import repository.UserRepository;
import repository.VersionRepository;
import services.LocalizedStrings;
import services.Path;
import services.applicant.exception.ApplicantNotFoundException;
import services.applicant.exception.ApplicationSubmissionException;
import services.applicant.exception.ProgramBlockNotFoundException;
import services.applicant.question.Scalar;
import services.program.PathNotInBlockException;
import services.program.ProgramDefinition;
import services.program.ProgramNotFoundException;
import services.question.QuestionOption;
import services.question.QuestionService;
import services.question.types.CheckboxQuestionDefinition;
import services.question.types.FileUploadQuestionDefinition;
import services.question.types.NameQuestionDefinition;
import services.question.types.QuestionDefinition;
import support.ProgramBuilder;

public class ApplicantServiceImplTest extends ResetPostgres {

  private ApplicantServiceImpl subject;
  private QuestionService questionService;
  private QuestionDefinition questionDefinition;
  private ProgramDefinition programDefinition;
  private UserRepository userRepository;
  private ApplicationRepository applicationRepository;
  private VersionRepository versionRepository;
  private CiviFormProfile trustedIntermediaryProfile;

  @Before
  public void setUp() throws Exception {
    subject = instanceOf(ApplicantServiceImpl.class);
    questionService = instanceOf(QuestionService.class);
    userRepository = instanceOf(UserRepository.class);
    applicationRepository = instanceOf(ApplicationRepository.class);
    versionRepository = instanceOf(VersionRepository.class);
    createQuestions();
    createProgram();

    trustedIntermediaryProfile = Mockito.mock(CiviFormProfile.class);
    Account account = new Account();
    account.setEmailAddress("test@example.com");
    Mockito.when(trustedIntermediaryProfile.isTrustedIntermediary()).thenReturn(true);
    Mockito.when(trustedIntermediaryProfile.getAccount())
        .thenReturn(CompletableFuture.completedFuture(account));
  }

  @Test
  public void stageAndUpdateIfValid_emptySetOfUpdates_leavesQuestionsUnansweredAndUpdatesMetadata()
      throws Exception {
    // We make the question optional since it's not valid to stage empty updates
    // for a required question.
    programDefinition =
        ProgramBuilder.newDraftProgram("test program", "desc")
            .withBlock()
            .withOptionalQuestion(questionDefinition)
            .buildDefinition();
    Applicant applicant = subject.createApplicant(1L).toCompletableFuture().join();
    subject
        .stageAndUpdateIfValid(applicant.id, programDefinition.id(), "1", ImmutableMap.of())
        .toCompletableFuture()
        .join();
    ApplicantData applicantData =
        userRepository.lookupApplicantSync(applicant.id).get().getApplicantData();

    Path questionPath =
        ApplicantData.APPLICANT_PATH.join(questionDefinition.getQuestionPathSegment());
    Scalar.getScalars(questionDefinition.getQuestionType()).stream()
        .map(scalar -> questionPath.join(scalar))
        .forEach(path -> assertThat(applicantData.hasPath(path)).isFalse());
    assertThat(applicantData.readLong(questionPath.join(Scalar.PROGRAM_UPDATED_IN)))
        .contains(programDefinition.id());
  }

  @Test
  public void stageAndUpdateIfValid_withUpdatesWithEmptyStrings_deletesJsonData() {
    // We make the question optional since it's not valid to update a required
    // question with an empty string (done below).
    programDefinition =
        ProgramBuilder.newDraftProgram("test program", "desc")
            .withBlock()
            .withOptionalQuestion(questionDefinition)
            .buildDefinition();
    Applicant applicant = subject.createApplicant(1L).toCompletableFuture().join();
    Path questionPath = Path.create("applicant.name");

    // Put something in there
    ImmutableMap<String, String> updates =
        ImmutableMap.<String, String>builder()
            .put(questionPath.join(Scalar.FIRST_NAME).toString(), "Alice")
            .put(questionPath.join(Scalar.LAST_NAME).toString(), "Doe")
            .build();
    subject
        .stageAndUpdateIfValid(applicant.id, programDefinition.id(), "1", updates)
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
        .stageAndUpdateIfValid(applicant.id, programDefinition.id(), "1", updates)
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
    createProgram(testQuestionBank.applicantKitchenTools().getQuestionDefinition());
    Applicant applicant = subject.createApplicant(1L).toCompletableFuture().join();
    Path questionPath = Path.create("applicant.kitchen_tools");

    // Put checkbox answer in
    ImmutableMap<String, String> updates =
        ImmutableMap.<String, String>builder()
            .put(questionPath.join(Scalar.SELECTIONS).asArrayElement().atIndex(0).toString(), "1")
            .put(questionPath.join(Scalar.SELECTIONS).asArrayElement().atIndex(1).toString(), "2")
            .build();
    subject
        .stageAndUpdateIfValid(applicant.id, programDefinition.id(), "1", updates)
        .toCompletableFuture()
        .join();
    ApplicantData applicantDataMiddle =
        userRepository.lookupApplicantSync(applicant.id).get().getApplicantData();
    assertThat(applicantDataMiddle.readList(questionPath.join(Scalar.SELECTIONS))).isNotEmpty();

    // Now put empty updates
    updates = ImmutableMap.of(questionPath.join(Scalar.SELECTIONS).asArrayElement().toString(), "");
    subject
        .stageAndUpdateIfValid(applicant.id, programDefinition.id(), "1", updates)
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
    createProgram(
        dateQuestion.getQuestionDefinition(),
        currencyQuestion.getQuestionDefinition(),
        numberQuestion.getQuestionDefinition());

    Applicant applicant = subject.createApplicant(1L).toCompletableFuture().join();

    // Stage invalid updates for each question.
    ImmutableMap<String, String> updates =
        ImmutableMap.<String, String>builder()
            .put(datePath.toString(), "invalid_date_input")
            .put(currencyPath.toString(), "invalid_currency_input")
            .put(numberPath.toString(), "invalid_number_input")
            .build();

    // We grab the result of the stage call to assert that it contains
    // data suitable for displaying errors downstream.
    ReadOnlyApplicantProgramService resultService =
        subject
            .stageAndUpdateIfValid(applicant.id, programDefinition.id(), "1", updates)
            .toCompletableFuture()
            .join();

    // Applicant data is not updated and is considered invalid.
    ApplicantData resultApplicantData = resultService.getApplicantData();
    assertThat(resultApplicantData.hasPath(datePath)).isFalse();
    assertThat(resultApplicantData.hasPath(currencyPath)).isFalse();
    assertThat(resultApplicantData.hasPath(numberPath)).isFalse();
    assertThat(resultApplicantData.getFailedUpdates())
        .isEqualTo(
            ImmutableMap.of(
                datePath, "invalid_date_input",
                currencyPath, "invalid_currency_input",
                numberPath, "invalid_number_input"));

    ApplicantData freshApplicantDataAfter =
        userRepository.lookupApplicantSync(applicant.id).get().getApplicantData();
    assertThat(freshApplicantDataAfter.hasPath(datePath)).isFalse();
    assertThat(freshApplicantDataAfter.hasPath(currencyPath)).isFalse();
    assertThat(freshApplicantDataAfter.hasPath(numberPath)).isFalse();
  }

  @Test
  public void
      stageAndUpdateIfValid_forEnumeratorBlock_putsMetadataWithEmptyUpdate_andCanPutRealRepeatedEntitiesInAfter() {
    programDefinition =
        ProgramBuilder.newDraftProgram("test program", "desc")
            .withBlock()
            .withRequiredQuestion(testQuestionBank.applicantHouseholdMembers())
            .buildDefinition();
    Applicant applicant = subject.createApplicant(1L).toCompletableFuture().join();
    Path enumeratorPath =
        ApplicantData.APPLICANT_PATH.join(
            testQuestionBank
                .applicantHouseholdMembers()
                .getQuestionDefinition()
                .getQuestionPathSegment());

    // Empty update should put metadata in
    ImmutableMap<String, String> updates = ImmutableMap.of();
    subject
        .stageAndUpdateIfValid(applicant.id, programDefinition.id(), "1", updates)
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
        .stageAndUpdateIfValid(applicant.id, programDefinition.id(), "1", updates)
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
        .stageAndUpdateIfValid(applicant.id, programDefinition.id(), "1", updates)
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
        ProgramBuilder.newDraftProgram("test program", "desc")
            .withBlock()
            .withRequiredQuestion(testQuestionBank.applicantHouseholdMembers())
            .buildDefinition();
    Applicant applicant = subject.createApplicant(1L).toCompletableFuture().join();
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
        .stageAndUpdateIfValid(applicant.id, programDefinition.id(), "1", updates)
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
        .stageAndUpdateIfValid(applicant.id, programDefinition.id(), "1", updates)
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

  @Test
  public void stageAndUpdateIfValid_withUpdates_isOk() {
    Applicant applicant = subject.createApplicant(1L).toCompletableFuture().join();

    ImmutableMap<String, String> updates =
        ImmutableMap.<String, String>builder()
            .put(Path.create("applicant.name").join(Scalar.FIRST_NAME).toString(), "Alice")
            .put(Path.create("applicant.name").join(Scalar.LAST_NAME).toString(), "Doe")
            .build();

    subject
        .stageAndUpdateIfValid(applicant.id, programDefinition.id(), "1", updates)
        .toCompletableFuture()
        .join();

    ApplicantData applicantDataAfter =
        userRepository.lookupApplicantSync(applicant.id).get().getApplicantData();

    assertThat(applicantDataAfter.asJsonString()).contains("Alice", "Doe");
  }

  @Test
  public void stageAndUpdateIfValid_updatesMetadataForQuestionOnce() {
    Applicant applicant = subject.createApplicant(1L).toCompletableFuture().join();

    ImmutableMap<String, String> updates =
        ImmutableMap.<String, String>builder()
            .put(Path.create("applicant.name").join(Scalar.FIRST_NAME).toString(), "Alice")
            .put(Path.create("applicant.name").join(Scalar.LAST_NAME).toString(), "Doe")
            .build();

    subject
        .stageAndUpdateIfValid(applicant.id, programDefinition.id(), "1", updates)
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
    QuestionDefinition multiSelectQuestion =
        questionService
            .create(
                new CheckboxQuestionDefinition(
                    "checkbox",
                    Optional.empty(),
                    "description",
                    LocalizedStrings.of(Locale.US, "question?"),
                    LocalizedStrings.of(Locale.US, "help text"),
                    ImmutableList.of(
                        QuestionOption.create(1L, LocalizedStrings.of(Locale.US, "cat")),
                        QuestionOption.create(2L, LocalizedStrings.of(Locale.US, "dog")),
                        QuestionOption.create(3L, LocalizedStrings.of(Locale.US, "horse")))))
            .getResult();
    createProgram(multiSelectQuestion);

    Applicant applicant = subject.createApplicant(1L).toCompletableFuture().join();

    Path checkboxPath = Path.create("applicant.checkbox").join(Scalar.SELECTIONS).asArrayElement();
    ImmutableMap<String, String> updates =
        ImmutableMap.<String, String>builder()
            .put(checkboxPath.atIndex(0).toString(), "1")
            .put(checkboxPath.atIndex(1).toString(), "2")
            .put(checkboxPath.atIndex(2).toString(), "3")
            .build();

    subject
        .stageAndUpdateIfValid(applicant.id, programDefinition.id(), "1", updates)
        .toCompletableFuture()
        .join();

    ApplicantData applicantDataAfter =
        userRepository.lookupApplicantSync(applicant.id).get().getApplicantData();

    assertThat(
            applicantDataAfter.readList(Path.create("applicant.checkbox").join(Scalar.SELECTIONS)))
        .hasValue(ImmutableList.of(1L, 2L, 3L));

    // Ensure that we can successfully overwrite the array.
    updates =
        ImmutableMap.<String, String>builder()
            .put(checkboxPath.atIndex(0).toString(), "3")
            .put(checkboxPath.atIndex(1).toString(), "1")
            .build();
    subject
        .stageAndUpdateIfValid(applicant.id, programDefinition.id(), "1", updates)
        .toCompletableFuture()
        .join();

    applicantDataAfter = userRepository.lookupApplicantSync(applicant.id).get().getApplicantData();

    assertThat(
            applicantDataAfter.readList(Path.create("applicant.checkbox").join(Scalar.SELECTIONS)))
        .hasValue(ImmutableList.of(3L, 1L));

    // Clear values by sending an empty item.
    updates =
        ImmutableMap.<String, String>builder().put(checkboxPath.atIndex(0).toString(), "").build();
    subject
        .stageAndUpdateIfValid(applicant.id, programDefinition.id(), "1", updates)
        .toCompletableFuture()
        .join();

    applicantDataAfter = userRepository.lookupApplicantSync(applicant.id).get().getApplicantData();

    assertThat(
            applicantDataAfter.readList(Path.create("applicant.checkbox").join(Scalar.SELECTIONS)))
        .isEmpty();
  }

  @Test
  public void stageAndUpdateIfValid_withEnumeratorChangesAndDeletes_isOk() {
    QuestionDefinition enumeratorQuestionDefinition =
        testQuestionBank.applicantHouseholdMembers().getQuestionDefinition();
    createProgram(enumeratorQuestionDefinition);

    Applicant applicant = subject.createApplicant(1L).toCompletableFuture().join();

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
        .stageAndUpdateIfValid(applicant.id, programDefinition.id(), "1", updates)
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
    createProgram(enumeratorQuestionDefinition);

    Applicant applicant = subject.createApplicant(1L).toCompletableFuture().join();

    Path enumeratorPath =
        ApplicantData.APPLICANT_PATH.join(enumeratorQuestionDefinition.getQuestionPathSegment());
    ImmutableMap<String, String> updates = ImmutableMap.of();

    subject
        .stageAndUpdateIfValid(applicant.id, programDefinition.id(), "1", updates)
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
                    .stageAndUpdateIfValid(badApplicantId, programDefinition.id(), "1", updates)
                    .toCompletableFuture()
                    .join())
        .withCauseInstanceOf(ApplicantNotFoundException.class)
        .withMessageContaining("Applicant not found for ID 1");
  }

  @Test
  public void stageAndUpdateIfValid_hasProgramNotFoundException() {
    Applicant applicant = subject.createApplicant(1L).toCompletableFuture().join();
    ImmutableMap<String, String> updates = ImmutableMap.of();
    long badProgramId = programDefinition.id() + 1000L;

    Throwable thrown =
        catchThrowable(
            () ->
                subject
                    .stageAndUpdateIfValid(applicant.id, badProgramId, "1", updates)
                    .toCompletableFuture()
                    .join());

    assertThat(thrown).isInstanceOf(CompletionException.class);
    assertThat(thrown).hasCauseInstanceOf(ProgramNotFoundException.class);
  }

  @Test
  public void stageAndUpdateIfValid_hasProgramBlockNotFoundException() {
    Applicant applicant = subject.createApplicant(1L).toCompletableFuture().join();
    ImmutableMap<String, String> updates = ImmutableMap.of();
    String badBlockId = "100";

    Throwable thrown =
        catchThrowable(
            () ->
                subject
                    .stageAndUpdateIfValid(
                        applicant.id, programDefinition.id(), badBlockId, updates)
                    .toCompletableFuture()
                    .join());

    assertThat(thrown).isInstanceOf(CompletionException.class);
    assertThat(thrown).hasCauseInstanceOf(ProgramBlockNotFoundException.class);
  }

  @Test
  public void stageAndUpdateIfValid_hasPathNotInBlockException() {
    Applicant applicant = subject.createApplicant(1L).toCompletableFuture().join();
    ImmutableMap<String, String> updates =
        ImmutableMap.of(
            Path.create("applicant.name.first").toString(), "Alice",
            Path.create("this.is.not.in.block").toString(), "Doe");

    Throwable thrown =
        catchThrowable(
            () ->
                subject
                    .stageAndUpdateIfValid(applicant.id, programDefinition.id(), "1", updates)
                    .toCompletableFuture()
                    .join());

    assertThat(thrown).isInstanceOf(CompletionException.class);
    assertThat(thrown).hasCauseInstanceOf(PathNotInBlockException.class);
  }

  @Test
  public void stageAndUpdateIfValid_hasIllegalArgumentExceptionForReservedMetadataScalarKeys() {
    Applicant applicant = subject.createApplicant(1L).toCompletableFuture().join();
    String reservedScalar = Path.create("applicant.name").join(Scalar.UPDATED_AT).toString();
    ImmutableMap<String, String> updates = ImmutableMap.of(reservedScalar, "12345");

    assertThatExceptionOfType(CompletionException.class)
        .isThrownBy(
            () ->
                subject
                    .stageAndUpdateIfValid(applicant.id, programDefinition.id(), "1", updates)
                    .toCompletableFuture()
                    .join())
        .withCauseInstanceOf(IllegalArgumentException.class)
        .withMessageContaining("Path contained reserved scalar key");
  }

  @Test
  public void
      stageAndUpdateIfValid_withIllegalArrayElement_hasIllegalArgumentExceptionForReservedMetadataScalarKeys() {
    Applicant applicant = subject.createApplicant(1L).toCompletableFuture().join();
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
                    .stageAndUpdateIfValid(applicant.id, programDefinition.id(), "1", updates)
                    .toCompletableFuture()
                    .join())
        .withCauseInstanceOf(IllegalArgumentException.class)
        .withMessageContaining("Path contained reserved scalar key");
  }

  @Test
  public void createApplicant_createsANewApplicant() {
    Applicant applicant = subject.createApplicant(1l).toCompletableFuture().join();

    assertThat(applicant.id).isNotNull();
  }

  @Test
  public void createApplicant_ApplicantTime() {
    Applicant applicant = subject.createApplicant(1l).toCompletableFuture().join();

    Instant t = Instant.now();

    assertThat(applicant.getWhenCreated()).isNotNull();
    assertThat(applicant.getWhenCreated()).isBefore(t);
  }

  @Test
  public void getReadOnlyApplicantService_getsReadOnlyApplicantServiceForTheApplicantAndProgram() {
    Applicant applicant = subject.createApplicant(1l).toCompletableFuture().join();

    ReadOnlyApplicantProgramService roApplicantProgramService =
        subject
            .getReadOnlyApplicantProgramService(applicant.id, programDefinition.id())
            .toCompletableFuture()
            .join();

    assertThat(roApplicantProgramService).isInstanceOf(ReadOnlyApplicantProgramService.class);
  }

  @Test
  public void submitApplication_returnsSavedApplication() {
    Applicant applicant = subject.createApplicant(1L).toCompletableFuture().join();
    applicant.setAccount(resourceCreator.insertAccount());
    applicant.save();
    ImmutableMap<String, String> updates =
        ImmutableMap.<String, String>builder()
            .put(Path.create("applicant.name").join(Scalar.FIRST_NAME).toString(), "Alice")
            .put(Path.create("applicant.name").join(Scalar.LAST_NAME).toString(), "Doe")
            .build();
    subject
        .stageAndUpdateIfValid(applicant.id, programDefinition.id(), "1", updates)
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
    Applicant applicant = subject.createApplicant(1L).toCompletableFuture().join();
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
                    "fileupload",
                    Optional.empty(),
                    "description",
                    LocalizedStrings.of(Locale.US, "question?"),
                    LocalizedStrings.of(Locale.US, "help text")))
            .getResult();

    Program firstProgram =
        ProgramBuilder.newDraftProgram("first test program", "desc")
            .withBlock()
            .withRequiredQuestionDefinitions(ImmutableList.of(fileUploadQuestion))
            .build();
    firstProgram.save();

    Program secondProgram =
        ProgramBuilder.newDraftProgram("second test program", "desc")
            .withBlock()
            .withRequiredQuestionDefinitions(ImmutableList.of(fileUploadQuestion))
            .build();
    secondProgram.save();

    subject
        .stageAndUpdateIfValid(applicant.id, firstProgram.id, "1", updates)
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
    Applicant applicant = subject.createApplicant(1L).toCompletableFuture().join();
    applicant.setAccount(resourceCreator.insertAccount());
    applicant.save();
    ImmutableMap<String, String> updates =
        ImmutableMap.<String, String>builder()
            .put(Path.create("applicant.name").join(Scalar.FIRST_NAME).toString(), "Alice")
            .put(Path.create("applicant.name").join(Scalar.LAST_NAME).toString(), "Doe")
            .build();
    subject
        .stageAndUpdateIfValid(applicant.id, programDefinition.id(), "1", updates)
        .toCompletableFuture()
        .join();

    Application oldApplication =
        subject
            .submitApplication(applicant.id, programDefinition.id(), trustedIntermediaryProfile)
            .toCompletableFuture()
            .join();

    updates =
        ImmutableMap.<String, String>builder()
            .put(Path.create("applicant.name").join(Scalar.FIRST_NAME).toString(), "Bob")
            .put(Path.create("applicant.name").join(Scalar.LAST_NAME).toString(), "Elisa")
            .build();
    subject
        .stageAndUpdateIfValid(applicant.id, programDefinition.id(), "1", updates)
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
  public void submitApplication_failsWithApplicationSubmissionException() {
    assertThatExceptionOfType(CompletionException.class)
        .isThrownBy(
            () ->
                subject
                    .submitApplication(9999L, 9999L, trustedIntermediaryProfile)
                    .toCompletableFuture()
                    .join())
        .withCauseInstanceOf(ApplicationSubmissionException.class)
        .withMessageContaining("Application", "failed to save");
  }

  @Test
  public void getName_invalidApplicantId_doesNotFail() {
    assertThat(subject.getName(9999L).toCompletableFuture().join()).isEmpty();
  }

  @Test
  public void getName_noName() {
    Applicant applicant = resourceCreator.insertApplicant();
    assertThat(subject.getName(applicant.id).toCompletableFuture().join()).isEmpty();
  }

  @Test
  public void getName_namedApplicantId() {
    Applicant applicant = resourceCreator.insertApplicant();
    applicant.getApplicantData().setUserName("Hello World");
    applicant.save();
    assertThat(subject.getName(applicant.id).toCompletableFuture().join())
        .isEqualTo(Optional.of("World, Hello"));
  }

  @Test
  public void getName_applicantWithThreeNames() {
    Applicant applicant = resourceCreator.insertApplicant();
    applicant.getApplicantData().setUserName("First Middle Last");
    applicant.save();
    assertThat(subject.getName(applicant.id).toCompletableFuture().join())
        .isEqualTo(Optional.of("Last, First"));
  }

  @Test
  public void getName_applicantWithManyNames() {
    Applicant applicant = resourceCreator.insertApplicant();
    applicant.getApplicantData().setUserName("First Second Third Fourth");
    applicant.save();
    assertThat(subject.getName(applicant.id).toCompletableFuture().join())
        .isEqualTo(Optional.of("First Second Third Fourth"));
  }

  @Test
  public void getName_applicantWithOneName() {
    Applicant applicant = resourceCreator.insertApplicant();
    applicant.getApplicantData().setUserName("Mononymous");
    applicant.save();
    assertThat(subject.getName(applicant.id).toCompletableFuture().join())
        .isEqualTo(Optional.of("Mononymous"));
  }

  @Test
  public void getEmail_invalidApplicantId_doesNotFail() {
    subject.getEmail(9999L).toCompletableFuture().join();
  }

  @Test
  public void getEmail_applicantWithNoEmail_returnsEmpty() {
    Applicant applicant = resourceCreator.insertApplicant();
    Account account = resourceCreator.insertAccount();
    applicant.setAccount(account);
    applicant.save();
    Optional<String> email = subject.getEmail(applicant.id).toCompletableFuture().join();
    assertThat(email).isEmpty();
  }

  @Test
  public void getEmail_applicantWithEmail() {
    Applicant applicant = resourceCreator.insertApplicant();
    Account account = resourceCreator.insertAccountWithEmail("test@example.com");
    applicant.setAccount(account);
    applicant.save();
    Optional<String> email = subject.getEmail(applicant.id).toCompletableFuture().join();
    assertThat(email).hasValue("test@example.com");
  }

  @Test
  public void relevantPrograms() {
    Applicant applicant = subject.createApplicant(1L).toCompletableFuture().join();
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

    ApplicantService.RelevantPrograms result =
        subject.relevantProgramsForApplicant(applicant.id).toCompletableFuture().join();

    assertThat(result.inProgress().stream().map(p -> p.program().id()))
        .containsExactly(programForDraft.id);
    assertThat(result.submitted().stream().map(p -> p.program().id()))
        .containsExactly(programForSubmitted.id);
    assertThat(result.unapplied().stream().map(p -> p.program().id()))
        .containsExactly(programForUnapplied.id);
  }

  @Test
  public void relevantPrograms_withNewerProgramVersion() {
    Applicant applicant = subject.createApplicant(1L).toCompletableFuture().join();

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

    ApplicantService.RelevantPrograms result =
        subject.relevantProgramsForApplicant(applicant.id).toCompletableFuture().join();

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
  public void relevantPrograms_hiddenFromIndex() {
    // This ensures that the applicant can always see that draft
    // applications for a given program, even if a newer version of the
    // program is hidden from the index.
    Applicant applicant = subject.createApplicant(1L).toCompletableFuture().join();

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

    ApplicantService.RelevantPrograms result =
        subject.relevantProgramsForApplicant(applicant.id).toCompletableFuture().join();

    assertThat(result.inProgress().stream().map(p -> p.program().id()))
        .containsExactly(originalProgramForDraftApp.id);
    // TODO(#2573): Determine if already submitted applications for hidden
    // programs should show in the index, similar to draft applications.
    assertThat(result.submitted()).isEmpty();
    // As part of test setup, a "test program" is initialized.
    // When calling publish, this will become active. This provides
    // confidence that the draft version created above is actually published.
    assertThat(result.unapplied().stream().map(p -> p.program().id()))
        .containsExactly(programDefinition.id());
  }

  @Test
  public void relevantPrograms_submittedTimestamp() {
    // Creates an app + draft app for a program as well as
    // an application for another program and ensures that
    // the submitted timestamp is present.
    Applicant applicant = subject.createApplicant(1L).toCompletableFuture().join();

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

    ApplicantService.RelevantPrograms result =
        subject.relevantProgramsForApplicant(applicant.id).toCompletableFuture().join();

    assertThat(result.inProgress().stream().map(p -> p.program().id()))
        .containsExactly(programForDraftApp.id);
    assertThat(result.inProgress().stream().map(p -> p.latestSubmittedApplicationTime()))
        .containsExactly(Optional.of(firstAppSubmitTime));
    assertThat(result.submitted().stream().map(p -> p.program().id()))
        .containsExactly(programForSubmittedApp.id);
    assertThat(result.submitted().stream().map(p -> p.latestSubmittedApplicationTime()))
        .containsExactly(Optional.of(secondAppSubmitTime));
    assertThat(result.unapplied()).isEmpty();
  }

  private void createQuestions() {
    questionDefinition =
        questionService
            .create(
                new NameQuestionDefinition(
                    "name",
                    Optional.empty(),
                    "description",
                    LocalizedStrings.of(Locale.US, "question?"),
                    LocalizedStrings.of(Locale.US, "help text")))
            .getResult();
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
  }
}
