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
import models.LifecycleStage;
import models.Program;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import repository.ApplicationRepository;
import repository.UserRepository;
import repository.WithPostgresContainer;
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
import services.question.types.NameQuestionDefinition;
import services.question.types.QuestionDefinition;
import support.ProgramBuilder;

public class ApplicantServiceImplTest extends WithPostgresContainer {

  private ApplicantServiceImpl subject;
  private QuestionService questionService;
  private QuestionDefinition questionDefinition;
  private ProgramDefinition programDefinition;
  private ApplicationRepository applicationRepository;
  private UserRepository userRepository;
  private CiviFormProfile trustedIntermediaryProfile;

  @Before
  public void setUp() throws Exception {
    subject = instanceOf(ApplicantServiceImpl.class);
    questionService = instanceOf(QuestionService.class);
    applicationRepository = instanceOf(ApplicationRepository.class);
    userRepository = instanceOf(UserRepository.class);
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
  public void stageAndUpdateIfValid_emptySetOfUpdates_doesNotChangeApplicant() {
    Applicant applicant = subject.createApplicant(1L).toCompletableFuture().join();
    ApplicantData applicantDataBefore = applicant.getApplicantData();

    subject
        .stageAndUpdateIfValid(applicant.id, programDefinition.id(), "1", ImmutableMap.of())
        .toCompletableFuture()
        .join();

    ApplicantData applicantDataAfter =
        userRepository.lookupApplicantSync(applicant.id).get().getApplicantData();

    assertThat(applicantDataAfter).isEqualTo(applicantDataBefore);
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
    subject.getName(9999L).toCompletableFuture().join();
  }

  @Test
  public void getName_anonymousApplicant() {
    Applicant applicant = resourceCreator.insertApplicant();
    String name = subject.getName(applicant.id).toCompletableFuture().join();
    assertThat(name).isEqualTo("<Anonymous Applicant>");
  }

  @Test
  public void getName_namedApplicantId() {
    Applicant applicant = resourceCreator.insertApplicant();
    applicant.getApplicantData().setUserName("Hello World");
    applicant.save();
    String name = subject.getName(applicant.id).toCompletableFuture().join();
    assertThat(name).isEqualTo("World, Hello");
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
    Program p1 =
        ProgramBuilder.newActiveProgram()
            .withBlock()
            .withQuestion(testQuestionBank.applicantName())
            .build();
    Program p2 =
        ProgramBuilder.newActiveProgram()
            .withBlock()
            .withQuestion(testQuestionBank.applicantFavoriteColor())
            .build();
    applicationRepository.createOrUpdateDraft(applicant.id, p1.id).toCompletableFuture().join();

    ImmutableMap<LifecycleStage, ImmutableList<ProgramDefinition>> programs =
        subject.relevantPrograms(applicant.id).toCompletableFuture().join();

    assertThat(programs.get(LifecycleStage.DRAFT).stream().map(ProgramDefinition::id))
        .containsExactly(p1.id);
    assertThat(programs.get(LifecycleStage.ACTIVE).stream().map(ProgramDefinition::id))
        .containsExactly(p1.id, p2.id);
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
            .withQuestionDefinitions(ImmutableList.copyOf(questions))
            .buildDefinition();
  }
}
