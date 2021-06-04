package repository;

import static org.assertj.core.api.Assertions.assertThat;

import io.ebean.DB;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import models.Question;
import org.junit.Before;
import org.junit.Test;
import services.LocalizedStrings;
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
    Question one = resourceCreator.insertQuestion();
    Question two = resourceCreator.insertQuestion();

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
    resourceCreator.insertQuestion();
    Question existing = resourceCreator.insertQuestion();

    Optional<Question> found = repo.lookupQuestion(existing.id).toCompletableFuture().join();

    assertThat(found).hasValue(existing);
  }

  @Test
  public void findConflictingQuestion_noConflicts_ok() throws Exception {
    QuestionDefinition applicantAddress =
        testQuestionBank.applicantAddress().getQuestionDefinition();
    QuestionDefinition newQuestionDefinition =
        new QuestionDefinitionBuilder(applicantAddress)
            .clearId()
            .setName("a brand new question")
            .build();

    Optional<Question> maybeConflict = repo.findConflictingQuestion(newQuestionDefinition);

    assertThat(maybeConflict).isEmpty();
  }

  @Test
  public void findConflictingQuestion_sameName_hasConflict() throws Exception {
    Question applicantAddress = testQuestionBank.applicantAddress();
    QuestionDefinition newQuestionDefinition =
        new QuestionDefinitionBuilder(applicantAddress.getQuestionDefinition())
            .clearId()
            .setEnumeratorId(Optional.of(1L))
            .build();

    Optional<Question> maybeConflict = repo.findConflictingQuestion(newQuestionDefinition);

    assertThat(maybeConflict).contains(applicantAddress);
  }

  @Test
  public void findConflictingQuestion_sameQuestionPathSegment_hasConflict() throws Exception {
    Question applicantAddress = testQuestionBank.applicantAddress();
    QuestionDefinition newQuestionDefinition =
        new QuestionDefinitionBuilder(applicantAddress.getQuestionDefinition())
            .clearId()
            .setName("applicant address!")
            .build();

    Optional<Question> maybeConflict = repo.findConflictingQuestion(newQuestionDefinition);

    assertThat(maybeConflict).contains(applicantAddress);
  }

  @Test
  public void findConflictingQuestion_sameQuestionPathSegmentButDifferentEnumeratorId_ok()
      throws Exception {
    Question applicantAddress = testQuestionBank.applicantAddress();
    QuestionDefinition newQuestionDefinition =
        new QuestionDefinitionBuilder(applicantAddress.getQuestionDefinition())
            .clearId()
            .setName("applicant_address")
            .setEnumeratorId(Optional.of(1L))
            .build();

    Optional<Question> maybeConflict = repo.findConflictingQuestion(newQuestionDefinition);

    assertThat(maybeConflict).isEmpty();
  }

  @Test
  public void findConflictingQuestion_sameQuestion_hasConflict() {
    Question applicantAddress = testQuestionBank.applicantAddress();
    Optional<Question> maybeConflict =
        repo.findConflictingQuestion(applicantAddress.getQuestionDefinition());

    assertThat(maybeConflict).contains(applicantAddress);
  }

  @Test
  public void findConflictingQuestion_differentVersion_hasConflict() throws Exception {
    Question applicantName = testQuestionBank.applicantName();
    QuestionDefinition questionDefinition =
        new QuestionDefinitionBuilder(applicantName.getQuestionDefinition()).setId(123123L).build();

    Optional<Question> maybeConflict = repo.findConflictingQuestion(questionDefinition);

    assertThat(maybeConflict).contains(applicantName);
  }

  @Test
  public void insertQuestion() {
    QuestionDefinition questionDefinition =
        new TextQuestionDefinition(
            "question",
            Optional.empty(),
            "applicant's name",
            LocalizedStrings.of(Locale.US, "What is your name?"),
            LocalizedStrings.empty());
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
            Optional.empty(),
            "applicant's name",
            LocalizedStrings.of(Locale.US, "What is your name?"),
            LocalizedStrings.empty());
    Question question = new Question(questionDefinition);

    repo.insertQuestionSync(question);

    assertThat(repo.lookupQuestion(question.id).toCompletableFuture().join()).hasValue(question);
  }

  @Test
  public void updateQuestion() throws UnsupportedQuestionTypeException {
    Question question = resourceCreator.insertQuestion();
    QuestionDefinition questionDefinition = question.getQuestionDefinition();
    questionDefinition =
        new QuestionDefinitionBuilder(questionDefinition).setDescription("new description").build();

    repo.updateQuestion(new Question(questionDefinition)).toCompletableFuture().join();

    Question q = repo.lookupQuestion(question.id).toCompletableFuture().join().get();
    assertThat(q.getQuestionDefinition()).isEqualTo(questionDefinition);
  }

  @Test
  public void updateQuestionSync() throws UnsupportedQuestionTypeException {
    Question question = resourceCreator.insertQuestion();
    QuestionDefinition questionDefinition = question.getQuestionDefinition();
    questionDefinition =
        new QuestionDefinitionBuilder(questionDefinition).setDescription("new description").build();

    repo.updateQuestionSync(new Question(questionDefinition));

    Question q = repo.lookupQuestion(question.id).toCompletableFuture().join().get();
    assertThat(q.getQuestionDefinition()).isEqualTo(questionDefinition);
  }

  @Test
  public void loadLegacy() {
    DB.sqlUpdate(
            "insert into questions (name, description, legacy_question_text,"
                + " legacy_question_help_text, question_type) values ('old schema"
                + " entry', 'description', '{\"en_us\": \"text\"}', '{\"en_us\": \"help\"}',"
                + " 'REPEATER');")
        .execute();

    Question found =
        repo.listQuestions().toCompletableFuture().join().stream()
            .filter(
                question -> question.getQuestionDefinition().getName().equals("old schema entry"))
            .findFirst()
            .get();

    assertThat(found.getQuestionDefinition().getQuestionText())
        .isEqualTo(LocalizedStrings.of(Locale.US, "text"));
    assertThat(found.getQuestionDefinition().getQuestionHelpText())
        .isEqualTo(LocalizedStrings.of(Locale.US, "help"));
  }
}
