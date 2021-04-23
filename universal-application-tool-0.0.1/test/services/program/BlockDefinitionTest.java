package services.program;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Optional;
import org.junit.Test;
import services.Path;
import services.question.types.QuestionDefinition;
import services.question.types.ScalarType;
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
  public void getScalarType() {
    BlockDefinition block = makeBlockDefinitionWithQuestions();
    assertThat(block.getScalarType(Path.create("applicant.applicant_name.first")))
        .hasValue(ScalarType.STRING);
    assertThat(block.getScalarType(Path.create("applicant.applicant_name.middle")))
        .hasValue(ScalarType.STRING);
    assertThat(block.getScalarType(Path.create("applicant.applicant_name.last")))
        .hasValue(ScalarType.STRING);
    assertThat(block.getScalarType(Path.create("applicant.applicant_address.street")))
        .hasValue(ScalarType.STRING);
    assertThat(block.getScalarType(Path.create("applicant.applicant_address.city")))
        .hasValue(ScalarType.STRING);
    assertThat(block.getScalarType(Path.create("applicant.applicant_address.state")))
        .hasValue(ScalarType.STRING);
    assertThat(block.getScalarType(Path.create("applicant.applicant_address.zip")))
        .hasValue(ScalarType.STRING);
    assertThat(block.getScalarType(Path.create("applicant.applicant_favorite_color.text")))
        .hasValue(ScalarType.STRING);
    assertThat(block.getScalarType(Path.create("fake.path"))).isEmpty();
  }

  @Test
  public void isRepeater_isFalse() {
    BlockDefinition blockDefinition = makeBlockDefinitionWithQuestions();

    assertThat(blockDefinition.isRepeater()).isFalse();
  }

  @Test
  public void isRepeated_isFalse() throws Exception {
    BlockDefinition blockDefinition = makeBlockDefinitionWithQuestions();

    assertThat(blockDefinition.isRepeated()).isFalse();
  }

  @Test
  public void isRepeater_isTrue() {
    BlockDefinition blockDefinition =
        BlockDefinition.builder()
            .setId(123L)
            .setName("Block Name")
            .setDescription("Block Description")
            .addQuestion(
                ProgramQuestionDefinition.create(
                    testQuestionBank.applicantHouseholdMembers().getQuestionDefinition()))
            .build();

    assertThat(blockDefinition.isRepeater()).isTrue();
  }

  @Test
  public void isRepeated_isTrue() {
    BlockDefinition blockDefinition =
        makeBlockDefinitionWithQuestions().toBuilder().setRepeaterId(Optional.of(1L)).build();

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
