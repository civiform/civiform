package models;

import static org.assertj.core.api.Assertions.assertThat;

import com.google.common.collect.ImmutableMap;
import java.util.Locale;
import java.util.Optional;
import org.junit.Test;
import repository.QuestionRepository;
import repository.WithPostgresContainer;
import services.question.QuestionDefinition;

public class QuestionTest extends WithPostgresContainer {

  @Test
  public void canSaveQuestion() {
    QuestionRepository repo = app.injector().instanceOf(QuestionRepository.class);
    QuestionDefinition definition =
        new QuestionDefinition(1L, 1L, "test", "my.path", "", ImmutableMap.of(), Optional.empty());
    Question question = new Question(definition);

    question.save();

    Question found = repo.lookupQuestion(definition.getId()).toCompletableFuture().join().get();

    assertThat(found.getQuestionDefinition().getId()).isEqualTo(1L);
    assertThat(found.getQuestionDefinition().getVersion()).isEqualTo(1L);
    assertThat(found.getQuestionDefinition().getName()).isEqualTo("test");
    assertThat(found.getQuestionDefinition().getPath()).isEqualTo("my.path");
    assertThat(found.getQuestionDefinition().getDescription()).isEqualTo("");
    assertThat(found.getQuestionDefinition().getQuestionText()).isEqualTo(ImmutableMap.of());
    assertThat(found.getQuestionDefinition().getQuestionHelpText()).isEqualTo(Optional.empty());
  }

  @Test
  public void canSerializeLocalizationMaps() {
    QuestionRepository repo = app.injector().instanceOf(QuestionRepository.class);
    QuestionDefinition definition =
        new QuestionDefinition(
            1L,
            1L,
            "",
            "",
            "",
            ImmutableMap.of(Locale.ENGLISH, "hello"),
            Optional.of(ImmutableMap.of(Locale.ENGLISH, "help")));
    Question question = new Question(definition);

    question.save();

    Question found = repo.lookupQuestion(definition.getId()).toCompletableFuture().join().get();

    assertThat(found.getQuestionDefinition().getQuestionText())
        .isEqualTo(ImmutableMap.of(Locale.ENGLISH, "hello"));
    assertThat(found.getQuestionDefinition().getQuestionHelpText())
        .hasValue(ImmutableMap.of(Locale.ENGLISH, "help"));
  }
}
