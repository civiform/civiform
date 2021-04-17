package services.applicant;

import static com.google.common.collect.ImmutableList.toImmutableList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.catchThrowable;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import java.time.Instant;
import java.util.Arrays;
import java.util.Locale;
import java.util.Optional;
import java.util.concurrent.CompletionException;
import models.Applicant;
import models.LifecycleStage;
import org.junit.Before;
import org.junit.Test;
import repository.ApplicantRepository;
import repository.WithPostgresContainer;
import services.ErrorAnd;
import services.Path;
import services.program.PathNotInBlockException;
import services.program.ProgramBlockNotFoundException;
import services.program.ProgramDefinition;
import services.program.ProgramNotFoundException;
import services.program.ProgramQuestionDefinition;
import services.program.ProgramService;
import services.program.ProgramServiceImpl;
import services.question.QuestionOption;
import services.question.QuestionService;
import services.question.types.CheckboxQuestionDefinition;
import services.question.types.NameQuestionDefinition;
import services.question.types.QuestionDefinition;

public class ApplicantServiceImplTest extends WithPostgresContainer {

  private ApplicantServiceImpl subject;
  private ProgramService programService;
  private QuestionService questionService;
  private QuestionDefinition questionDefinition;
  private ProgramDefinition programDefinition;
  private ApplicantRepository applicantRepository;

  @Before
  public void setUp() throws Exception {
    subject = instanceOf(ApplicantServiceImpl.class);
    programService = instanceOf(ProgramServiceImpl.class);
    questionService = instanceOf(QuestionService.class);
    applicantRepository = instanceOf(ApplicantRepository.class);
    createQuestions();
    createProgram();
  }

  @Test
  public void stageAndUpdateIfValid_emptySetOfUpdates_isNotAnErrorAndDoesNotChangeApplicant() {
    Applicant applicant = subject.createApplicant(1L).toCompletableFuture().join();
    ApplicantData applicantDataBefore = applicant.getApplicantData();

    ErrorAnd<ReadOnlyApplicantProgramService, Exception> errorAnd =
        subject
            .stageAndUpdateIfValid(
                applicant.id, programDefinition.id(), "1", ImmutableSet.<Update>builder().build())
            .toCompletableFuture()
            .join();

    ApplicantData applicantDataAfter =
        applicantRepository.lookupApplicantSync(applicant.id).get().getApplicantData();

    assertThat(applicantDataAfter).isEqualTo(applicantDataBefore);
    assertThat(errorAnd.getResult()).isInstanceOf(ReadOnlyApplicantProgramService.class);
    assertThat(errorAnd.isError()).isFalse();
  }

  @Test
  public void stageAndUpdateIfValid_withUpdates_isOk() {
    Applicant applicant = subject.createApplicant(1L).toCompletableFuture().join();

    ImmutableSet<Update> updates =
        ImmutableSet.of(
            Update.create(Path.create("applicant.name.first"), "Alice"),
            Update.create(Path.create("applicant.name.last"), "Doe"));

    ErrorAnd<ReadOnlyApplicantProgramService, Exception> errorAnd =
        subject
            .stageAndUpdateIfValid(applicant.id, programDefinition.id(), "1", updates)
            .toCompletableFuture()
            .join();

    assertThat(errorAnd.getResult()).isInstanceOf(ReadOnlyApplicantProgramService.class);
    assertThat(errorAnd.isError()).isFalse();

    ApplicantData applicantDataAfter =
        applicantRepository.lookupApplicantSync(applicant.id).get().getApplicantData();

    assertThat(applicantDataAfter.asJsonString()).contains("Alice", "Doe");
  }

  @Test
  public void stageAndUpdateIfValid_updatesMetadataForQuestionOnce() {
    Applicant applicant = subject.createApplicant(1L).toCompletableFuture().join();

    ImmutableSet<Update> updates =
        ImmutableSet.of(
            Update.create(Path.create("applicant.name.first"), "Alice"),
            Update.create(Path.create("applicant.name.last"), "Doe"));

    ErrorAnd<ReadOnlyApplicantProgramService, Exception> errorAnd =
        subject
            .stageAndUpdateIfValid(applicant.id, programDefinition.id(), "1", updates)
            .toCompletableFuture()
            .join();

    assertThat(errorAnd.isError()).isFalse();
    assertThat(errorAnd.getResult()).isInstanceOf(ReadOnlyApplicantProgramService.class);

    ApplicantData applicantDataAfter =
        applicantRepository.lookupApplicantSync(applicant.id).get().getApplicantData();

    Path programIdPath =
        Path.create("applicant.name." + QuestionDefinition.METADATA_UPDATE_PROGRAM_ID_KEY);
    Path timestampPath =
        Path.create("applicant.name." + QuestionDefinition.METADATA_UPDATE_TIME_KEY);
    assertThat(applicantDataAfter.readLong(programIdPath)).hasValue(programDefinition.id());
    assertThat(applicantDataAfter.readLong(timestampPath)).isPresent();
  }

