package services.program;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Locale;
import models.LifecycleStage;
import org.junit.Test;
import services.question.types.QuestionDefinition;
import support.TestQuestionBank;

public class ProgramDefinitionTest {

  @Test
  public void createProgramDefinition() {
    BlockDefinition blockA =
        BlockDefinition.builder()
            .setId(123L)
            .setName("Block Name")
            .setDescription("Block Description")
            .build();
    ProgramDefinition.builder()
        .setId(123L)
        .addName(Locale.US, "The Program")
        .addDescription(Locale.US, "This program is for testing.")
        .setLifecycleStage(LifecycleStage.ACTIVE)
        .addBlockDefinition(blockA)
        .build();
  }

  @Test
  public void getBlockDefinition_hasValue() {
    BlockDefinition blockA =
        BlockDefinition.builder()
            .setId(123L)
            .setName("Block Name")
            .setDescription("Block Description")
            .build();
    ProgramDefinition program =
        ProgramDefinition.builder()
            .setId(123L)
            .addName(Locale.US, "The Program")
            .addDescription(Locale.US, "This program is for testing.")
            .setLifecycleStage(LifecycleStage.ACTIVE)
            .addBlockDefinition(blockA)
            .build();

    assertThat(program.getBlockDefinitionByIndex(0)).hasValue(blockA);
  }

  @Test
  public void getBlockDefinition_outOfBounds_isEmpty() {
    ProgramDefinition program =
        ProgramDefinition.builder()
            .setId(123L)
            .addName(Locale.US, "The Program")
            .addDescription(Locale.US, "This program is for testing.")
            .setLifecycleStage(LifecycleStage.ACTIVE)
            .build();

    assertThat(program.getBlockDefinitionByIndex(0)).isEmpty();
  }

  @Test
  public void hasQuestion_trueIfTheProgramUsesTheQuestion() {
    QuestionDefinition questionA = TestQuestionBank.applicantName().getQuestionDefinition();
    QuestionDefinition questionB = TestQuestionBank.applicantAddress().getQuestionDefinition();
    QuestionDefinition questionC =
        TestQuestionBank.applicantFavoriteColor().getQuestionDefinition();

    BlockDefinition blockA =
        BlockDefinition.builder()
            .setId(123L)
            .setName("Block Name")
            .setDescription("Block Description")
            .addQuestion(ProgramQuestionDefinition.create(questionA))
            .build();

    BlockDefinition blockB =
        BlockDefinition.builder()
            .setId(321L)
            .setName("Block Name")
            .setDescription("Block Description")
            .addQuestion(ProgramQuestionDefinition.create(questionB))
            .build();

    ProgramDefinition program =
        ProgramDefinition.builder()
            .setId(123L)
            .addName(Locale.US, "The Program")
            .addDescription(Locale.US, "This program is for testing.")
            .setLifecycleStage(LifecycleStage.ACTIVE)
            .addBlockDefinition(blockA)
            .addBlockDefinition(blockB)
            .build();

    assertThat(program.hasQuestion(questionA)).isTrue();
    assertThat(program.hasQuestion(questionB)).isTrue();
    assertThat(program.hasQuestion(questionC)).isFalse();
  }
}
