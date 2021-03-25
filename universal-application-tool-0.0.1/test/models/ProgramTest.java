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
import services.question.AddressQuestionDefinition;
import services.question.NameQuestionDefinition;
import services.question.QuestionDefinition;
import services.question.QuestionDefinitionBuilder;
import services.question.QuestionType;
import services.question.UnsupportedQuestionTypeException;

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
            .setName("ProgramTest")
            .setDescription("desc")
            .setLifecycleStage(LifecycleStage.ACTIVE)
            .setBlockDefinitions(ImmutableList.of(blockDefinition))
            .build();
    Program program = new Program(definition);

    program.save();

    Program found = repo.lookupProgram(program.id).toCompletableFuture().join().get();

    assertThat(found.getProgramDefinition().name()).isEqualTo("ProgramTest");
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
            .setName("ProgramTest")
            .setDescription("desc")
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
