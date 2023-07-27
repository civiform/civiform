package support;

import static org.assertj.core.api.Assertions.assertThat;

import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import models.Question;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import services.question.types.QuestionType;

@RunWith(JUnitParamsRunner.class)
public class TestQuestionBankTest {

  private static final TestQuestionBank testQuestionBank = new TestQuestionBank(false);

  @Before
  public void reset() {
    testQuestionBank.reset();
  }

  @Test
  public void withoutDatabase_canGetQuestion() {
    Question question = testQuestionBank.applicantAddress();

    assertThat(question.id).isEqualTo(1L);
  }

  @Test
  public void withoutDatabase_setsId() {
    testQuestionBank.applicantAddress();
    Question question = testQuestionBank.applicantName();

    assertThat(question.id).isEqualTo(2L);
  }

  @Test
  @Parameters(source = QuestionType.class)
  public void getSampleQuestionForAllTypes_hasASampleForEachType(QuestionType questionType) {
    // A null question type is not allowed to be created and won't show in the list
    if (questionType == QuestionType.NULL_QUESTION) {
      return;
    }

    assertThat(testQuestionBank.getSampleQuestionsForAllTypes().get(questionType)).isNotNull();
  }
}
