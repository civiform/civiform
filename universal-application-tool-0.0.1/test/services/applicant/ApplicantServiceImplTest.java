package services.applicant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.catchThrowable;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import java.time.Instant;
import java.util.Locale;
import java.util.Optional;
import java.util.concurrent.CompletionException;
import models.Applicant;
import org.junit.Before;
import org.junit.Test;
import repository.UserRepository;
import repository.WithPostgresContainer;
import services.LocalizedStrings;
import services.Path;
import services.applicant.exception.ApplicantNotFoundException;
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
  private UserRepository userRepository;

  @Before
  public void setUp() throws Exception {
    subject = instanceOf(ApplicantServiceImpl.class);
    questionService = instanceOf(QuestionService.class);
    userRepository = instanceOf(UserRepository.class);
    createQuestions();
    createProgram();
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

    Path checkboxPath = Path.create("applicant.checkbox").join(Scalar.SELECTION).asArrayElement();
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
            applicantDataAfter.readList(Path.create("applicant.checkbox").join(Scalar.SELECTION)))
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
            applicantDataAfter.readList(Path.create("applicant.checkbox").join(Scalar.SELECTION)))
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
            applicantDataAfter.readList(Path.create("applicant.checkbox").join(Scalar.SELECTION)))
        .hasValue(ImmutableList.of());
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
