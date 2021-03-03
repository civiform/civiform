package services.program;

import static org.assertj.core.api.Assertions.assertThat;

import com.google.common.collect.ImmutableMap;
import java.util.Locale;
import org.junit.Test;
import services.question.QuestionDefinition;
import services.question.QuestionDefinitionBuilder;
import services.question.QuestionType;

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
        .setName("The Program")
        .setDescription("This program is for testing.")
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
            .setName("The Program")
            .setDescription("This program is for testing.")
            .addBlockDefinition(blockA)
            .build();

    assertThat(program.getBlockDefinition(0)).hasValue(blockA);
  }

  @Test
  public void getBlockDefinition_outOfBounds_isEmpty() {
    ProgramDefinition program =
        ProgramDefinition.builder()
            .setId(123L)
            .setName("The Program")
            .setDescription("This program is for testing.")
            .build();

    assertThat(program.getBlockDefinition(0)).isEmpty();
  }

  @Test
  public void hasQuestion_trueIfTheProgramUsesTheQuestion() throws Exception {
    QuestionDefinition questionA =
        new QuestionDefinitionBuilder()
            .setId(1L)
            .setVersion(1L)
            .setName("my name")
            .setPath("my.path.name")
            .setDescription("description")
            .setQuestionType(QuestionType.TEXT)
            .setQuestionText(ImmutableMap.of(Locale.ENGLISH, "question?"))
            .setQuestionHelpText(ImmutableMap.of(Locale.ENGLISH, "help text"))
            .build();
    QuestionDefinition questionB =
        new QuestionDefinitionBuilder()
            .setId(2L)
            .setVersion(1L)
            .setName("my name")
            .setPath("my.path.name")
            .setDescription("description")
            .setQuestionType(QuestionType.TEXT)
            .setQuestionText(ImmutableMap.of(Locale.ENGLISH, "question?"))
            .setQuestionHelpText(ImmutableMap.of(Locale.ENGLISH, "help text"))
            .build();
    QuestionDefinition questionC =
        new QuestionDefinitionBuilder()
            .setId(3L)
            .setVersion(1L)
            .setName("my name")
            .setPath("my.path.name")
            .setDescription("description")
            .setQuestionType(QuestionType.TEXT)
            .setQuestionText(ImmutableMap.of(Locale.ENGLISH, "question?"))
            .setQuestionHelpText(ImmutableMap.of(Locale.ENGLISH, "help text"))
            .build();

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
            .setName("The Program")
            .setDescription("This program is for testing.")
            .addBlockDefinition(blockA)
            .addBlockDefinition(blockB)
            .build();

    assertThat(program.hasQuestion(questionA)).isTrue();
    assertThat(program.hasQuestion(questionB)).isTrue();
    assertThat(program.hasQuestion(questionC)).isFalse();
  }
}
