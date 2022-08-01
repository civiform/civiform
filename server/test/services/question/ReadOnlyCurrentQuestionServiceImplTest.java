package services.question;

import static org.assertj.core.api.Assertions.assertThat;

import com.google.common.collect.ImmutableList;
import models.Question;
import models.Version;
import org.junit.Before;
import org.junit.Test;
import repository.ResetPostgres;
import repository.VersionRepository;
import services.question.exceptions.QuestionNotFoundException;
import services.question.types.AddressQuestionDefinition;
import services.question.types.NameQuestionDefinition;
import services.question.types.QuestionDefinition;
import services.question.types.TextQuestionDefinition;
import support.TestQuestionBank;

public class ReadOnlyCurrentQuestionServiceImplTest extends ResetPostgres {

  private static final TestQuestionBank testQuestionBank = new TestQuestionBank(false);

  private VersionRepository versionRepository;

  private NameQuestionDefinition nameQuestion;
  private AddressQuestionDefinition addressQuestion;
  private TextQuestionDefinition basicQuestion;
  private ImmutableList<QuestionDefinition> questions;

  @Before
  public void setupQuestions() {
    versionRepository = instanceOf(VersionRepository.class);
    nameQuestion =
        (NameQuestionDefinition) testQuestionBank.applicantName().getQuestionDefinition();
    addressQuestion =
        (AddressQuestionDefinition) testQuestionBank.applicantAddress().getQuestionDefinition();
    basicQuestion =
        (TextQuestionDefinition) testQuestionBank.applicantFavoriteColor().getQuestionDefinition();
    questions = ImmutableList.of(nameQuestion, addressQuestion, basicQuestion);
  }

  @Test
  public void getAll_returnsEmpty() {
    var service = new ReadOnlyCurrentQuestionServiceImpl(versionRepository);
    assertThat(service.getAllQuestions()).isEmpty();
  }

  @Test
  public void getAllQuestions() {
    addQuestionsToVersion(versionRepository.getActiveVersion(), questions);
    var service = new ReadOnlyCurrentQuestionServiceImpl(versionRepository);
    assertThat(service.getAllQuestions().size()).isEqualTo(3);
  }

  @Test
  public void getEnumeratorQuestions() {
    QuestionDefinition enumeratorQuestion =
        testQuestionBank.applicantHouseholdMembers().getQuestionDefinition();

    addQuestionsToVersion(versionRepository.getActiveVersion(), questions);
    addQuestionsToVersion(
        versionRepository.getActiveVersion(), ImmutableList.of(enumeratorQuestion));

    var service = new ReadOnlyCurrentQuestionServiceImpl(versionRepository);

    assertThat(service.getAllEnumeratorQuestions().size()).isEqualTo(1);
    assertThat(service.getAllEnumeratorQuestions().get(0)).isEqualTo(enumeratorQuestion);
    assertThat(service.getUpToDateEnumeratorQuestions().size()).isEqualTo(1);
    assertThat(service.getUpToDateEnumeratorQuestions().get(0)).isEqualTo(enumeratorQuestion);
  }

  @Test
  public void getQuestionDefinition_byId() throws QuestionNotFoundException {
    addQuestionsToVersion(versionRepository.getActiveVersion(), questions);
    long questionId = nameQuestion.getId();

    var service = new ReadOnlyCurrentQuestionServiceImpl(versionRepository);

    assertThat(service.getQuestionDefinition(questionId)).isEqualTo(nameQuestion);
  }

  private void addQuestionsToVersion(Version version, ImmutableList<QuestionDefinition> questions) {
    questions.stream()
        .forEach(
            q -> {
              Question dbQuestion = new Question(q);
              // While we can initialize the Question model from an existing QuestionDefinition,
              // an attempt to update the M2M between versions and questions will not fail silently.
              // This is presumably due to EBean considering the Question as "dirty" or unpersisted.
              dbQuestion.save();
              version.addQuestion(dbQuestion);
            });
    version.save();
  }
}
