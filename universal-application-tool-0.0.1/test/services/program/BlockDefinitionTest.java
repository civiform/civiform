package services.program;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Optional;
import org.junit.Test;
import services.question.types.QuestionDefinition;
import support.TestQuestionBank;

public class BlockDefinitionTest {

  private static final TestQuestionBank testQuestionBank = new TestQuestionBank(false);

  @Test
  public void createBlockDefinition() {
    BlockDefinition block =
        BlockDefinition.builder()
            .setId(123L)
            .setName("Block Name")
            .setDescription("Block Description")
            .build();

    assertThat(block.id()).isEqualTo(123L);
  }

  @Test
  public void isEnumerator_isFalse() {
    BlockDefinition blockDefinition = makeBlockDefinitionWithQuestions();

    assertThat(blockDefinition.isEnumerator()).isFalse();
  }

  @Test
  public void isRepeated_isFalse() throws Exception {
    BlockDefinition blockDefinition = makeBlockDefinitionWithQuestions();

    assertThat(blockDefinition.isRepeated()).isFalse();
  }

  @Test
  public void isEnumerator_isTrue() {
    BlockDefinition blockDefinition =
        BlockDefinition.builder()
            .setId(123L)
            .setName("Block Name")
            .setDescription("Block Description")
            .addQuestion(
                ProgramQuestionDefinition.create(
                    testQuestionBank.applicantHouseholdMembers().getQuestionDefinition()))
            .build();

    assertThat(blockDefinition.isEnumerator()).isTrue();
  }

  @Test
  public void isRepeated_isTrue() {
    BlockDefinition blockDefinition =
        makeBlockDefinitionWithQuestions().toBuilder().setEnumeratorId(Optional.of(1L)).build();

    assertThat(blockDefinition.isRepeated()).isTrue();
  }

  private BlockDefinition makeBlockDefinitionWithQuestions() {
    QuestionDefinition nameQuestion = testQuestionBank.applicantName().getQuestionDefinition();
    QuestionDefinition addressQuestion =
        testQuestionBank.applicantAddress().getQuestionDefinition();
    QuestionDefinition colorQuestion =
        testQuestionBank.applicantFavoriteColor().getQuestionDefinition();

    BlockDefinition block =
        BlockDefinition.builder()
            .setId(123L)
            .setName("Block Name")
            .setDescription("Block Description")
            .addQuestion(ProgramQuestionDefinition.create(nameQuestion))
            .addQuestion(ProgramQuestionDefinition.create(addressQuestion))
            .addQuestion(ProgramQuestionDefinition.create(colorQuestion))
            .build();
    return block;
  }
}
