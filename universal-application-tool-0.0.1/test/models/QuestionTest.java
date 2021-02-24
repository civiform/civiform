package models;

import static org.assertj.core.api.Assertions.assertThat;

import com.google.common.collect.ImmutableMap;
import java.util.Locale;
import java.util.Optional;
import java.util.OptionalLong;
import org.junit.Before;
import org.junit.Test;
import repository.QuestionRepository;
import repository.WithPostgresContainer;
import services.question.AddressQuestionDefinition;
import services.question.QuestionDefinition;
import services.question.TextQuestionDefinition;

public class QuestionTest extends WithPostgresContainer {

  private QuestionRepository repo;

  @Before
  public void setupQuestionRepository() {
    repo = instanceOf(QuestionRepository.class);
  }

  @Test
  public void canSaveQuestion() {
    QuestionDefinition definition =
        new TextQuestionDefinition(
            OptionalLong.empty(), 1L, "test", "my.path", "", ImmutableMap.of(), Optional.empty());
    Question question = new Question(definition);

    question.save();

    Question found = repo.lookupQuestion(question.id).toCompletableFuture().join().get();

    assertThat(found.getQuestionDefinition().getId().getAsLong()).isEqualTo(question.id);
    assertThat(found.getQuestionDefinition().getVersion()).isEqualTo(1L);
    assertThat(found.getQuestionDefinition().getName()).isEqualTo("test");
    assertThat(found.getQuestionDefinition().getPath()).isEqualTo("my.path");
    assertThat(found.getQuestionDefinition().getDescription()).isEqualTo("");
    assertThat(found.getQuestionDefinition().getQuestionText()).isEqualTo(ImmutableMap.of());
    assertThat(found.getQuestionDefinition().getQuestionHelpText()).isEqualTo(Optional.empty());
  }

  @Test
  public void canSerializeLocalizationMaps() {
    QuestionDefinition definition =
        new TextQuestionDefinition(
            OptionalLong.empty(),
            1L,
            "",
            "",
            "",
            ImmutableMap.of(Locale.ENGLISH, "hello"),
            Optional.of(ImmutableMap.of(Locale.ENGLISH, "help")));
    Question question = new Question(definition);

    question.save();

    Question found = repo.lookupQuestion(question.id).toCompletableFuture().join().get();

    assertThat(found.getQuestionDefinition().getQuestionText())
        .isEqualTo(ImmutableMap.of(Locale.ENGLISH, "hello"));
    assertThat(found.getQuestionDefinition().getQuestionHelpText())
        .hasValue(ImmutableMap.of(Locale.ENGLISH, "help"));
  }

  @Test
  public void canSerializeDifferentQuestionTypes() {
    AddressQuestionDefinition address =
        new AddressQuestionDefinition(
            OptionalLong.empty(), 1L, "address", "", "", ImmutableMap.of(), Optional.empty());
    Question question = new Question(address);

    question.save();

    Question found = repo.lookupQuestion(question.id).toCompletableFuture().join().get();

    assertThat(found.getQuestionDefinition()).isInstanceOf(AddressQuestionDefinition.class);
  }
}
