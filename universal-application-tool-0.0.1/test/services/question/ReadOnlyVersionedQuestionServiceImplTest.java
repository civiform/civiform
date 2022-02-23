package services.question;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.google.common.collect.ImmutableList;
import models.LifecycleStage;
import models.Question;
import models.Version;
import org.junit.Before;
import org.junit.Test;
import services.question.exceptions.QuestionNotFoundException;
import services.question.types.AddressQuestionDefinition;
import services.question.types.NameQuestionDefinition;
import services.question.types.QuestionDefinition;
import services.question.types.TextQuestionDefinition;
import support.TestQuestionBank;

public class ReadOnlyVersionedQuestionServiceImplTest {

  private static final TestQuestionBank testQuestionBank = new TestQuestionBank(false);

  private final ReadOnlyQuestionService emptyService =
      new ReadOnlyVersionedQuestionServiceImpl(new Version(LifecycleStage.OBSOLETE));
  private NameQuestionDefinition nameQuestion;
  private AddressQuestionDefinition addressQuestion;
  private TextQuestionDefinition basicQuestion;
  private ImmutableList<QuestionDefinition> questions;
  private ReadOnlyQuestionService service;

  @Before
  public void setupQuestions() {
    nameQuestion =
        (NameQuestionDefinition) testQuestionBank.applicantName().getQuestionDefinition();
    addressQuestion =
        (AddressQuestionDefinition) testQuestionBank.applicantAddress().getQuestionDefinition();
    basicQuestion =
        (TextQuestionDefinition) testQuestionBank.applicantFavoriteColor().getQuestionDefinition();
    questions = ImmutableList.of(nameQuestion, addressQuestion, basicQuestion);
    service =
        new ReadOnlyVersionedQuestionServiceImpl(
            new Version(LifecycleStage.OBSOLETE) {
              @Override
              public ImmutableList<Question> getQuestions() {
                return questions.stream()
                    .map(q -> new Question(q))
                    .collect(ImmutableList.toImmutableList());
              }
            });
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
    QuestionDefinition enumeratorQuestion =
        testQuestionBank.applicantHouseholdMembers().getQuestionDefinition();

    service =
        new ReadOnlyVersionedQuestionServiceImpl(
            new Version(LifecycleStage.OBSOLETE) {
              @Override
              public ImmutableList<Question> getQuestions() {
                return ImmutableList.<QuestionDefinition>builder()
                    .addAll(questions)
                    .add(enumeratorQuestion)
                    .build()
                    .stream()
                    .map(q -> new Question(q))
                    .collect(ImmutableList.toImmutableList());
              }
            });

    assertThat(service.getAllEnumeratorQuestions().size()).isEqualTo(1);
    assertThat(service.getAllEnumeratorQuestions().get(0)).isEqualTo(enumeratorQuestion);
  }

  @Test
  public void getQuestionDefinition_byId() throws QuestionNotFoundException {
    long questionId = nameQuestion.getId();
    assertThat(service.getQuestionDefinition(questionId)).isEqualTo(nameQuestion);
  }

  @Test
  public void getQuestionDefinition_notFound() throws QuestionNotFoundException {
    assertThatThrownBy(() -> service.getQuestionDefinition(9999L))
        .isInstanceOf(QuestionNotFoundException.class);
  }
}
