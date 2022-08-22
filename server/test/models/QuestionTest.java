package models;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;

import com.google.common.collect.ImmutableList;
import java.util.Locale;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import org.junit.Before;
import org.junit.Test;
import repository.QuestionRepository;
import repository.ResetPostgres;
import services.LocalizedStrings;
import services.question.QuestionOption;
import services.question.exceptions.UnsupportedQuestionTypeException;
import services.question.types.AddressQuestionDefinition;
import services.question.types.EnumeratorQuestionDefinition;
import services.question.types.MultiOptionQuestionDefinition;
import services.question.types.QuestionDefinition;
import services.question.types.QuestionDefinitionBuilder;
import services.question.types.QuestionType;
import services.question.types.TextQuestionDefinition;
import services.question.types.TextQuestionDefinition.TextValidationPredicates;

public class QuestionTest extends ResetPostgres {

  private QuestionRepository repo;

  @Before
  public void setupQuestionRepository() {
    repo = instanceOf(QuestionRepository.class);
  }

  @Test
  public void canSaveQuestion() throws UnsupportedQuestionTypeException {
    QuestionDefinition definition =
        new TextQuestionDefinition(
            "test", Optional.empty(), "", LocalizedStrings.of(), LocalizedStrings.empty());
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
            "test", Optional.empty(), "", LocalizedStrings.of(), LocalizedStrings.empty());
    Question question = new Question(questionDefinition);
    question.save();

    Question found = repo.lookupQuestion(question.id).toCompletableFuture().join().get();

    assertThat(found.getQuestionDefinition().getEnumeratorId()).isEmpty();
  }

  @Test
  public void canSerializeEnumeratorId_NonEmptyOptionalLong() {
    QuestionDefinition questionDefinition =
        new TextQuestionDefinition(
            "test", Optional.of(10L), "", LocalizedStrings.of(), LocalizedStrings.empty());
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
            Optional.empty(),
            "",
            LocalizedStrings.of(Locale.US, "hello"),
            LocalizedStrings.of(Locale.US, "help"));
    Question question = new Question(definition);

    question.save();

    Question found = repo.lookupQuestion(question.id).toCompletableFuture().join().get();

    assertThat(found.getQuestionDefinition().getQuestionText())
        .isEqualTo(LocalizedStrings.of(Locale.US, "hello"));
    assertThat(found.getQuestionDefinition().getQuestionHelpText())
        .isEqualTo(LocalizedStrings.of(Locale.US, "help"));
  }

  @Test
  public void canSerializeDifferentQuestionTypes() {
    AddressQuestionDefinition address =
        new AddressQuestionDefinition(
            "address", Optional.empty(), "", LocalizedStrings.of(), LocalizedStrings.empty());
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
            Optional.empty(),
            "",
            LocalizedStrings.of(),
            LocalizedStrings.empty(),
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
            .setEnumeratorId(Optional.of(123L))
            .setQuestionText(LocalizedStrings.of())
            .setQuestionHelpText(LocalizedStrings.empty())
            .setQuestionOptions(
                ImmutableList.of(
                    QuestionOption.create(1L, LocalizedStrings.of(Locale.US, "option"))))
            .build();
    Question question = new Question(definition);

    question.save();

    Question found = repo.lookupQuestion(question.id).toCompletableFuture().join().get();

    assertThat(found.getQuestionDefinition().getQuestionType().isMultiOptionType()).isTrue();
    MultiOptionQuestionDefinition multiOption =
        (MultiOptionQuestionDefinition) found.getQuestionDefinition();

    assertThat(multiOption.getOptions())
        .isEqualTo(
            ImmutableList.of(QuestionOption.create(1L, LocalizedStrings.of(Locale.US, "option"))));
    assertThat(multiOption.getEnumeratorId()).hasValue(123L);
  }

  @Test
  public void canSerializeAndDeserializeEnumeratorQuestion()
      throws UnsupportedQuestionTypeException {
    LocalizedStrings entityType = LocalizedStrings.of(Locale.US, "entity");

    QuestionDefinition definition =
        new QuestionDefinitionBuilder()
            .setQuestionType(QuestionType.ENUMERATOR)
            .setName("")
            .setDescription("")
            .setEnumeratorId(Optional.of(123L))
            .setQuestionText(LocalizedStrings.of())
            .setQuestionHelpText(LocalizedStrings.empty())
            .setEntityType(entityType)
            .build();
    Question question = new Question(definition);

    question.save();

    Question found = repo.lookupQuestion(question.id).toCompletableFuture().join().get();

    assertThat(found.getQuestionDefinition().getQuestionType()).isEqualTo(QuestionType.ENUMERATOR);
    EnumeratorQuestionDefinition enumerator =
        (EnumeratorQuestionDefinition) found.getQuestionDefinition();

    assertThat(enumerator.getEntityType()).isEqualTo(entityType);
  }

  @Test
  public void testTimestamps() throws Exception {
    QuestionDefinition questionDefinition =
        new TextQuestionDefinition(
            "test", Optional.of(10L), "", LocalizedStrings.of(), LocalizedStrings.empty());
    Question initialQuestion = new Question(questionDefinition);
    initialQuestion.save();

    assertThat(initialQuestion.getCreateTime()).isNotEmpty();
    assertThat(initialQuestion.getLastModifiedTime()).isNotEmpty();

    // Ensure a freshly loaded copy has the same timestamps.
    Question freshlyLoaded =
        repo.lookupQuestion(initialQuestion.id).toCompletableFuture().join().get();
    assertThat(freshlyLoaded.getCreateTime()).isEqualTo(initialQuestion.getCreateTime());
    assertThat(freshlyLoaded.getLastModifiedTime())
        .isEqualTo(initialQuestion.getLastModifiedTime());

    // Update the copy.
    // When persisting models with @WhenModified fields, EBean
    // truncates the persisted timestamp to milliseconds:
    // https://github.com/seattle-uat/civiform/pull/2499#issuecomment-1133325484.
    // Sleep for a few milliseconds to ensure that a subsequent
    // update would have a distinct timestamp.
    TimeUnit.MILLISECONDS.sleep(5);
    freshlyLoaded.markAsDirty();
    freshlyLoaded.save();

    Question afterUpdate =
        repo.lookupQuestion(initialQuestion.id).toCompletableFuture().join().get();
    assertThat(afterUpdate.getCreateTime()).isEqualTo(initialQuestion.getCreateTime());
    assertThat(afterUpdate.getLastModifiedTime()).isPresent();
    assertThat(afterUpdate.getLastModifiedTime().get())
        .isAfter(initialQuestion.getLastModifiedTime().get());
  }
}
