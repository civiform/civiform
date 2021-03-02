package repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.google.common.collect.ImmutableMap;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import models.Question;
import org.junit.Before;
import org.junit.Test;
import services.question.QuestionDefinition;
import services.question.QuestionDefinitionBuilder;
import services.question.TextQuestionDefinition;
import services.question.UnsupportedQuestionTypeException;

public class QuestionRepositoryTest extends WithPostgresContainer {

  private QuestionRepository repo;

  @Before
  public void setupQuestionRepository() {
    repo = instanceOf(QuestionRepository.class);
  }

  @Test
  public void listQuestions_empty() {
    assertThat(repo.listQuestions().toCompletableFuture().join()).isEmpty();
  }

  @Test
  public void listQuestions() {
    Question one = resourceCreator().insertQuestion("path.one");
    Question two = resourceCreator().insertQuestion("path.two");

    Set<Question> list = repo.listQuestions().toCompletableFuture().join();

    assertThat(list).containsExactly(one, two);
  }

  @Test
  public void lookupQuestion_returnsEmptyOptionalWhenQuestionNotFound() {
    Optional<Question> found = repo.lookupQuestion(1L).toCompletableFuture().join();

    assertThat(found).isEmpty();
  }

  @Test
  public void lookupQuestion_findsCorrectQuestion() {
    resourceCreator().insertQuestion("path.one");
    Question existing = resourceCreator().insertQuestion("path.existing");

    Optional<Question> found = repo.lookupQuestion(existing.id).toCompletableFuture().join();

    assertThat(found).hasValue(existing);
  }

  @Test
  public void pathConflicts_returnsTrueForBadPaths() {
    String path = "applicant.address";

    assertThat(pathConflicts(path, "applicant")).isTrue();
    assertThat(pathConflicts(path, "Applicant.Address")).isTrue();
    assertThat(pathConflicts(path, "applicant.address.street")).isTrue();
    assertThat(pathConflicts(path, "applicant.address.some.other.field")).isTrue();
  }

  @Test
  public void pathConflicts_returnsFalseForValidPaths() {
    String path = "applicant.address";

    assertThat(pathConflicts(path, "applicant.employment")).isFalse();
    assertThat(pathConflicts(path, "other.path")).isFalse();
    assertThat(pathConflicts(path, "other.applicant")).isFalse();
    assertThat(pathConflicts(path, "other.applicant.address")).isFalse();
    assertThat(pathConflicts(path, "other.applicant.address.street")).isFalse();
    assertThat(pathConflicts(path, "other.applicant.address.some.other.field")).isFalse();
  }

  private boolean pathConflicts(String path, String otherPath) {
    return QuestionRepository.PathConflictDetector.pathConflicts(path, otherPath);
  }

  @Test
  public void findConflictingQuestion_returnsEmptyWhenNoQuestions() {
    Optional<Question> found =
        repo.findConflictingQuestion("path.one").toCompletableFuture().join();

    assertThat(found).isEmpty();
  }

  @Test
  public void findConflictingQuestion_returnsEmptyWhenNoPathConflict() {
    resourceCreator().insertQuestion("path.one");
    resourceCreator().insertQuestion("path.two");

    Optional<Question> found =
        repo.findConflictingQuestion("path.other").toCompletableFuture().join();

    assertThat(found).isEmpty();
  }

  @Test
  public void findConflictingQuestion_returnsQuestionWhenConflictingPath() {
    Question questionOne = resourceCreator().insertQuestion("path.one");
    resourceCreator().insertQuestion("path.two");

    Optional<Question> found =
        repo.findConflictingQuestion("path.one").toCompletableFuture().join();

    assertThat(found).hasValue(questionOne);
  }

  @Test
  public void lookupQuestionByPath_returnsEmptyOptionalWhenQuestionNotFound() {
    Optional<Question> found =
        repo.lookupQuestionByPath("invalid.path").toCompletableFuture().join();

    assertThat(found).isEmpty();
  }

  @Test
  public void lookupQuestionByPath_findsCorrectQuestion() {
    resourceCreator().insertQuestion("path.one");
    Question existing = resourceCreator().insertQuestion("path.existing");

    Optional<Question> found =
        repo.lookupQuestionByPath("path.existing").toCompletableFuture().join();

    assertThat(found).hasValue(existing);
  }

  @Test
  public void lookupQuestionByPath_versioningNotSupportedYet() {
    resourceCreator().insertQuestion("path.one");
    resourceCreator().insertQuestion("path.one", 2L);

    assertThatThrownBy(() -> repo.lookupQuestionByPath("path.one").toCompletableFuture().join())
        .isInstanceOf(java.util.concurrent.CompletionException.class)
        .hasMessageContaining("NonUniqueResultException")
        .hasMessageContaining("expecting 0 or 1 results but got [2]");
  }

  @Test
  public void insertQuestion() {
    QuestionDefinition questionDefinition =
        new TextQuestionDefinition(
            2L,
            "question",
            "applicant.name",
            "applicant's name",
            ImmutableMap.of(Locale.US, "What is your name?"),
            ImmutableMap.of());
    Question question = new Question(questionDefinition);

    repo.insertQuestion(question).toCompletableFuture().join();

    long id = question.id;
    Question q = repo.lookupQuestion(id).toCompletableFuture().join().get();
    assertThat(q.id).isEqualTo(id);
  }

  @Test
  public void insertQuestionSync() {
    QuestionDefinition questionDefinition =
        new TextQuestionDefinition(
            2L,
            "question",
            "applicant.name",
            "applicant's name",
            ImmutableMap.of(Locale.US, "What is your name?"),
            ImmutableMap.of());
    Question question = new Question(questionDefinition);

    repo.insertQuestionSync(question);

    assertThat(repo.lookupQuestion(question.id).toCompletableFuture().join()).hasValue(question);
  }

  @Test
  public void updateQuestion() throws UnsupportedQuestionTypeException {
    Question question = resourceCreator().insertQuestion("path.one");
    QuestionDefinition questionDefinition = question.getQuestionDefinition();
    questionDefinition =
        new QuestionDefinitionBuilder(questionDefinition).setDescription("new description").build();

    repo.updateQuestion(new Question(questionDefinition)).toCompletableFuture().join();

    Question q = repo.lookupQuestion(question.id).toCompletableFuture().join().get();
    assertThat(q.getQuestionDefinition()).isEqualTo(questionDefinition);
  }

  @Test
  public void updateQuestionSync() throws UnsupportedQuestionTypeException {
    Question question = resourceCreator().insertQuestion("path.one");
    QuestionDefinition questionDefinition = question.getQuestionDefinition();
    questionDefinition =
        new QuestionDefinitionBuilder(questionDefinition).setDescription("new description").build();

    repo.updateQuestionSync(new Question(questionDefinition));

    Question q = repo.lookupQuestion(question.id).toCompletableFuture().join().get();
    assertThat(q.getQuestionDefinition()).isEqualTo(questionDefinition);
  }
}
