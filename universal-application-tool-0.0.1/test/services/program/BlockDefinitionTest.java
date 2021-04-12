package services.program;

import static org.assertj.core.api.Assertions.assertThat;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import java.util.Locale;
import java.util.Optional;
import models.LifecycleStage;
import org.junit.Test;
import services.Path;
import services.question.types.QuestionDefinition;
import services.question.types.QuestionDefinitionBuilder;
import services.question.types.QuestionType;
import services.question.types.ScalarType;
import support.TestQuestionBank;

public class BlockDefinitionTest {

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
  public void getScalarType() throws Exception {
    BlockDefinition block = makeBlockDefinitionWithQuestions();
    assertThat(block.getScalarType(Path.create("applicant.name.first")))
        .hasValue(ScalarType.STRING);
    assertThat(block.getScalarType(Path.create("applicant.name.middle")))
        .hasValue(ScalarType.STRING);
    assertThat(block.getScalarType(Path.create("applicant.name.last"))).hasValue(ScalarType.STRING);
    assertThat(block.getScalarType(Path.create("applicant.address.street")))
        .hasValue(ScalarType.STRING);
    assertThat(block.getScalarType(Path.create("applicant.address.city")))
        .hasValue(ScalarType.STRING);
    assertThat(block.getScalarType(Path.create("applicant.address.state")))
        .hasValue(ScalarType.STRING);
    assertThat(block.getScalarType(Path.create("applicant.address.zip")))
        .hasValue(ScalarType.STRING);
    assertThat(block.getScalarType(Path.create("applicant.color.text")))
        .hasValue(ScalarType.STRING);
    assertThat(block.getScalarType(Path.create("fake.path"))).isEmpty();
  }

  @Test
  public void hasPaths() throws Exception {
    BlockDefinition block = makeBlockDefinitionWithQuestions();
    ImmutableList<Path> paths =
        ImmutableList.of(
            Path.create("applicant.name.first"),
            Path.create("applicant.name.middle"),
            Path.create("applicant.name.last"),
            Path.create("applicant.address.street"),
            Path.create("applicant.address.city"),
            Path.create("applicant.address.state"),
            Path.create("applicant.address.zip"),
            Path.create("applicant.color.text"));

    assertThat(block.hasPaths(paths)).isTrue();

    assertThat(block.hasPaths(Path.create("fake.path"))).isFalse();
  }

  @Test
  public void isRepeater_isFalse() throws Exception {
    BlockDefinition blockDefinition = makeBlockDefinitionWithQuestions();

    assertThat(blockDefinition.isRepeater()).isFalse();
  }

  @Test
  public void isRepeated_isFalse() throws Exception {
    BlockDefinition blockDefinition = makeBlockDefinitionWithQuestions();

    assertThat(blockDefinition.isRepeated()).isFalse();
  }

  @Test
  public void isRepeater_isTrue() throws Exception {
    BlockDefinition blockDefinition =
        BlockDefinition.builder()
            .setId(123L)
            .setName("Block Name")
            .setDescription("Block Description")
            .addQuestion(
                ProgramQuestionDefinition.create(TestQuestionBank.applicantHouseholdMembers().getQuestionDefinition()))
            .build();

    assertThat(blockDefinition.isRepeater()).isTrue();
  }

  @Test
  public void isRepeated_isTrue() throws Exception {
    BlockDefinition blockDefinition =
        makeBlockDefinitionWithQuestions().toBuilder().setRepeaterId(Optional.of(1L)).build();

    assertThat(blockDefinition.isRepeated()).isTrue();
  }

  private BlockDefinition makeBlockDefinitionWithQuestions() throws Exception {
    QuestionDefinition nameQuestion = TestQuestionBank.applicantName().getQuestionDefinition();
    QuestionDefinition addressQuestion = TestQuestionBank.applicantAddress().getQuestionDefinition();
    QuestionDefinition colorQuestion = TestQuestionBank.applicantFavoriteColor().getQuestionDefinition();

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
