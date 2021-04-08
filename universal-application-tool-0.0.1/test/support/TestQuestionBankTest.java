package support;

import static org.assertj.core.api.Assertions.assertThat;

import models.Question;
import org.junit.Before;
import org.junit.Test;

public class TestQuestionBankTest {

  @Before
  public void reset() {
    TestQuestionBank.reset();
  }

  @Test
  public void withoutDatabase_canGetQuestion() {
    Question question = TestQuestionBank.applicantAddress();

    assertThat(question.id).isEqualTo(1L);
  }

  @Test
  public void withoutDatabase_setsId() {
    TestQuestionBank.applicantAddress();
    Question question = TestQuestionBank.applicantName();

    assertThat(question.id).isEqualTo(2L);
  }
}
