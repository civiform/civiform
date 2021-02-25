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

public class QuestionServiceImplTest extends WithPostgresContainer {
  QuestionServiceImpl questionService;

  QuestionDefinition questionDefinition =
      new TextQuestionDefinition(
          1L,
          "my name",
          "my.path.name",
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
                    "your.name", Locale.GERMAN, "Wie heisst du?", Optional.empty()))
        .isInstanceOf(UnsupportedOperationException.class)
        .hasMessage("Not supported yet.");
  }

  @Test
  public void create_returnsOptionalEmptyWhenFails() {
    questionService.create(questionDefinition);

    assertThat(questionService.create(questionDefinition).isPresent()).isFalse();
  }

  @Test
  public void create_failsWithInvalidPathPattern() {
    QuestionDefinition question =
        new TextQuestionDefinition(
            1L,
            "name",
            "#invalid&path-pattern!",
            "description",
            ImmutableMap.of(Locale.ENGLISH, "question?"),
            Optional.empty());

    assertThat(questionService.create(question).isPresent()).isFalse();
  }

  @Test
  public void create_returnsQuestionDefinitionWhenSucceeds() {
    assertThat(questionService.create(questionDefinition).get().getPath())
        .isEqualTo(questionDefinition.getPath());
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
  public void update_notImplemented() {
    assertThatThrownBy(() -> questionService.update(null))
        .isInstanceOf(UnsupportedOperationException.class)
        .hasMessage("Not supported yet.");
  }
}
