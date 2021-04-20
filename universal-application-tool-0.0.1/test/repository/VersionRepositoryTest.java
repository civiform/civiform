package repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

import java.util.ArrayList;
import java.util.List;
import models.LifecycleStage;
import models.Program;
import models.Question;
import org.junit.Before;
import org.junit.Test;
import services.program.DuplicateProgramQuestionException;
import services.program.ProgramBlockNotFoundException;
import services.program.ProgramDefinition;
import services.program.ProgramNotFoundException;
import services.program.ProgramService;
import services.question.exceptions.QuestionNotFoundException;

public class VersionRepositoryTest extends WithPostgresContainer {

  private ProgramRepository programRepository;
  private VersionRepository versionRepository;
  private ProgramService programService;

  @Before
  public void setupProgramRepository() {
    programRepository = instanceOf(ProgramRepository.class);
    versionRepository = instanceOf(VersionRepository.class);
    programService = instanceOf(ProgramService.class);
  }

  @Test
  public void publishProgram_simple() {
    Program active = resourceCreator().insertProgram("name");
    active.setLifecycleStage(LifecycleStage.ACTIVE);
    active.setVersion(1L);
    active.save();
    Program draft = resourceCreator().insertProgram("name");
    draft.setLifecycleStage(LifecycleStage.DRAFT);
    draft.setVersion(2L);
    draft.save();

    versionRepository.publishNewSynchronizedVersion();

    draft.refresh();
    assertThat(draft.getLifecycleStage()).isEqualTo(LifecycleStage.ACTIVE);
    assertThat(draft.getVersion()).isEqualTo(2);
    active.refresh();
    assertThat(active.getLifecycleStage()).isEqualTo(LifecycleStage.OBSOLETE);
    assertThat(active.getVersion()).isEqualTo(1);
  }

  @Test
  public void publishProgram_passthrough() {
    Program active = resourceCreator().insertProgram("passthrough");
    active.setLifecycleStage(LifecycleStage.ACTIVE);
    active.setVersion(1L);
    active.save();

    versionRepository.publishNewSynchronizedVersion();

    active.refresh();
    assertThat(active.getLifecycleStage()).isEqualTo(LifecycleStage.OBSOLETE);

    programRepository
        .listPrograms()
        .thenApplyAsync(
            programs -> {
              List<Program> programsWithName = new ArrayList<>();
              for (Program program : programs) {
                if (program.getProgramDefinition().adminName().equals("passthrough")) {
                  programsWithName.add(program);
                }
              }
              assertThat(programsWithName).hasSize(2);
              return programsWithName;
            })
        .toCompletableFuture()
        .join()
        .stream()
        .forEach(
            program -> {
              switch (program.getVersion().intValue()) {
                case 1:
                  assertThat(program.getLifecycleStage()).isEqualTo(LifecycleStage.OBSOLETE);
                  break;
                case 2:
                  assertThat(program.getLifecycleStage()).isEqualTo(LifecycleStage.ACTIVE);
                  break;
                default:
                  fail("Found unexpected version: %s", program.getVersion());
              }
            });
  }

  @Test
  public void publishQuestion_simple()
      throws ProgramNotFoundException, DuplicateProgramQuestionException, QuestionNotFoundException,
          ProgramBlockNotFoundException {
    // Create program, version 1, with an old version of this question.
    Question question1 = resourceCreator().insertQuestion("foo.bar", 1, "q");
    Program program = resourceCreator().insertProgram("program", LifecycleStage.ACTIVE);
    resourceCreator().addQuestionToProgram(program, question1);
    program.refresh();
    assertThat(program.getVersion()).isEqualTo(1);

    // Create question 2, in draft state.
    Question question2 = resourceCreator().insertQuestion("foo.bar", 2, "q");
    question2.setLifecycleStage(LifecycleStage.DRAFT);
    question2.save();

    // Create a new draft of program.
    ProgramDefinition draft = programService.newDraftOf(program.id);
    assertThat(draft.blockDefinitions()).hasSize(1);
    assertThat(draft.blockDefinitions().get(0).programQuestionDefinitions()).hasSize(1);
    // Confirm that the new draft includes the new version of the question.
    assertThat(draft.blockDefinitions().get(0).programQuestionDefinitions().get(0).id())
        .isEqualTo(question2.id);
    question2.refresh();
    assertThat(question2.getLifecycleStage()).isEqualTo(LifecycleStage.DRAFT);

    versionRepository.publishNewSynchronizedVersion();

    question2.refresh();
    assertThat(question2.getLifecycleStage()).isEqualTo(LifecycleStage.ACTIVE);
    question1.refresh();
    assertThat(question1.getLifecycleStage()).isEqualTo(LifecycleStage.OBSOLETE);
  }
}
