package services.question;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.google.common.collect.ImmutableMap;
import java.util.Locale;
import java.util.Optional;
import java.util.concurrent.CompletionStage;
import models.LifecycleStage;
import models.Question;
import org.junit.Before;
import org.junit.Test;
import repository.WithPostgresContainer;
import services.CiviFormError;
import services.ErrorAnd;
import services.Path;
import services.question.exceptions.InvalidUpdateException;
import services.question.exceptions.UnsupportedQuestionTypeException;
import services.question.types.QuestionDefinition;
import services.question.types.QuestionDefinitionBuilder;
import services.question.types.QuestionType;
import services.question.types.TextQuestionDefinition;

public class QuestionServiceImplTest extends WithPostgresContainer {
  QuestionServiceImpl questionService;

  QuestionDefinition questionDefinition =
      new TextQuestionDefinition(
          1L,
          "my name",
          Path.create("my.path.name"),
          Optional.empty(),
          "description",
          LifecycleStage.ACTIVE,
          ImmutableMap.of(Locale.US, "question?"),
          ImmutableMap.of(Locale.US, "help text"));

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
  public void create_failsWhenPathConflicts() throws Exception {
    Question applicantName = testQuestionBank.applicantName();
    QuestionDefinition questionDefinition =
        new QuestionDefinitionBuilder(applicantName.getQuestionDefinition()).clearId().build();

    ErrorAnd<QuestionDefinition, CiviFormError> errorAndResult =
        questionService.create(questionDefinition);

    assertThat(errorAndResult.hasResult()).isFalse();
    assertThat(errorAndResult.isError()).isTrue();
    assertThat(errorAndResult.getErrors())
        .containsOnly(
            CiviFormError.of(
                String.format(
                    "path '%s' conflicts with question id: %d",
                    questionDefinition.getPath(), applicantName.id)));
  }

  @Test
  public void create_returnsQuestionDefinitionWhenSucceeds() {
    ErrorAnd<QuestionDefinition, CiviFormError> errorAndResult =
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
  }

  @Test
  public void update_returnsQuestionDefinitionWhenSucceeds() throws Exception {
    QuestionDefinition nameQuestion = testQuestionBank.applicantName().getQuestionDefinition();
    QuestionDefinition toUpdate =
        new QuestionDefinitionBuilder(nameQuestion).setDescription("updated description").build();

    ErrorAnd<QuestionDefinition, CiviFormError> errorAndResult = questionService.update(toUpdate);

    assertThat(errorAndResult.isError()).isFalse();
    assertThat(errorAndResult.hasResult()).isTrue();
    assertThat(errorAndResult.getResult().getDescription()).isEqualTo("updated description");
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
  public void update_failsWhenQuestionInvariantsChange() throws Exception {
    QuestionDefinition nameQuestion = testQuestionBank.applicantName().getQuestionDefinition();

    QuestionDefinition toUpdate =
        new QuestionDefinitionBuilder(nameQuestion)
            .setName("this is a new name")
            .setPath(Path.create("this is a new path"))
            .setRepeaterId(Optional.of(100L))
            .setQuestionType(QuestionType.ADDRESS)
            .build();

    ErrorAnd<QuestionDefinition, CiviFormError> errorAndResult = questionService.update(toUpdate);

    assertThat(errorAndResult.hasResult()).isFalse();
    assertThat(errorAndResult.isError()).isTrue();
    assertThat(errorAndResult.getErrors())
        .containsOnly(
            CiviFormError.of(
                String.format(
                    "question names mismatch: %s does not match %s",
                    nameQuestion.getName(), toUpdate.getName())),
            CiviFormError.of(
                String.format(
                    "question paths mismatch: %s does not match %s",
                    nameQuestion.getPath(), toUpdate.getPath())),
            CiviFormError.of(
                String.format(
                    "question repeater ids mismatch: [no repeater] does not match %s",
                    toUpdate.getRepeaterId().get())),
            CiviFormError.of(
                String.format(
                    "question types mismatch: %s does not match %s",
                    nameQuestion.getQuestionType(), toUpdate.getQuestionType())));
  }
}
