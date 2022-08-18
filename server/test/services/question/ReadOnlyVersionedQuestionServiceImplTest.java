package services.question;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.google.common.collect.ImmutableList;
import models.LifecycleStage;
import models.Question;
import models.Version;
import org.junit.Before;
import org.junit.Test;
import repository.ResetPostgres;
import services.question.exceptions.QuestionNotFoundException;
import support.TestQuestionBank;

public class ReadOnlyVersionedQuestionServiceImplTest extends ResetPostgres {

  private final ReadOnlyQuestionService emptyService =
      new ReadOnlyVersionedQuestionServiceImpl(new Version(LifecycleStage.OBSOLETE));
  private TestQuestionBank testQuestionBank;
  private Question nameQuestion;
  private Question addressQuestion;
  private Question basicQuestion;
  private ImmutableList<Question> questions;
  private ReadOnlyQuestionService service;

  @Before
  public void setupQuestions() {
    testQuestionBank = new TestQuestionBank(true);
    // The question bank initializes these questions in the active version.
    nameQuestion = testQuestionBank.applicantName();
    addressQuestion = testQuestionBank.applicantAddress();
    basicQuestion = testQuestionBank.applicantFavoriteColor();
    questions = ImmutableList.of(nameQuestion, addressQuestion, basicQuestion);
    Version version = new Version(LifecycleStage.OBSOLETE);
    addQuestionsToVersion(version, questions);
    service = new ReadOnlyVersionedQuestionServiceImpl(version);
  }

  @Test
  public void getAll_returnsEmpty() {
    assertThat(emptyService.getAllQuestions()).isEmpty();
  }

  @Test
  public void getAllQuestions() {
    assertThat(service.getAllQuestions().size()).isEqualTo(3);
  }

  @Test
  public void getActiveAndDraftQuestions_notSupported() {
    assertThatThrownBy(() -> service.getActiveAndDraftQuestions())
        .isInstanceOf(RuntimeException.class)
        .hasMessageContaining("does not support getActiveAndDraftQuestions");
  }

  @Test
  public void getUpToDateQuestions_notSupported() {
    assertThatThrownBy(() -> service.getUpToDateQuestions())
        .isInstanceOf(RuntimeException.class)
        .hasMessageContaining("does not support getUpToDateQuestions");
  }

  @Test
  public void getUpToDateEnumeratorQuestions_notSupported() {
    assertThatThrownBy(() -> service.getUpToDateEnumeratorQuestions())
        .isInstanceOf(RuntimeException.class)
        .hasMessageContaining("does not support getUpToDateEnumeratorQuestions");
  }

  @Test
  public void getEnumeratorQuestions() {
    Question enumeratorQuestion = testQuestionBank.applicantHouseholdMembers();

    Version version = new Version(LifecycleStage.OBSOLETE);
    addQuestionsToVersion(version, questions);
    addQuestionsToVersion(version, ImmutableList.of(enumeratorQuestion));
    var service = new ReadOnlyVersionedQuestionServiceImpl(version);
    assertThat(service.getAllEnumeratorQuestions().size()).isEqualTo(1);
    assertThat(service.getAllEnumeratorQuestions().get(0))
        .isEqualTo(enumeratorQuestion.getQuestionDefinition());
  }

  @Test
  public void getQuestionDefinition_byId() throws QuestionNotFoundException {
    long questionId = nameQuestion.id;
    assertThat(service.getQuestionDefinition(questionId))
        .isEqualTo(nameQuestion.getQuestionDefinition());
  }

  @Test
  public void getQuestionDefinition_notFound() throws QuestionNotFoundException {
    assertThatThrownBy(() -> service.getQuestionDefinition(9999L))
        .isInstanceOf(QuestionNotFoundException.class);
  }

  private static void addQuestionsToVersion(Version version, ImmutableList<Question> questions) {
    questions.stream()
        .forEach(
            q -> {
              version.addQuestion(q);
            });
    version.save();
  }
}
