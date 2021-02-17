package models;

import static org.assertj.core.api.Assertions.assertThat;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import java.util.Locale;
import java.util.Optional;
import org.junit.Test;
import repository.ProgramRepository;
import repository.WithTruncatingTables;
import services.program.BlockDefinition;
import services.program.ProgramDefinition;
import services.question.QuestionDefinition;

public class ProgramTest extends WithTruncatingTables {

  @Test
  public void canSaveProgram() {
    ProgramRepository repo = app.injector().instanceOf(ProgramRepository.class);

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
            .setId(100L)
            .setName("name")
            .setDescription("desc")
            .setBlockDefinitions(ImmutableList.of(blockDefinition))
            .build();
    Program program = new Program(definition);

    program.save();

    Program found = repo.lookupProgram(100L).toCompletableFuture().join().get();

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
