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
import services.Path;
import services.question.exceptions.UnsupportedQuestionTypeException;
import services.question.types.QuestionDefinition;
import services.question.types.QuestionDefinitionBuilder;
import services.question.types.TextQuestionDefinition;

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
    Question one = resourceCreator.insertQuestion("path.one");
    Question two = resourceCreator.insertQuestion("path.two");

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
    resourceCreator.insertQuestion("path.one");
    Question existing = resourceCreator.insertQuestion("path.existing");

    Optional<Question> found = repo.lookupQuestion(existing.id).toCompletableFuture().join();

    assertThat(found).hasValue(existing);
  }

  @Test
  public void findPathConflictingQuestion_noConflicts_ok() throws Exception {
    QuestionDefinition applicantAddress =
        testQuestionBank.applicantAddress().getQuestionDefinition();
    QuestionDefinition newQuestionDefinition =
        new QuestionDefinitionBuilder(applicantAddress)
            .clearId()
            .setName("a brand new question")
            .setPath(Path.create("applicant.a_brand_new_question"))
            .build();

    Optional<Question> maybeConflict = repo.findConflictingQuestion(newQuestionDefinition);

    assertThat(maybeConflict).isEmpty();
  }

  @Test
  public void findPathConflictingQuestion_sameQuestionPathSegment_hasConflict() throws Exception {
    Question applicantAddress = testQuestionBank.applicantAddress();
    QuestionDefinition newQuestionDefinition =
        new QuestionDefinitionBuilder(applicantAddress.getQuestionDefinition())
            .clearId()
            .setName("applicant_address")
            .setPath(Path.create("fake"))
            .build();

    Optional<Question> maybeConflict = repo.findConflictingQuestion(newQuestionDefinition);

    assertThat(maybeConflict).contains(applicantAddress);
  }

  @Test
  public void findPathConflictingQuestion_sameQuestionPathSegmentButDifferentRepeaterId_ok()
      throws Exception {
    Question applicantAddress = testQuestionBank.applicantAddress();
    QuestionDefinition newQuestionDefinition =
        new QuestionDefinitionBuilder(applicantAddress.getQuestionDefinition())
            .clearId()
            .setName("applicant_address")
            .setRepeaterId(Optional.of(1L))
            .setPath(Path.create("fake"))
            .build();

    Optional<Question> maybeConflict = repo.findConflictingQuestion(newQuestionDefinition);

    assertThat(maybeConflict).isEmpty();
  }

  @Test
  public void findPathConflictingQuestion_sameQuestion_hasConflict() {
    Question applicantAddress = testQuestionBank.applicantAddress();
    Optional<Question> maybeConflict =
        repo.findConflictingQuestion(applicantAddress.getQuestionDefinition());

    assertThat(maybeConflict).contains(applicantAddress);
  }

  @Test
  public void findPathConflictingQuestion_differentVersion_hasConflict() throws Exception {
    Question applicantName = testQuestionBank.applicantName();
    QuestionDefinition questionDefinition =
        new QuestionDefinitionBuilder(applicantName.getQuestionDefinition()).setId(123123L).build();

    Optional<Question> maybeConflict = repo.findConflictingQuestion(questionDefinition);

    assertThat(maybeConflict).contains(applicantName);
  }

  @Test
  public void lookupQuestionByPath_returnsEmptyOptionalWhenQuestionNotFound() {
    Optional<Question> found =
        repo.lookupQuestionByPath("invalid.path").toCompletableFuture().join();

    assertThat(found).isEmpty();
  }

  @Test
  public void lookupQuestionByPath_findsCorrectQuestion() {
    resourceCreator.insertQuestion("path.one");
    Question existing = resourceCreator.insertQuestion("path.existing");

    Optional<Question> found =
        repo.lookupQuestionByPath("path.existing").toCompletableFuture().join();

    assertThat(found).hasValue(existing);
  }

  @Test
  public void lookupQuestionByPath_versioningNotSupportedYet() {
    resourceCreator.insertQuestion("path.one");
    resourceCreator.insertQuestion("path.one");

    assertThatThrownBy(() -> repo.lookupQuestionByPath("path.one").toCompletableFuture().join())
        .isInstanceOf(java.util.concurrent.CompletionException.class)
        .hasCauseInstanceOf(javax.persistence.NonUniqueResultException.class)
        .hasMessageContaining("expecting 0 or 1 results but got [2]");
  }

  @Test
  public void insertQuestion() {
    QuestionDefinition questionDefinition =
        new TextQuestionDefinition(
            "question",
            Path.create("applicant.name"),
            Optional.empty(),
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
            "question",
            Path.create("applicant.name"),
            Optional.empty(),
            "applicant's name",
            ImmutableMap.of(Locale.US, "What is your name?"),
            ImmutableMap.of());
    Question question = new Question(questionDefinition);

    repo.insertQuestionSync(question);

    assertThat(repo.lookupQuestion(question.id).toCompletableFuture().join()).hasValue(question);
  }

  @Test
  public void updateQuestion() throws UnsupportedQuestionTypeException {
    Question question = resourceCreator.insertQuestion("path.one");
    QuestionDefinition questionDefinition = question.getQuestionDefinition();
    questionDefinition =
        new QuestionDefinitionBuilder(questionDefinition).setDescription("new description").build();

    repo.updateQuestion(new Question(questionDefinition)).toCompletableFuture().join();

    Question q = repo.lookupQuestion(question.id).toCompletableFuture().join().get();
    assertThat(q.getQuestionDefinition()).isEqualTo(questionDefinition);
  }

  @Test
  public void updateQuestionSync() throws UnsupportedQuestionTypeException {
    Question question = resourceCreator.insertQuestion("path.one");
    QuestionDefinition questionDefinition = question.getQuestionDefinition();
    questionDefinition =
        new QuestionDefinitionBuilder(questionDefinition).setDescription("new description").build();

    repo.updateQuestionSync(new Question(questionDefinition));

    Question q = repo.lookupQuestion(question.id).toCompletableFuture().join().get();
    assertThat(q.getQuestionDefinition()).isEqualTo(questionDefinition);
  }
}
