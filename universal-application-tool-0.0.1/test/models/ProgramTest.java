package models;

import static org.assertj.core.api.Assertions.assertThat;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import java.util.Locale;
import java.util.Optional;
import org.junit.Test;
import repository.ProgramRepository;
import repository.WithPostgresContainer;
import services.program.BlockDefinition;
import services.program.ProgramDefinition;
import services.question.QuestionDefinition;

public class ProgramTest extends WithPostgresContainer {

  @Test
  public void canSaveProgram() {
    ProgramRepository repo = app.injector().instanceOf(ProgramRepository.class);
    Program program = new Program();

    QuestionDefinition questionDefinition =
        new QuestionDefinition(
            165L,
            "2",
            "question",
            "applicant.name",
            "applicant's name",
            ImmutableMap.of(Locale.US, "What is your name?"),
            Optional.empty());

    BlockDefinition blockDefinition =
        BlockDefinition.builder()
            .setId(1L)
            .setName("First Block")
            .setDescription("basic info")
            .setQuestionDefinitions(ImmutableList.of(questionDefinition))
            .build();

    ProgramDefinition definition =
        ProgramDefinition.builder()
            .setId(100l)
            .setName("name")
            .setDescription("desc")
            .setBlockDefinitions(ImmutableList.of(blockDefinition))
            .build();
    program.setProgramDefinition(definition);

    program.save();

    Program found = repo.lookupProgram("name").toCompletableFuture().join().get();

    assertThat(found.name).isEqualTo("name");
    assertThat(found.description).isEqualTo("desc");
    assertThat(found.id).isEqualTo(100L);

    assertThat(found.getProgramDefinition().name()).isEqualTo("name");
    assertThat(found.getProgramDefinition().blockDefinitions().get(0).name())
        .isEqualTo("First Block");

    assertThat(
            found
                .getProgramDefinition()
                .blockDefinitions()
                .get(0)
                .questionDefinitions()
                .get(0)
                .getQuestionText(Locale.US))
        .isEqualTo("What is your name?");
  }
}
