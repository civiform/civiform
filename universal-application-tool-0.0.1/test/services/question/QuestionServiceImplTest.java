package services.question;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.google.common.collect.ImmutableMap;
import java.util.Locale;
import java.util.Optional;
import java.util.concurrent.CompletionStage;
import org.junit.Before;
import org.junit.Test;
import repository.WithPostgresContainer;
import services.ErrorAnd;
import services.Path;

public class QuestionServiceImplTest extends WithPostgresContainer {
  QuestionServiceImpl questionService;

  QuestionDefinition questionDefinition =
      new TextQuestionDefinition(
          1L,
          "my name",
          Path.create("my.path.name"),
          "description",
          ImmutableMap.of(Locale.ENGLISH, "question?"),
          ImmutableMap.of(Locale.ENGLISH, "help text"));

  @Before
  public void setProgramServiceImpl() {
    questionService = instanceOf(QuestionServiceImpl.class);
  }

  @Test
  public void addTranslation_notImplemented() {
    assertThatThrownBy(
            () ->
                questionService.addTranslation(
                    Path.create("your.name"), Locale.GERMAN, "Wie heisst du?", Optional.empty()))
        .isInstanceOf(UnsupportedOperationException.class)
        .hasMessage("Not supported yet.");
  }

  @Test
  public void create_failsWhenPathConflicts() {
    questionService.create(questionDefinition);

    ErrorAnd<QuestionDefinition, QuestionServiceError> errorAndResult =
        questionService.create(questionDefinition);

    assertThat(errorAndResult.hasResult()).isFalse();
    assertThat(errorAndResult.isError()).isTrue();
    assertThat(errorAndResult.getErrors())
        .containsOnly(
            QuestionServiceError.of(
                String.format(
                    "path '%s' conflicts with question: %s",
                    questionDefinition.getPath(), questionDefinition.getPath())));
  }

  @Test
  public void create_failsWithInvalidPathPattern() {
    QuestionDefinition question =
        new TextQuestionDefinition(
            1L,
            "name",
            Path.create("#invalid&path-pattern!"),
            "description",
            ImmutableMap.of(Locale.ENGLISH, "question?"),
            ImmutableMap.of());

    ErrorAnd<QuestionDefinition, QuestionServiceError> errorAndResult =
        questionService.create(question);

    assertThat(errorAndResult.hasResult()).isFalse();
    assertThat(errorAndResult.isError()).isTrue();
    assertThat(errorAndResult.getErrors())
        .containsOnly(
            QuestionServiceError.of(
                String.format("invalid path pattern: '%s'", question.getPath())));
  }

  @Test
  public void create_returnsQuestionDefinitionWhenSucceeds() {
    ErrorAnd<QuestionDefinition, QuestionServiceError> errorAndResult =
        questionService.create(questionDefinition);

    assertThat(errorAndResult.isError()).isFalse();
    assertThat(errorAndResult.hasResult()).isTrue();
    assertThat(errorAndResult.getResult().getPath()).isEqualTo(questionDefinition.getPath());
  }

  @Test
  public void getReadOnlyQuestionService() {
    questionService.create(questionDefinition);

    CompletionStage<ReadOnlyQuestionService> completionStage =
        questionService.getReadOnlyQuestionService();

    ReadOnlyQuestionService roService = completionStage.toCompletableFuture().join();

    assertThat(roService.getAllQuestions().size()).isEqualTo(1);
  }

  @Test
  public void getReadOnlyQuestionService_empty() {
    CompletionStage<ReadOnlyQuestionService> completionStage =
        questionService.getReadOnlyQuestionService();

    ReadOnlyQuestionService emptyService = completionStage.toCompletableFuture().join();

    assertThat(emptyService.getAllQuestions()).isEmpty();
    assertThat(emptyService.getAllScalars()).isEmpty();
  }

  @Test
  public void update_returnsQuestionDefinitionWhenSucceeds()
      throws InvalidUpdateException, UnsupportedQuestionTypeException {
    QuestionDefinition question = questionService.create(questionDefinition).getResult();
    QuestionDefinition toUpdate =
        new QuestionDefinitionBuilder(question).setName("updated name").build();

    assertThat(questionService.update(toUpdate).getName()).isEqualTo("updated name");
  }

  @Test
  public void update_failsWhenQuestionNotPersisted() {
    assertThatThrownBy(() -> questionService.update(questionDefinition))
        .isInstanceOf(InvalidUpdateException.class)
        .hasMessageContaining("question definition is not persisted");
  }

  @Test
  public void update_failsWhenQuestionNotExistent() throws UnsupportedQuestionTypeException {
    QuestionDefinition question =
        new QuestionDefinitionBuilder(questionDefinition).setId(9999L).build();
    assertThatThrownBy(() -> questionService.update(question))
        .isInstanceOf(InvalidUpdateException.class)
        .hasMessageContaining("question with id 9999 does not exist");
  }

  @Test
  public void update_failsWhenQuestionPathChanges() throws UnsupportedQuestionTypeException {
    QuestionDefinition question = questionService.create(questionDefinition).getResult();
    QuestionDefinition toUpdate =
        new QuestionDefinitionBuilder(question).setPath(Path.create("new.path")).build();

    assertThatThrownBy(() -> questionService.update(toUpdate))
        .isInstanceOf(InvalidUpdateException.class)
        .hasMessageContaining("question paths mismatch");
  }

  @Test
  public void update_failsWhenQuestionTypeChanges() throws UnsupportedQuestionTypeException {
    QuestionDefinition question = questionService.create(questionDefinition).getResult();
    QuestionDefinition toUpdate =
        new QuestionDefinitionBuilder(question).setQuestionType(QuestionType.ADDRESS).build();

    assertThatThrownBy(() -> questionService.update(toUpdate))
        .isInstanceOf(InvalidUpdateException.class)
        .hasMessageContaining("question types mismatch");
  }
}
