package services.applicant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.catchThrowable;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import java.util.Locale;
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
import services.question.NameQuestionDefinition;
import services.question.QuestionDefinition;
import services.question.QuestionService;

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
                applicant.id, programDefinition.id(), 1L, ImmutableSet.<Update>builder().build())
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
            .stageAndUpdateIfValid(applicant.id, programDefinition.id(), 1L, updates)
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
            .stageAndUpdateIfValid(applicant.id, programDefinition.id(), 1L, updates)
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
  public void stageAndUpdateIfValid_hasApplicantNotFoundException() {
    ImmutableSet<Update> updates = ImmutableSet.of();
    long badApplicantId = 1L;

    ErrorAnd<ReadOnlyApplicantProgramService, Exception> errorAnd =
        subject
            .stageAndUpdateIfValid(badApplicantId, programDefinition.id(), 1L, updates)
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
                    .stageAndUpdateIfValid(applicant.id, badProgramId, 1L, updates)
                    .toCompletableFuture()
                    .join());

    assertThat(thrown).isInstanceOf(CompletionException.class);
    assertThat(thrown).hasCauseInstanceOf(ProgramNotFoundException.class);
  }

  @Test
  public void stageAndUpdateIfValid_hasProgramBlockNotFoundException() {
    Applicant applicant = subject.createApplicant(1L).toCompletableFuture().join();
    ImmutableSet<Update> updates = ImmutableSet.of();
    long badBlockId = 100L;

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
            .stageAndUpdateIfValid(applicant.id, programDefinition.id(), 1L, updates)
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
                    .stageAndUpdateIfValid(applicant.id, programDefinition.id(), 1L, updates)
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
                    "description",
                    LifecycleStage.ACTIVE,
                    ImmutableMap.of(Locale.US, "question?"),
                    ImmutableMap.of(Locale.US, "help text")))
            .getResult();
  }

  private void createProgram() throws Exception {
    programDefinition = programService.createProgramDefinition("test program", "desc").getResult();
    programDefinition =
        programService
            .addBlockToProgram(programDefinition.id(), "test block", "test block description")
            .getResult();
    programDefinition =
        programService.setBlockQuestions(
            programDefinition.id(),
            programDefinition.blockDefinitions().get(0).id(),
            ImmutableList.of(ProgramQuestionDefinition.create(questionDefinition)));
  }
}
