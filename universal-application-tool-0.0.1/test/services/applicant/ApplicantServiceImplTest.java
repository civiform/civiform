package services.applicant;

import static org.assertj.core.api.Assertions.assertThat;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import java.util.Locale;
import java.util.Optional;
import models.Applicant;
import org.junit.Before;
import org.junit.Test;
import repository.WithPostgresContainer;
import services.program.ProgramDefinition;
import services.program.ProgramService;
import services.program.ProgramServiceImpl;
import services.question.QuestionDefinition;
import services.question.QuestionService;

public class ApplicantServiceImplTest extends WithPostgresContainer {

  private ApplicantServiceImpl subject;
  private ProgramService programService;
  private QuestionService questionService;
  private QuestionDefinition questionDefinition;
  private ProgramDefinition programDefinition;

  @Before
  public void setUp() throws Exception {
    subject = app.injector().instanceOf(ApplicantServiceImpl.class);
    programService = app.injector().instanceOf(ProgramServiceImpl.class);
    questionService = app.injector().instanceOf(QuestionService.class);

    createQuestions();
    createProgram();
  }

  @Test
  public void createApplicant_createsANewApplicant() {
    Applicant applicant = subject.createApplicant(1l).toCompletableFuture().join();

    assertThat(applicant.id).isNotNull();
  }

  @Test
  public void update_aNewValidTextScalarPath_buildsThePathAndUpdates() {
  }

  @Test
  public void getReadOnlyApplicantService_getsReadOnlyApplicantServiceForTheApplicantAndProgram() {
    Applicant applicant = subject.createApplicant(1l).toCompletableFuture().join();

    ReadOnlyApplicantProgramService roApplicantProgramService = subject
        .getReadOnlyApplicantProgramService(applicant.id, programDefinition.id())
        .toCompletableFuture().join();

    assertThat(roApplicantProgramService).isInstanceOf(ReadOnlyApplicantProgramService.class);
  }

  private void createQuestions() {
    questionDefinition = questionService.create(new QuestionDefinition(
        123L,
        1L,
        "my name",
        "my.path.name",
        "description",
        ImmutableMap.of(Locale.ENGLISH, "question?"),
        Optional.of(ImmutableMap.of(Locale.ENGLISH, "help text")))).get();
  }

  private void createProgram() throws Exception {
    programDefinition = programService.createProgramDefinition("test program", "desc");
    programDefinition = programService
        .addBlockToProgram(programDefinition.id(), "test block", "test block description");
    programDefinition = programService
        .setBlockQuestions(programDefinition.id(), programDefinition.blockDefinitions().get(0).id(),
            ImmutableList
                .of(questionDefinition));
  }
}
