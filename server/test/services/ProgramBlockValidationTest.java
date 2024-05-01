package services;

import static org.assertj.core.api.Assertions.assertThat;

import models.QuestionModel;
import models.VersionModel;
import org.junit.Before;
import org.junit.Test;
import repository.ResetPostgres;
import repository.VersionRepository;
import services.ProgramBlockValidation.AddQuestionResult;
import services.program.ProgramDefinition;
import services.question.QuestionService;
import services.question.types.QuestionDefinition;
import support.ProgramBuilder;

public class ProgramBlockValidationTest extends ResetPostgres {

  private ProgramBlockValidation programBlockValidation;
  private VersionRepository versionRepository;
  private QuestionModel questionForTombstone;
  private QuestionService questionService;
  private QuestionModel questionForEligible;
  private VersionModel version;
  private QuestionModel householdMemberQuestion;
  private QuestionModel householdMemberNameQuestion;

  @Before
  public void setProgramBlockValidation()
      throws services.question.exceptions.QuestionNotFoundException {
    versionRepository = instanceOf(repository.VersionRepository.class);
    questionService = instanceOf(services.question.QuestionService.class);
    version = versionRepository.getDraftVersionOrCreate();
    String tombstonedQuestionOneName = "tombstoneOne";
    questionForTombstone = resourceCreator.insertQuestion(tombstonedQuestionOneName);
    version.addQuestion(questionForTombstone);
    versionRepository.addTombstoneForQuestionInVersion(questionForTombstone, version);
    version.save();
    questionForEligible = resourceCreator.insertQuestion("eligible question");
    version.addQuestion(questionForEligible);
    householdMemberQuestion = resourceCreator.insertEnum("householdMemberQuestion");
    householdMemberNameQuestion =
        resourceCreator.insertEnumQuestion("householdMemberWageQuestion", householdMemberQuestion);
    version.addQuestion(householdMemberQuestion);
    version.addQuestion(householdMemberNameQuestion);
    version.save();
    ProgramBlockValidationFactory programBlockValidationFactory =
        new ProgramBlockValidationFactory(versionRepository, questionService);
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

  @Test
  public void canAddQuestions_cantAddQuestionNotInActiveDraftState() throws Exception {
    QuestionDefinition householdMemberQuestion =
        testQuestionBank.applicantHouseholdMembers().getQuestionDefinition();
    QuestionDefinition householdMemberNameQuestion =
        testQuestionBank.applicantHouseholdMemberName().getQuestionDefinition();
    ProgramDefinition program =
        ProgramBuilder.newDraftProgram("program1")
            .withBlock()
            .withRequiredQuestionDefinition(householdMemberQuestion)
            .withRepeatedBlock()
            .buildDefinition();
    assertThat(
            programBlockValidation.canAddQuestion(
                program, program.getLastBlockDefinition(), householdMemberNameQuestion))
        .isEqualTo(
            services.ProgramBlockValidation.AddQuestionResult
                .QUESTION_NOT_IN_ACTIVE_OR_DRAFT_STATE);
  }

  @Test
  public void canAddQuestion_eligible() throws Exception {
    QuestionDefinition question = questionForEligible.getQuestionDefinition();
    ProgramDefinition program =
        ProgramBuilder.newDraftProgram("program1").withBlock().buildDefinition();
    assertThat(
            programBlockValidation.canAddQuestion(
                program, program.getLastBlockDefinition(), question))
        .isEqualTo(AddQuestionResult.ELIGIBLE);
  }

  @Test
  public void canAddQuestion_duplicate() throws Exception {
    QuestionDefinition question = testQuestionBank.applicantName().getQuestionDefinition();
    ProgramDefinition program =
        ProgramBuilder.newDraftProgram("program1")
            .withBlock()
            .withRequiredQuestionDefinition(question)
            .buildDefinition();
    assertThat(
            programBlockValidation.canAddQuestion(
                program, program.getLastBlockDefinition(), question))
        .isEqualTo(AddQuestionResult.DUPLICATE);
  }

  @Test
  public void canAddQuestion_blockIsSingleQuestion() throws Exception {
    QuestionDefinition nameQuestion = testQuestionBank.applicantName().getQuestionDefinition();
    QuestionDefinition fileQuestion = testQuestionBank.applicantFile().getQuestionDefinition();
    ProgramDefinition program =
        ProgramBuilder.newDraftProgram("program1")
            .withBlock()
            .withRequiredQuestionDefinition(fileQuestion)
            .buildDefinition();
    assertThat(
            programBlockValidation.canAddQuestion(
                program, program.getLastBlockDefinition(), nameQuestion))
        .isEqualTo(AddQuestionResult.BLOCK_IS_SINGLE_QUESTION);
  }

  @Test
  public void canAddQuestion_cantAddSingleQuestionBlock() throws Exception {
    QuestionDefinition nameQuestion = testQuestionBank.applicantName().getQuestionDefinition();
    QuestionDefinition fileQuestion = testQuestionBank.applicantFile().getQuestionDefinition();
    ProgramDefinition program =
        ProgramBuilder.newDraftProgram("program1")
            .withBlock()
            .withRequiredQuestionDefinition(nameQuestion)
            .buildDefinition();
    assertThat(
            programBlockValidation.canAddQuestion(
                program, program.getLastBlockDefinition(), fileQuestion))
        .isEqualTo(AddQuestionResult.CANT_ADD_SINGLE_BLOCK_QUESTION_TO_NON_EMPTY_BLOCK);
  }

  @Test
  public void canAddQuestion_cantAddEmuratorQuestionToNonEnumeratorBlock() throws Exception {
    QuestionDefinition question =
        testQuestionBank.applicantHouseholdMemberName().getQuestionDefinition();
    ProgramDefinition program =
        ProgramBuilder.newDraftProgram("program1").withBlock().buildDefinition();
    assertThat(
            programBlockValidation.canAddQuestion(
                program, program.getLastBlockDefinition(), question))
        .isEqualTo(AddQuestionResult.ENUMERATOR_MISMATCH);
  }

  @Test
  public void canAddQuestion_cantAddNonEmuratorQuestionToEnumeratorBlock() throws Exception {
    QuestionDefinition nameQuestion = testQuestionBank.applicantName().getQuestionDefinition();
    QuestionDefinition householdMemberQuestion =
        testQuestionBank.applicantHouseholdMembers().getQuestionDefinition();
    ProgramDefinition program =
        ProgramBuilder.newDraftProgram("program1")
            .withBlock()
            .withRequiredQuestionDefinition(householdMemberQuestion)
            .withRepeatedBlock()
            .buildDefinition();
    assertThat(
            programBlockValidation.canAddQuestion(
                program, program.getLastBlockDefinition(), nameQuestion))
        .isEqualTo(AddQuestionResult.ENUMERATOR_MISMATCH);
  }

  @Test
  public void canAddQuestion_canAddEmuratorQuestionToEnumeratorBlock() throws Exception {
    ProgramDefinition program =
        ProgramBuilder.newDraftProgram("program1")
            .withBlock()
            .withRequiredQuestionDefinition(householdMemberQuestion.getQuestionDefinition())
            .withRepeatedBlock()
            .buildDefinition();
    assertThat(
            programBlockValidation.canAddQuestion(
                program,
                program.getLastBlockDefinition(),
                householdMemberNameQuestion.getQuestionDefinition()))
        .isEqualTo(AddQuestionResult.ELIGIBLE);
  }
}
