package services;

import static org.assertj.core.api.Assertions.assertThat;

import models.Question;
import models.Version;
import org.junit.Before;
import org.junit.Test;
import repository.ResetPostgres;
import repository.VersionRepository;
import services.program.ProgramDefinition;
import services.question.QuestionService;
import support.ProgramBuilder;

public class ProgramBlockValidationFactoryTest extends ResetPostgres {

  ProgramBlockValidation programBlockValidation;
  QuestionService questionService;
  VersionRepository versionRepository;
  Question questionForTombstone;

  @Before
  public void setProgramBlockValidation()
      throws services.question.exceptions.QuestionNotFoundException {
    questionService = instanceOf(services.question.QuestionService.class);
    versionRepository = instanceOf(repository.VersionRepository.class);
    Version version = versionRepository.getDraftVersion();
    String tombstonedQuestionOneName = "tombstoneOne";
    questionForTombstone = resourceCreator.insertQuestion(tombstonedQuestionOneName);
    version.addQuestion(questionForTombstone);
    version.addTombstoneForQuestion(questionForTombstone);
    version.save();
    ProgramBlockValidationFactory programBlockValidationFactory =
        new ProgramBlockValidationFactory(versionRepository);
    programBlockValidation = programBlockValidationFactory.create();
  }

  @Test
  public void canAddQuestions_cantAddArchivedQuestion() throws Exception {
    ProgramDefinition program =
        ProgramBuilder.newDraftProgram("program1")
            .withBlock()
            .withRequiredQuestionDefinition(questionForTombstone.getQuestionDefinition())
            .buildDefinition();
    assertThat(
            programBlockValidation.canAddQuestion(
                program,
                program.getLastBlockDefinition(),
                questionForTombstone.getQuestionDefinition()))
        .isEqualTo(services.ProgramBlockValidation.AddQuestionResult.QUESTION_TOMBSTONED);
  }
}
