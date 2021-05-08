package services.question;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.google.common.collect.ImmutableList;
import java.util.Optional;
import models.LifecycleStage;
import models.Question;
import models.Version;
import org.junit.Before;
import org.junit.Test;
import services.Path;
import services.question.exceptions.InvalidQuestionTypeException;
import services.question.exceptions.QuestionNotFoundException;
import services.question.types.AddressQuestionDefinition;
import services.question.types.NameQuestionDefinition;
import services.question.types.QuestionDefinition;
import services.question.types.TextQuestionDefinition;
import support.TestQuestionBank;

public class ReadOnlyQuestionServiceImplTest {

  private static final TestQuestionBank testQuestionBank = new TestQuestionBank(false);

  private final ReadOnlyQuestionService emptyService =
      new ReadOnlyQuestionServiceImpl(
          new Version(LifecycleStage.ACTIVE), new Version(LifecycleStage.DRAFT));
  private NameQuestionDefinition nameQuestion;
  private AddressQuestionDefinition addressQuestion;
  private TextQuestionDefinition basicQuestion;
  private ImmutableList<QuestionDefinition> questions;
  private ReadOnlyQuestionService service;

  @Before
  public void setupQuestions() {
    // The tests mimic that the persisted questions are read into ReadOnlyQuestionService.
    // Therefore, question ids cannot be of.
    nameQuestion =
        (NameQuestionDefinition) testQuestionBank.applicantName().getQuestionDefinition();
    addressQuestion =
        (AddressQuestionDefinition) testQuestionBank.applicantAddress().getQuestionDefinition();
    basicQuestion =
        (TextQuestionDefinition) testQuestionBank.applicantFavoriteColor().getQuestionDefinition();
    questions = ImmutableList.of(nameQuestion, addressQuestion, basicQuestion);
    service =
        new ReadOnlyQuestionServiceImpl(
            new Version(LifecycleStage.ACTIVE) {
              @Override
              public ImmutableList<Question> getQuestions() {
                return questions.stream()
                    .map(q -> new Question(q))
                    .collect(ImmutableList.toImmutableList());
              }
            },
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
        new ReadOnlyQuestionServiceImpl(
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
            new Version(LifecycleStage.DRAFT));

    assertThat(service.getAllEnumeratorQuestions().size()).isEqualTo(1);
    assertThat(service.getAllEnumeratorQuestions().get(0)).isEqualTo(enumeratorQuestion);
    assertThat(service.getUpToDateEnumeratorQuestions().size()).isEqualTo(1);
    assertThat(service.getUpToDateEnumeratorQuestions().get(0)).isEqualTo(enumeratorQuestion);
  }

  @Test
  public void makePath() throws Exception {
    Path path = service.makePath(Optional.empty(), "this is foRMA$$$tte34d_we_i!!rd", false);
    assertThat(path).isEqualTo(Path.create("applicant.this_is_formattedweird"));
  }

  @Test
  public void makePath_withEnumerator() throws Exception {
    QuestionDefinition householdMembers =
        testQuestionBank.applicantHouseholdMembers().getQuestionDefinition();
    service =
        new ReadOnlyQuestionServiceImpl(
            new Version(LifecycleStage.ACTIVE) {
              @Override
              public ImmutableList<Question> getQuestions() {
                return ImmutableList.<Question>builder()
                    .add(new Question(householdMembers))
                    .build();
              }
            },
            new Version(LifecycleStage.DRAFT));

    Path path = service.makePath(Optional.of(householdMembers.getId()), "some question name", true);

    assertThat(path)
        .isEqualTo(Path.create("applicant.applicant_household_members[].some_question_name[]"));
  }

  @Test
  public void makePath_withBadEnumerator_throws() {
    QuestionDefinition applicantName = testQuestionBank.applicantName().getQuestionDefinition();
    service =
        new ReadOnlyQuestionServiceImpl(
            new Version(LifecycleStage.ACTIVE) {
              @Override
              public ImmutableList<Question> getQuestions() {
                return ImmutableList.<Question>builder().add(new Question(applicantName)).build();
              }
            },
            new Version(LifecycleStage.DRAFT));

    assertThatThrownBy(
            () -> service.makePath(Optional.of(applicantName.getId()), "some question name", true))
        .isInstanceOf(InvalidQuestionTypeException.class)
        .hasMessage("NAME is not a valid question type.");
  }

  @Test
  public void makePath_isRepeated() throws Exception {
    Path path = service.makePath(Optional.empty(), "this is foRMA$$$tte34d_we_i!!rd", true);
    assertThat(path).isEqualTo(Path.create("applicant.this_is_formattedweird[]"));
  }

  @Test
  public void getQuestionDefinition_byId() throws QuestionNotFoundException {
    long questionId = nameQuestion.getId();
    assertThat(service.getQuestionDefinition(questionId)).isEqualTo(nameQuestion);
  }
}