  @Test
  public void stageAndUpdateIfValid_rawUpdatesContainMultiSelectAnswers_isOk() throws Exception {
    QuestionDefinition multiSelectQuestion =
        questionService
            .create(
                new CheckboxQuestionDefinition(
                    1L,
                    "checkbox",
                    Path.create("applicant.checkbox"),
                    Optional.empty(),
                    "description",
                    LifecycleStage.ACTIVE,
                    ImmutableMap.of(Locale.US, "question?"),
                    ImmutableMap.of(Locale.US, "help text"),
                    ImmutableList.of(
                        QuestionOption.create(1L, ImmutableMap.of(Locale.US, "cat")),
                        QuestionOption.create(2L, ImmutableMap.of(Locale.US, "dog")))))
            .getResult();
    createProgram(multiSelectQuestion);

    Applicant applicant = subject.createApplicant(1L).toCompletableFuture().join();

    ImmutableMap<String, String> rawUpdates =
        ImmutableMap.<String, String>builder()
            .put("applicant.checkbox.selection[0]", "1")
            .put("applicant.checkbox.selection[1]", "2")
            .build();

    ErrorAnd<ReadOnlyApplicantProgramService, Exception> errorAnd =
        subject
            .stageAndUpdateIfValid(applicant.id, programDefinition.id(), "1", rawUpdates)
            .toCompletableFuture()
            .join();

    assertThat(errorAnd.isError()).isFalse();
    assertThat(errorAnd.getResult()).isInstanceOf(ReadOnlyApplicantProgramService.class);

    ApplicantData applicantDataAfter =
        applicantRepository.lookupApplicantSync(applicant.id).get().getApplicantData();

    assertThat(applicantDataAfter.readList(Path.create("applicant.checkbox.selection")))
        .hasValue(ImmutableList.of(1L, 2L));
  }

  @Test
  public void stageAndUpdateIfValid_hasApplicantNotFoundException() {
    ImmutableSet<Update> updates = ImmutableSet.of();
    long badApplicantId = 1L;

    ErrorAnd<ReadOnlyApplicantProgramService, Exception> errorAnd =
        subject
            .stageAndUpdateIfValid(badApplicantId, programDefinition.id(), "1", updates)
            .toCompletableFuture()
            .join();

    assertThat(errorAnd.hasResult()).isFalse();
    assertThat(errorAnd.getErrors()).hasSize(1);
    assertThat(errorAnd.getErrors().asList().get(0)).isInstanceOf(ApplicantNotFoundException.class);
  }

  @Test
  public void stageAndUpdateIfValid_hasProgramNotFoundException() {
    Applicant applicant = subject.createApplicant(1L).toCompletableFuture().join();
    ImmutableSet<Update> updates = ImmutableSet.of();
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
    ImmutableSet<Update> updates = ImmutableSet.of();
    String badBlockId = "100";

    ErrorAnd<ReadOnlyApplicantProgramService, Exception> errorAnd =
        subject
            .stageAndUpdateIfValid(applicant.id, programDefinition.id(), badBlockId, updates)
            .toCompletableFuture()
            .join();

    assertThat(errorAnd.hasResult()).isFalse();
    assertThat(errorAnd.getErrors()).hasSize(1);
    assertThat(errorAnd.getErrors().asList().get(0))
        .isInstanceOf(ProgramBlockNotFoundException.class);
  }

  @Test
  public void stageAndUpdateIfValid_hasPathNotInBlockException() {
    Applicant applicant = subject.createApplicant(1L).toCompletableFuture().join();
    ImmutableSet<Update> updates =
        ImmutableSet.of(
            Update.create(Path.create("applicant.name.first"), "Alice"),
            Update.create(Path.create("this.is.not.in.block"), "Doe"));

    ErrorAnd<ReadOnlyApplicantProgramService, Exception> errorAnd =
        subject
            .stageAndUpdateIfValid(applicant.id, programDefinition.id(), "1", updates)
            .toCompletableFuture()
            .join();

    assertThat(errorAnd.hasResult()).isFalse();
    assertThat(errorAnd.getErrors()).hasSize(1);
    assertThat(errorAnd.getErrors().asList().get(0)).isInstanceOf(PathNotInBlockException.class);
  }

  @Test
  public void stageAndUpdateIfValid_hasIllegalArgumentExceptionForReservedScalarKeys() {
    Applicant applicant = subject.createApplicant(1L).toCompletableFuture().join();
    String reservedScalar = "applicant.name." + QuestionDefinition.METADATA_UPDATE_TIME_KEY;
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
                    1L,
                    "my name",
                    Path.create("applicant.name"),
                    Optional.empty(),
                    "description",
                    LifecycleStage.ACTIVE,
                    ImmutableMap.of(Locale.US, "question?"),
                    ImmutableMap.of(Locale.US, "help text")))
            .getResult();
  }

  private void createProgram() throws Exception {
    createProgram(questionDefinition);
  }

  private void createProgram(QuestionDefinition... questions) throws Exception {
    programDefinition =
        programService
            .createProgramDefinition("test program", "desc", "name", "description")
            .getResult();
    programDefinition =
        programService
            .addBlockToProgram(programDefinition.id(), "test block", "test block description")
            .getResult();
    programDefinition =
        programService.setBlockQuestions(
            programDefinition.id(),
            programDefinition.blockDefinitions().get(0).id(),
            Arrays.stream(questions)
                .map(ProgramQuestionDefinition::create)
                .collect(toImmutableList()));
  }
}
