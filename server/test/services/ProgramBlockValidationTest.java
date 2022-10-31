package services;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.Test;
import repository.ResetPostgres;
import services.ProgramBlockValidation.AddQuestionResult;
import services.program.ProgramDefinition;
import services.question.types.QuestionDefinition;
import support.ProgramBuilder;

public class ProgramBlockValidationTest extends ResetPostgres {

  @Test
  public void canAddQuestion_eligible() throws Exception {
    QuestionDefinition question = testQuestionBank.applicantName().getQuestionDefinition();
    ProgramDefinition program =
        ProgramBuilder.newDraftProgram("program1").withBlock().buildDefinition();
    assertThat(
            ProgramBlockValidation.canAddQuestion(
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
            ProgramBlockValidation.canAddQuestion(
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
            ProgramBlockValidation.canAddQuestion(
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
            ProgramBlockValidation.canAddQuestion(
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
            ProgramBlockValidation.canAddQuestion(
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
            ProgramBlockValidation.canAddQuestion(
                program, program.getLastBlockDefinition(), nameQuestion))
        .isEqualTo(AddQuestionResult.ENUMERATOR_MISMATCH);
  }
}
