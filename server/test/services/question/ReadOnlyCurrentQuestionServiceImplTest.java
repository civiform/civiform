package services.question;

import static org.assertj.core.api.Assertions.assertThat;

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

public class ReadOnlyCurrentQuestionServiceImplTest {

  private static final TestQuestionBank testQuestionBank = new TestQuestionBank(false);

  private final ReadOnlyQuestionService emptyService =
      new ReadOnlyCurrentQuestionServiceImpl(
          new Version(LifecycleStage.ACTIVE),
          new Version(LifecycleStage.DRAFT),
          new Version(LifecycleStage.DRAFT));
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
        new ReadOnlyCurrentQuestionServiceImpl(
            new Version(LifecycleStage.ACTIVE) {
              @Override
              public ImmutableList<Question> getQuestions() {
                return questions.stream()
                    .map(q -> new Question(q))
                    .collect(ImmutableList.toImmutableList());
              }
            },
            new Version(LifecycleStage.DRAFT),
            new Version(LifecycleStage.DRAFT));
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
  public void getEnumeratorQuestions() {
    QuestionDefinition enumeratorQuestion =
        testQuestionBank.applicantHouseholdMembers().getQuestionDefinition();

    service =
        new ReadOnlyCurrentQuestionServiceImpl(
            new Version(LifecycleStage.ACTIVE) {
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
            },
            new Version(LifecycleStage.DRAFT),
            new Version(LifecycleStage.DRAFT));

    assertThat(service.getAllEnumeratorQuestions().size()).isEqualTo(1);
    assertThat(service.getAllEnumeratorQuestions().get(0)).isEqualTo(enumeratorQuestion);
    assertThat(service.getUpToDateEnumeratorQuestions().size()).isEqualTo(1);
    assertThat(service.getUpToDateEnumeratorQuestions().get(0)).isEqualTo(enumeratorQuestion);
  }

  @Test
  public void getQuestionDefinition_byId() throws QuestionNotFoundException {
    long questionId = nameQuestion.getId();
    assertThat(service.getQuestionDefinition(questionId)).isEqualTo(nameQuestion);
  }
}
