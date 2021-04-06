package support;

import static org.assertj.core.api.Assertions.assertThat;

import com.google.common.collect.ImmutableMap;
import java.util.Locale;
import models.LifecycleStage;
import models.Program;
import org.junit.Test;
import services.Path;
import services.program.BlockDefinition;
import services.program.ProgramDefinition;
import services.question.NameQuestionDefinition;
import services.question.QuestionDefinition;
import services.question.QuestionDefinitionBuilder;
import services.question.QuestionType;

public class ProgramBuilderTest {
  @Test
  public void fluentlyCreateProgramWithBlocks() {
    ProgramDefinition programDefinition =
        ProgramBuilder.newProgram("name", "description")
            .withName("a new name")
            .withDescription("a new description")
            .withBlock()
            .withName("block 1")
            .withDescription("block 1 description")
            .withBlock()
            .withName("block 2")
            .withDescription("block 2 description")
            .withBlock("block 3", "block 3 description")
            .buildDefinition();

    assertThat(programDefinition.id()).isGreaterThan(0);
    assertThat(programDefinition.name()).isEqualTo("a new name");
    assertThat(programDefinition.description()).isEqualTo("a new description");

    assertThat(programDefinition.blockDefinitions()).hasSize(3);
    assertThat(programDefinition.getBlockDefinitionByIndex(0).get().id()).isEqualTo(1L);
    assertThat(programDefinition.getBlockDefinitionByIndex(0).get().name()).isEqualTo("block 1");
    assertThat(programDefinition.getBlockDefinitionByIndex(0).get().description())
        .isEqualTo("block 1 description");
    assertThat(programDefinition.getBlockDefinitionByIndex(1).get().id()).isEqualTo(2L);
    assertThat(programDefinition.getBlockDefinitionByIndex(1).get().name()).isEqualTo("block 2");
    assertThat(programDefinition.getBlockDefinitionByIndex(1).get().description())
        .isEqualTo("block 2 description");
    assertThat(programDefinition.getBlockDefinitionByIndex(2).get().id()).isEqualTo(3L);
    assertThat(programDefinition.getBlockDefinitionByIndex(2).get().name()).isEqualTo("block 3");
    assertThat(programDefinition.getBlockDefinitionByIndex(2).get().description())
        .isEqualTo("block 3 description");
  }

  @Test
  public void createProgramWithEmptyBlock() {
    ProgramDefinition program = ProgramBuilder.newProgram("name", "desc").buildDefinition();

    assertThat(program.getBlockDefinitionByIndex(0).get().name()).isEqualTo("");
    assertThat(program.getBlockDefinitionByIndex(0).get().description()).isEqualTo("");
  }

  @Test
  public void createProgramWithQuestions() throws Exception {
    QuestionDefinition nameQuestionDefinition =
        new QuestionDefinitionBuilder()
            .setId(1L)
            .setVersion(1L)
            .setName("my name")
            .setPath(Path.create("my.path.name"))
            .setDescription("description")
            .setQuestionType(QuestionType.NAME)
            .setQuestionText(ImmutableMap.of(Locale.US, "question?"))
            .setQuestionHelpText(ImmutableMap.of(Locale.US, "help text"))
            .setLifecycleStage(LifecycleStage.DRAFT)
            .build();
    ProgramDefinition programDefinition =
        ProgramBuilder.newProgram("name", "description")
            .withBlock()
            .withName("block 1")
            .withDescription("block 1 description")
            .withQuestionDefinition(nameQuestionDefinition)
            .buildDefinition();

    assertThat(programDefinition.id()).isGreaterThan(0);
    assertThat(programDefinition.blockDefinitions()).hasSize(1);
    BlockDefinition block = programDefinition.blockDefinitions().get(0);
    assertThat(block.programQuestionDefinitions()).hasSize(1);
    QuestionDefinition question = block.getQuestionDefinition(0);
    assertThat(question).isInstanceOf(NameQuestionDefinition.class);
  }

  @Test
  public void emptyProgram() {
    Program program = ProgramBuilder.newProgram().build();

    assertThat(program.id).isGreaterThan(0);
    assertThat(program.getProgramDefinition().name()).isEqualTo("");
    assertThat(program.getProgramDefinition().description()).isEqualTo("");
    assertThat(program.getProgramDefinition().getBlockCount()).isEqualTo(1);
  }
}
