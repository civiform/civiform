package models;

import static org.assertj.core.api.Assertions.assertThat;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import java.util.Locale;
import org.junit.Before;
import org.junit.Test;
import repository.ProgramRepository;
import repository.WithPostgresContainer;
import services.Path;
import services.program.BlockDefinition;
import services.program.ProgramDefinition;
import services.program.ProgramQuestionDefinition;
import services.question.exceptions.UnsupportedQuestionTypeException;
import services.question.types.AddressQuestionDefinition;
import services.question.types.NameQuestionDefinition;
import services.question.types.QuestionDefinition;
import services.question.types.QuestionDefinitionBuilder;
import services.question.types.QuestionType;

public class ProgramTest extends WithPostgresContainer {

  private ProgramRepository repo;

  @Before
  public void setupProgramRepository() {
    repo = instanceOf(ProgramRepository.class);
  }

  @Test
  public void canSaveProgram() throws UnsupportedQuestionTypeException {
    QuestionDefinition questionDefinition =
        new QuestionDefinitionBuilder()
            .setQuestionType(QuestionType.TEXT)
            .setId(123L)
            .setVersion(2L)
            .setName("question")
            .setLifecycleStage(LifecycleStage.ACTIVE)
            .setPath(Path.create("applicant.name"))
            .setDescription("applicant's name")
            .setQuestionText(ImmutableMap.of(Locale.US, "What is your name?"))
            .build();

    BlockDefinition blockDefinition =
        BlockDefinition.builder()
            .setId(1L)
            .setName("First Block")
            .setDescription("basic info")
            .setProgramQuestionDefinitions(
                ImmutableList.of(ProgramQuestionDefinition.create(questionDefinition)))
            .build();

    ProgramDefinition definition =
        ProgramDefinition.builder()
            .setId(1L)
            .setAdminName("Admin name")
            .setAdminDescription("Admin description")
            .addLocalizedName(Locale.US, "ProgramTest")
            .addLocalizedDescription(Locale.US, "desc")
            .setLifecycleStage(LifecycleStage.ACTIVE)
            .setBlockDefinitions(ImmutableList.of(blockDefinition))
            .build();
    Program program = new Program(definition);

    program.save();

    Program found = repo.lookupProgram(program.id).toCompletableFuture().join().get();

    assertThat(found.getProgramDefinition().adminName()).isEqualTo("Admin name");
    assertThat(found.getProgramDefinition().localizedName())
        .isEqualTo(ImmutableMap.of(Locale.US, "ProgramTest"));
    assertThat(found.getProgramDefinition().blockDefinitions().get(0).name())
        .isEqualTo("First Block");

    assertThat(
            found
                .getProgramDefinition()
                .blockDefinitions()
                .get(0)
                .programQuestionDefinitions()
                .get(0)
                .id())
        .isEqualTo(questionDefinition.getId());
  }

  @Test
  public void correctlySerializesDifferentQuestionTypes() throws UnsupportedQuestionTypeException {
    AddressQuestionDefinition addressQuestionDefinition =
        (AddressQuestionDefinition)
            new QuestionDefinitionBuilder()
                .setQuestionType(QuestionType.ADDRESS)
                .setId(456L)
                .setVersion(2L)
                .setName("address question")
                .setLifecycleStage(LifecycleStage.ACTIVE)
                .setPath(Path.create("applicant.address"))
                .setDescription("applicant's address")
                .setQuestionText(ImmutableMap.of(Locale.US, "What is your address?"))
                .build();
    NameQuestionDefinition nameQuestionDefinition =
        (NameQuestionDefinition)
            new QuestionDefinitionBuilder()
                .setQuestionType(QuestionType.NAME)
                .setId(789L)
                .setVersion(2L)
                .setName("name question")
                .setLifecycleStage(LifecycleStage.ACTIVE)
                .setPath(Path.create("applicant.name"))
                .setDescription("applicant's name")
                .setQuestionText(ImmutableMap.of(Locale.US, "What is your name?"))
                .build();

    BlockDefinition blockDefinition =
        BlockDefinition.builder()
            .setId(1L)
            .setName("First Block")
            .setDescription("basic info")
            .setProgramQuestionDefinitions(
                ImmutableList.of(
                    ProgramQuestionDefinition.create(addressQuestionDefinition),
                    ProgramQuestionDefinition.create(nameQuestionDefinition)))
            .build();

    ProgramDefinition definition =
        ProgramDefinition.builder()
            .setId(1L)
            .setAdminName("Admin name")
            .setAdminDescription("Admin description")
            .addLocalizedName(Locale.US, "ProgramTest")
            .addLocalizedDescription(Locale.US, "desc")
            .setLifecycleStage(LifecycleStage.ACTIVE)
            .setBlockDefinitions(ImmutableList.of(blockDefinition))
            .build();
    Program program = new Program(definition);
    program.save();

    Program found = repo.lookupProgram(program.id).toCompletableFuture().join().get();

    ProgramQuestionDefinition addressQuestion =
        found.getProgramDefinition().blockDefinitions().get(0).programQuestionDefinitions().get(0);
    assertThat(addressQuestion.id()).isEqualTo(addressQuestionDefinition.getId());
    ProgramQuestionDefinition nameQuestion =
        found.getProgramDefinition().blockDefinitions().get(0).programQuestionDefinitions().get(1);
    assertThat(nameQuestion.id()).isEqualTo(nameQuestionDefinition.getId());
  }
}
