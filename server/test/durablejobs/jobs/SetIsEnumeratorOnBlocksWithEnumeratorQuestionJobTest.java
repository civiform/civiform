package durablejobs.jobs;

import static org.assertj.core.api.Assertions.assertThat;

import durablejobs.DurableJobName;
import java.time.Instant;
import models.JobType;
import models.PersistedDurableJobModel;
import models.ProgramModel;
import models.QuestionModel;
import org.junit.Before;
import org.junit.Test;
import repository.ProgramRepository;
import repository.QuestionRepository;
import repository.ResetPostgres;
import services.program.ProgramDefinition;
import services.program.ProgramNotFoundException;
import services.program.ProgramService;
import support.ProgramBuilder;

public class SetIsEnumeratorOnBlocksWithEnumeratorQuestionJobTest extends ResetPostgres {
  ProgramRepository programRepository;
  QuestionRepository questionRepository;
  ProgramService programService;
  PersistedDurableJobModel jobModel =
      new PersistedDurableJobModel(
          DurableJobName.SET_IS_ENUMERATOR_ON_BLOCKS_WITH_ENUMERATOR_QUESTION_20260106.toString(),
          JobType.RUN_ONCE,
          Instant.now());
  QuestionModel enumQuestion;
  QuestionModel nonEnumQuestion;
  SetIsEnumeratorOnBlocksWithEnumeratorQuestionJob job;

  @Before
  public void setUp() {
    programRepository = instanceOf(ProgramRepository.class);
    questionRepository = instanceOf(QuestionRepository.class);
    programService = instanceOf(ProgramService.class);
    enumQuestion = resourceCreator.insertEnum("enum-question");
    nonEnumQuestion = resourceCreator.insertQuestion("non-enum-question");
    job =
        new SetIsEnumeratorOnBlocksWithEnumeratorQuestionJob(
            jobModel, programRepository, questionRepository);
  }

  // Verifies that blocks with enumerator questions are updated to have isEnumerator = true.
  @Test
  public void run_setsIsEnumeratorOnBlocksWithAnEnumeratorQuestion()
      throws ProgramNotFoundException {
    ProgramModel program =
        ProgramBuilder.newDraftProgram("program-with-enum")
            .withBlock("block-with-enumerator-question")
            .withRequiredQuestion(enumQuestion)
            .build();

    ProgramDefinition foundProgramBeforeJob = programService.getFullProgramDefinition(program.id);
    assertThat(foundProgramBeforeJob.getBlockDefinitionByIndex(0).get().getIsEnumerator())
        .isFalse();

    job.run();

    ProgramDefinition foundProgramAfterJob = programService.getFullProgramDefinition(program.id);
    assertThat(foundProgramAfterJob.getBlockDefinitionByIndex(0).get().getIsEnumerator()).isTrue();
  }

  // Ensures that blocks without enumerator questions remain unchanged (isEnumerator = false).
  @Test
  public void run_doesNotSetIsEnumeratorOnBlocksWithoutAnEnumeratorQuestion()
      throws ProgramNotFoundException {
    ProgramModel program =
        ProgramBuilder.newDraftProgram("program-without-enum")
            .withBlock("block-without-enumerator-question")
            .withRequiredQuestion(nonEnumQuestion)
            .build();

    ProgramDefinition foundProgramBeforeJob = programService.getFullProgramDefinition(program.id);
    assertThat(foundProgramBeforeJob.getBlockDefinitionByIndex(0).get().getIsEnumerator())
        .isFalse();

    job.run();

    ProgramDefinition foundProgramAfterJob = programService.getFullProgramDefinition(program.id);
    assertThat(foundProgramAfterJob.getBlockDefinitionByIndex(0).get().getIsEnumerator()).isFalse();
  }

  // Checks that only blocks containing enumerator questions are updated in a multi-block program.
  @Test
  public void run_setsIsEnumeratorOnlyOnBlocksWithEnumeratorQuestions_withMultipleBlocks()
      throws ProgramNotFoundException {
    ProgramModel program =
        ProgramBuilder.newDraftProgram("multi-block-program")
            .withBlock("block-1")
            .withRequiredQuestion(enumQuestion)
            .withBlock("block-2")
            .withRequiredQuestion(nonEnumQuestion)
            .build();

    job.run();

    ProgramDefinition foundProgram = programService.getFullProgramDefinition(program.id);
    assertThat(foundProgram.getBlockDefinitionByIndex(0).get().getIsEnumerator()).isTrue();
    assertThat(foundProgram.getBlockDefinitionByIndex(1).get().getIsEnumerator()).isFalse();
  }

  // Confirms that running the job multiple times does not change the result or cause errors.
  @Test
  public void run_canRunMultipleTimes() throws ProgramNotFoundException {
    ProgramModel program =
        ProgramBuilder.newDraftProgram("program-with-enum")
            .withBlock("block-with-enumerator-question")
            .withRequiredQuestion(enumQuestion)
            .build();

    ProgramDefinition foundProgramBeforeJob = programService.getFullProgramDefinition(program.id);
    assertThat(foundProgramBeforeJob.getBlockDefinitionByIndex(0).get().getIsEnumerator())
        .isFalse();

    job.run();

    ProgramDefinition foundProgramAfterJob = programService.getFullProgramDefinition(program.id);
    assertThat(foundProgramAfterJob.getBlockDefinitionByIndex(0).get().getIsEnumerator()).isTrue();

    job.run();

    ProgramDefinition foundProgramAfterSecondRun =
        programService.getFullProgramDefinition(program.id);
    assertThat(foundProgramAfterSecondRun.getBlockDefinitionByIndex(0).get().getIsEnumerator())
        .isTrue();
  }
}
