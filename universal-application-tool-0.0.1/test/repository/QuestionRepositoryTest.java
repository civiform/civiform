package repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.google.common.collect.ImmutableMap;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import models.LifecycleStage;
import models.Question;
import org.junit.Before;
import org.junit.Test;
import services.Path;
import services.question.exceptions.UnsupportedQuestionTypeException;
import services.question.types.QuestionDefinition;
import services.question.types.QuestionDefinitionBuilder;
import services.question.types.QuestionType;
import services.question.types.TextQuestionDefinition;
import support.TestQuestionBank;

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
  public void findPathConflictingQuestion_noConflicts_ok() throws Exception {
    QuestionDefinition applicantAddress =
        TestQuestionBank.applicantAddress().getQuestionDefinition();
    QuestionDefinition newQuestionDefinition =
        new QuestionDefinitionBuilder(applicantAddress)
            .clearId()
            .setName("a brand new question")
            .setPath(Path.create("applicant.a_brand_new_question"))
            .build();

    Optional<Question> maybeConflict = repo.findPathConflictingQuestion(newQuestionDefinition);

    assertThat(maybeConflict).isEmpty();
  }

  @Test
  public void findPathConflictingQuestion_sameQuestion_hasConflict() {
    Question applicantAddress = TestQuestionBank.applicantAddress();
    Optional<Question> maybeConflict =
        repo.findPathConflictingQuestion(applicantAddress.getQuestionDefinition());

    assertThat(maybeConflict).contains(applicantAddress);
  }

  @Test
  public void findPathConflictingQuestion_differentVersion_hasConflict() throws Exception {
    Question applicantName = TestQuestionBank.applicantName();
    QuestionDefinition questionDefinition =
        new QuestionDefinitionBuilder(applicantName.getQuestionDefinition())
            .setId(123123L)
            .setVersion(433L)
            .build();

    Optional<Question> maybeConflict = repo.findPathConflictingQuestion(questionDefinition);

    assertThat(maybeConflict).contains(applicantName);
  }

  @Test
  public void findPathConflictingQuestion_samePath_hasConflict() throws Exception {
    Question applicantName = TestQuestionBank.applicantName();
    QuestionDefinition questionDefinition =
        new QuestionDefinitionBuilder(applicantName.getQuestionDefinition()).clearId().build();

    Optional<Question> maybeConflict = repo.findPathConflictingQuestion(questionDefinition);

    assertThat(maybeConflict).hasValue(applicantName);
  }

  @Test
  public void findPathConflictingQuestion_repeaterConflictsWithNonRepeater_hasConflict()
      throws Exception {
    Question householdMembers = TestQuestionBank.applicantHouseholdMembers();
    QuestionDefinition newQuestionDefinition =
        new QuestionDefinitionBuilder(householdMembers.getQuestionDefinition())
            .clearId()
            .setPath(householdMembers.getQuestionDefinition().getPath().withoutArrayReference())
            .setQuestionType(QuestionType.TEXT)
            .build();

    Optional<Question> maybeConflict = repo.findPathConflictingQuestion(newQuestionDefinition);

    assertThat(maybeConflict).contains(householdMembers);
  }

  @Test
  public void findPathConflictingQuestion_startsWithNewPath_hasConflict() throws Exception {
    Question applicantName = TestQuestionBank.applicantName();
    QuestionDefinition newQuestionDefinition =
        new QuestionDefinitionBuilder(applicantName.getQuestionDefinition())
            .clearId()
            .setPath(applicantName.getQuestionDefinition().getPath().parentPath())
            .build();

    Optional<Question> maybeConflict = repo.findPathConflictingQuestion(newQuestionDefinition);

    assertThat(maybeConflict).contains(applicantName);
  }

  @Test
  public void findPathConflictingQuestion_newPathStartsWithNonRepeater_hasConflict()
      throws Exception {
    Question applicantName = TestQuestionBank.applicantName();
    QuestionDefinition newQuestionDefinition =
        new QuestionDefinitionBuilder(applicantName.getQuestionDefinition())
            .clearId()
            .setPath(applicantName.getQuestionDefinition().getPath().join("another segment"))
            .build();

    Optional<Question> maybeConflict = repo.findPathConflictingQuestion(newQuestionDefinition);

    assertThat(maybeConflict).contains(applicantName);
  }

  @Test
  public void findPathConflictingQuestion_newPathStartsWithRepeater_hasNoConflict()
      throws Exception {
    Question householdMembers = TestQuestionBank.applicantHouseholdMembers();
    QuestionDefinition newQuestionDefinition =
        new QuestionDefinitionBuilder(householdMembers.getQuestionDefinition())
            .clearId()
            .setPath(householdMembers.getQuestionDefinition().getPath().join("another segment"))
            .build();

    Optional<Question> maybeConflict = repo.findPathConflictingQuestion(newQuestionDefinition);

    assertThat(maybeConflict).isEmpty();
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
        .hasCauseInstanceOf(javax.persistence.NonUniqueResultException.class)
        .hasMessageContaining("expecting 0 or 1 results but got [2]");
  }

  @Test
  public void insertQuestion() {
    QuestionDefinition questionDefinition =
        new TextQuestionDefinition(
            2L,
            "question",
            Path.create("applicant.name"),
            Optional.empty(),
            "applicant's name",
            LifecycleStage.ACTIVE,
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
            Path.create("applicant.name"),
            Optional.empty(),
            "applicant's name",
            LifecycleStage.ACTIVE,
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
