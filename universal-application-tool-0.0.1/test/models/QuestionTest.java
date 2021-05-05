package models;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import java.util.Locale;
import java.util.Optional;
import org.junit.Before;
import org.junit.Test;
import repository.QuestionRepository;
import repository.WithPostgresContainer;
import services.Path;
import services.question.QuestionOption;
import services.question.exceptions.UnsupportedQuestionTypeException;
import services.question.types.AddressQuestionDefinition;
import services.question.types.MultiOptionQuestionDefinition;
import services.question.types.QuestionDefinition;
import services.question.types.QuestionDefinitionBuilder;
import services.question.types.QuestionType;
import services.question.types.TextQuestionDefinition;
import services.question.types.TextQuestionDefinition.TextValidationPredicates;

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
            "test",
            Path.create("my.path"),
            Optional.empty(),
            "",
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
  public void canSerializeEnumeratorId_EmptyOptionalLong() {
    QuestionDefinition questionDefinition =
        new TextQuestionDefinition(
            "test",
            Path.create("my.path"),
            Optional.empty(),
            "",
            ImmutableMap.of(),
            ImmutableMap.of());
    Question question = new Question(questionDefinition);
    question.save();

    Question found = repo.lookupQuestion(question.id).toCompletableFuture().join().get();

    assertThat(found.getQuestionDefinition().getEnumeratorId()).isEmpty();
  }

  @Test
  public void canSerializeEnumeratorId_NonEmptyOptionalLong() {
    QuestionDefinition questionDefinition =
        new TextQuestionDefinition(
            "test",
            Path.create("my.path"),
            Optional.of(10L),
            "",
            ImmutableMap.of(),
            ImmutableMap.of());
    Question question = new Question(questionDefinition);
    question.save();

    Question found = repo.lookupQuestion(question.id).toCompletableFuture().join().get();

    assertThat(found.getQuestionDefinition().getEnumeratorId()).hasValue(10L);
  }

  @Test
  public void canSerializeLocalizationMaps() {
    QuestionDefinition definition =
        new TextQuestionDefinition(
            "",
            Path.empty(),
            Optional.empty(),
            "",
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
            "address", Path.empty(), Optional.empty(), "", ImmutableMap.of(), ImmutableMap.of());
    Question question = new Question(address);

    question.save();

    Question found = repo.lookupQuestion(question.id).toCompletableFuture().join().get();

    assertThat(found.getQuestionDefinition()).isInstanceOf(AddressQuestionDefinition.class);
  }

  @Test
  public void canSerializeValidationPredicates() {
    QuestionDefinition definition =
        new TextQuestionDefinition(
            "",
            Path.empty(),
            Optional.empty(),
            "",
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
            .setEnumeratorId(Optional.of(123L))
            .setQuestionText(ImmutableMap.of())
            .setQuestionHelpText(ImmutableMap.of())
            .setQuestionOptions(
                ImmutableList.of(QuestionOption.create(1L, ImmutableMap.of(Locale.US, "option"))))
            .build();
    Question question = new Question(definition);

    question.save();

    Question found = repo.lookupQuestion(question.id).toCompletableFuture().join().get();

    assertThat(found.getQuestionDefinition().getQuestionType().isMultiOptionType()).isTrue();
    MultiOptionQuestionDefinition multiOption =
        (MultiOptionQuestionDefinition) found.getQuestionDefinition();

    assertThat(multiOption.getOptions())
        .isEqualTo(
            ImmutableList.of(QuestionOption.create(1L, ImmutableMap.of(Locale.US, "option"))));
    assertThat(multiOption.getEnumeratorId()).hasValue(123L);
  }
}
