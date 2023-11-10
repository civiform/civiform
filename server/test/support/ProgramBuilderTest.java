package support;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Locale;
import models.ProgramModel;
import org.junit.Test;
import repository.ResetPostgres;
import services.LocalizedStrings;
import services.program.BlockDefinition;
import services.program.ProgramDefinition;
import services.question.types.NameQuestionDefinition;
import services.question.types.QuestionDefinition;
import services.question.types.QuestionDefinitionBuilder;
import services.question.types.QuestionType;

public class ProgramBuilderTest extends ResetPostgres {
  @Test
  public void fluentlyCreateProgramWithBlocks() {
    ProgramDefinition programDefinition =
        ProgramBuilder.newDraftProgram("name", "description")
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
    assertThat(programDefinition.adminName()).isEqualTo("a new name");
    assertThat(programDefinition.adminDescription()).isEqualTo("a new description");
    assertThat(programDefinition.localizedName()).isEqualTo(LocalizedStrings.of(Locale.US, "name"));
    assertThat(programDefinition.localizedDescription())
        .isEqualTo(LocalizedStrings.of(Locale.US, "description"));

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
    ProgramDefinition program = ProgramBuilder.newDraftProgram("name", "desc").buildDefinition();

    assertThat(program.getBlockDefinitionByIndex(0).get().name()).isEqualTo("");
    assertThat(program.getBlockDefinitionByIndex(0).get().description()).isEqualTo("");
  }

  @Test
  public void createProgramWithQuestions() throws Exception {
    QuestionDefinition nameQuestionDefinition =
        new QuestionDefinitionBuilder()
            .setId(1L)
            .setName("my name")
            .setDescription("description")
            .setQuestionType(QuestionType.NAME)
            .setQuestionText(LocalizedStrings.of(Locale.US, "question?"))
            .setQuestionHelpText(LocalizedStrings.of(Locale.US, "help text"))
            .build();
    ProgramDefinition programDefinition =
        ProgramBuilder.newDraftProgram("name", "description")
            .withBlock()
            .withName("block 1")
            .withDescription("block 1 description")
            .withRequiredQuestionDefinition(nameQuestionDefinition)
            .buildDefinition();

    assertThat(programDefinition.id()).isGreaterThan(0);
    assertThat(programDefinition.blockDefinitions()).hasSize(1);
    BlockDefinition block = programDefinition.blockDefinitions().get(0);
    assertThat(block.programQuestionDefinitions()).hasSize(1);
    QuestionDefinition question = block.getQuestionDefinition(0);
    assertThat(question).isInstanceOf(NameQuestionDefinition.class);
  }

  @Test
  public void emptyProgram() throws Exception {
    ProgramModel program = ProgramBuilder.newDraftProgram().build();

    assertThat(program.id).isGreaterThan(0);
    assertThat(program.getProgramDefinition().adminName()).isEmpty();
    assertThat(program.getProgramDefinition().localizedName().get(Locale.US)).isEmpty();
    assertThat(program.getProgramDefinition().localizedDescription().get(Locale.US)).isEmpty();
    assertThat(program.getProgramDefinition().getBlockCount()).isEqualTo(1);
  }
}
