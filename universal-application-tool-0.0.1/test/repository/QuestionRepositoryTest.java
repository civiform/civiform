package repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.google.common.collect.ImmutableMap;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import models.Question;
import org.junit.Before;
import org.junit.Test;
import services.question.QuestionDefinition;
import services.question.TextQuestionDefinition;

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
    Question one = saveQuestion("path.one");
    Question two = saveQuestion("path.two");

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
    saveQuestion("path.one");
    Question existing = saveQuestion("path.existing");

    Optional<Question> found = repo.lookupQuestion(existing.id).toCompletableFuture().join();

    assertThat(found).hasValue(existing);
  }

  @Test
  public void lookupPathConflict_returnsFalseWhenNoQuestions() {
    Boolean hasConflict = repo.lookupPathConflict("path.one").toCompletableFuture().join();

    assertThat(hasConflict).isEqualTo(Boolean.FALSE);
  }

  @Test
  public void lookupPathConflict_returnsFalseWhenNoPathConflict() {
    saveQuestion("path.one");
    saveQuestion("path.two");
    Boolean hasConflict = repo.lookupPathConflict("path.other").toCompletableFuture().join();

    assertThat(hasConflict).isEqualTo(Boolean.FALSE);
  }

  @Test
  public void lookupPathConflict_returnsTrueWhenPathConflicts() {
    saveQuestion("path.one");
    saveQuestion("path.two");
    Boolean hasConflict = repo.lookupPathConflict("path.one").toCompletableFuture().join();

    assertThat(hasConflict).isEqualTo(Boolean.TRUE);
  }

  @Test
  public void lookupQuestionByPath_returnsEmptyOptionalWhenQuestionNotFound() {
    Optional<Question> found =
        repo.lookupQuestionByPath("invalid.path").toCompletableFuture().join();

    assertThat(found).isEmpty();
  }

  @Test
  public void lookupQuestionByPath_findsCorrectQuestion() {
    saveQuestion("path.one");
    Question existing = saveQuestion("path.existing");

    Optional<Question> found =
        repo.lookupQuestionByPath("path.existing").toCompletableFuture().join();

    assertThat(found).hasValue(existing);
  }

  @Test
  public void lookupQuestionByPath_findsLatestVersionedQuestion() {
    saveQuestion("path.one");
    Question v2 = saveQuestion("path.one", 2L);

    Optional<Question> found = repo.lookupQuestionByPath("path.one").toCompletableFuture().join();

    assertThat(found).hasValue(v2);
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

  private Question saveQuestion(String path) {
    return saveQuestion(path, 1L);
  }

  private Question saveQuestion(String path, long version) {
    QuestionDefinition definition =
        new TextQuestionDefinition(version, "", path, "", ImmutableMap.of(), ImmutableMap.of());
    Question question = new Question(definition);
    question.save();
    return question;
  }
}
