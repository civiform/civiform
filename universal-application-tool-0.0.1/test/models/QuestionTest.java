package models;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;

import com.google.common.collect.ImmutableListMultimap;
import com.google.common.collect.ImmutableMap;
import java.util.Locale;
import org.junit.Before;
import org.junit.Test;
import repository.QuestionRepository;
import repository.WithPostgresContainer;
import services.Path;
import services.question.AddressQuestionDefinition;
import services.question.MultiOptionQuestionDefinition;
import services.question.QuestionDefinition;
import services.question.QuestionDefinitionBuilder;
import services.question.QuestionType;
import services.question.TextQuestionDefinition;
import services.question.TextQuestionDefinition.TextValidationPredicates;
import services.question.UnsupportedQuestionTypeException;

public class QuestionTest extends WithPostgresContainer {

  private QuestionRepository repo;

  @Before
  public void setupQuestionRepository() {
    repo = instanceOf(QuestionRepository.class);
  }

  @Test
  public void canSaveQuestion() throws UnsupportedQuestionTypeException {
    QuestionDefinition definition =
        new TextQuestionDefinition(
            1L,
            "test",
            Path.create("my.path"),
            "",
            LifecycleStage.ACTIVE,
            ImmutableMap.of(),
            ImmutableMap.of());
    Question question = new Question(definition);

    question.save();

    Question found = repo.lookupQuestion(question.id).toCompletableFuture().join().get();

    QuestionDefinition expected =
        new QuestionDefinitionBuilder(definition).setId(question.id).build();
    assertEquals(expected, found.getQuestionDefinition());
  }

  @Test
  public void canSerializeLocalizationMaps() {
    QuestionDefinition definition =
        new TextQuestionDefinition(
            1L,
            "",
            Path.empty(),
            "",
            LifecycleStage.ACTIVE,
            ImmutableMap.of(Locale.US, "hello"),
            ImmutableMap.of(Locale.US, "help"));
    Question question = new Question(definition);

    question.save();

    Question found = repo.lookupQuestion(question.id).toCompletableFuture().join().get();

    assertThat(found.getQuestionDefinition().getQuestionText())
        .isEqualTo(ImmutableMap.of(Locale.US, "hello"));
    assertThat(found.getQuestionDefinition().getQuestionHelpText())
        .isEqualTo(ImmutableMap.of(Locale.US, "help"));
  }

  @Test
  public void canSerializeDifferentQuestionTypes() {
    AddressQuestionDefinition address =
        new AddressQuestionDefinition(
            1L,
            "address",
            Path.empty(),
            "",
            LifecycleStage.ACTIVE,
            ImmutableMap.of(),
            ImmutableMap.of());
    Question question = new Question(address);

    question.save();

    Question found = repo.lookupQuestion(question.id).toCompletableFuture().join().get();

    assertThat(found.getQuestionDefinition()).isInstanceOf(AddressQuestionDefinition.class);
  }

  @Test
  public void canSerializeValidationPredicates() {
    QuestionDefinition definition =
        new TextQuestionDefinition(
            1L,
            "",
            Path.empty(),
            "",
            LifecycleStage.ACTIVE,
            ImmutableMap.of(),
            ImmutableMap.of(),
            TextValidationPredicates.create(0, 128));
    Question question = new Question(definition);

    question.save();

    Question found = repo.lookupQuestion(question.id).toCompletableFuture().join().get();

    assertThat(found.getQuestionDefinition().getValidationPredicates())
        .isEqualTo(TextValidationPredicates.create(0, 128));
  }

  @Test
  public void canSerializeAndDeserializeMultiOptionQuestion()
      throws UnsupportedQuestionTypeException {
    QuestionDefinition definition =
        new QuestionDefinitionBuilder()
            .setQuestionType(QuestionType.DROPDOWN)
            .setName("")
            .setDescription("")
            .setPath(Path.empty())
            .setQuestionText(ImmutableMap.of())
            .setLifecycleStage(LifecycleStage.ACTIVE)
            .setQuestionHelpText(ImmutableMap.of())
            .setQuestionOptions(ImmutableListMultimap.of(Locale.US, "option"))
            .build();
    Question question = new Question(definition);

    question.save();

    Question found = repo.lookupQuestion(question.id).toCompletableFuture().join().get();

    assertThat(found.getQuestionDefinition().getQuestionType().isMultiOptionType()).isTrue();
    MultiOptionQuestionDefinition multiOption =
        (MultiOptionQuestionDefinition) found.getQuestionDefinition();
    assertThat(multiOption.getOptions()).isEqualTo(ImmutableListMultimap.of(Locale.US, "option"));
  }
}
