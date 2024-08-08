package services.question;

import static org.assertj.core.api.Assertions.assertThat;

import models.QuestionModel;
import org.junit.Before;
import org.junit.Test;
import repository.ResetPostgres;
import repository.VersionRepository;
import services.question.exceptions.QuestionNotFoundException;
import support.TestQuestionBank;

public class ReadOnlyCurrentQuestionServiceImplTest extends ResetPostgres {

  private VersionRepository versionRepository;
  private TestQuestionBank testQuestionBank;

  @Before
  public void setupQuestions() {
    versionRepository = instanceOf(VersionRepository.class);
    testQuestionBank = new TestQuestionBank(true);
  }

  @Test
  public void getAll_returnsEmpty() {
    var service = new ReadOnlyCurrentQuestionServiceImpl(versionRepository);
    assertThat(service.getAllQuestions()).isEmpty();
  }

  @Test
  public void getAllQuestions() {
    // The question bank initializes these in the active version.
    testQuestionBank.nameApplicantName();
    testQuestionBank.addressApplicantAddress();
    testQuestionBank.textApplicantFavoriteColor();
    var service = new ReadOnlyCurrentQuestionServiceImpl(versionRepository);
    assertThat(service.getAllQuestions().size()).isEqualTo(3);
  }

  @Test
  public void getEnumeratorQuestions() {
    // The question bank initializes this question in the active version.
    QuestionModel enumeratorQuestion = testQuestionBank.enumeratorApplicantHouseholdMembers();

    var service = new ReadOnlyCurrentQuestionServiceImpl(versionRepository);

    assertThat(service.getAllEnumeratorQuestions().size()).isEqualTo(1);
    assertThat(service.getAllEnumeratorQuestions().get(0))
        .isEqualTo(enumeratorQuestion.getQuestionDefinition());
    assertThat(service.getUpToDateEnumeratorQuestions().size()).isEqualTo(1);
    assertThat(service.getUpToDateEnumeratorQuestions().get(0))
        .isEqualTo(enumeratorQuestion.getQuestionDefinition());
  }

  @Test
  public void getQuestionDefinition_byId() throws QuestionNotFoundException {
    // The question bank initializes these in the active version.
    QuestionModel nameQuestion = testQuestionBank.nameApplicantName();
    long questionId = nameQuestion.id;

    var service = new ReadOnlyCurrentQuestionServiceImpl(versionRepository);

    assertThat(service.getQuestionDefinition(questionId))
        .isEqualTo(nameQuestion.getQuestionDefinition());
  }
}
