package support;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.google.common.collect.ImmutableMap;
import java.util.Locale;
import org.junit.Test;
import repository.WithPostgresContainer;
import services.Path;
import services.program.BlockDefinition;
import services.program.ProgramDefinition;
import services.program.ProgramNeedsABlockException;
import services.question.NameQuestionDefinition;
import services.question.QuestionDefinition;
import services.question.QuestionDefinitionBuilder;
import services.question.QuestionType;

public class ProgramBuilderTest extends WithPostgresContainer {
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
            .build();

    assertThat(programDefinition.id()).isGreaterThan(0);
    assertThat(programDefinition.name()).isEqualTo("a new name");
    assertThat(programDefinition.description()).isEqualTo("a new description");

    assertThat(programDefinition.blockDefinitions()).hasSize(3);
    assertThat(programDefinition.getBlockDefinition(0).get().id()).isEqualTo(1L);
    assertThat(programDefinition.getBlockDefinition(0).get().name()).isEqualTo("block 1");
    assertThat(programDefinition.getBlockDefinition(0).get().description())
        .isEqualTo("block 1 description");
    assertThat(programDefinition.getBlockDefinition(1).get().id()).isEqualTo(2L);
    assertThat(programDefinition.getBlockDefinition(1).get().name()).isEqualTo("block 2");
    assertThat(programDefinition.getBlockDefinition(1).get().description())
        .isEqualTo("block 2 description");
    assertThat(programDefinition.getBlockDefinition(2).get().id()).isEqualTo(3L);
    assertThat(programDefinition.getBlockDefinition(2).get().name()).isEqualTo("block 3");
    assertThat(programDefinition.getBlockDefinition(2).get().description())
        .isEqualTo("block 3 description");
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
            .setQuestionText(ImmutableMap.of(Locale.ENGLISH, "question?"))
            .setQuestionHelpText(ImmutableMap.of(Locale.ENGLISH, "help text"))
            .build();
    ProgramDefinition programDefinition =
        ProgramBuilder.newProgram("name", "description")
            .withBlock()
            .withName("block 1")
            .withDescription("block 1 description")
            .withQuestion(nameQuestionDefinition)
            .build();

    assertThat(programDefinition.id()).isGreaterThan(0);
    assertThat(programDefinition.blockDefinitions()).hasSize(1);
    BlockDefinition block = programDefinition.blockDefinitions().get(0);
    assertThat(block.programQuestionDefinitions()).hasSize(1);
    QuestionDefinition question = block.getQuestionDefinition(0);
    assertThat(question).isInstanceOf(NameQuestionDefinition.class);
  }

  @Test
  public void program_withNoBlocks_throws() {
    assertThatThrownBy(() -> ProgramBuilder.newProgram("name", "description").build())
        .isInstanceOf(ProgramNeedsABlockException.class);
  }
}
