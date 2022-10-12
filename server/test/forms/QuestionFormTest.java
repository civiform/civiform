package forms;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.Test;
import services.question.types.QuestionType;

public class QuestionFormTest {

  private static class TestQuestionForm extends QuestionForm {

    @Override
    public QuestionType getQuestionType() {
      return null;
    }
  }

  @Test
  public void getRedirectUrl_relativeUrl() {
    TestQuestionForm form = new TestQuestionForm();
    form.setRedirectUrl("/some/relative/url?queryparam=a#hashparam=b");
    assertThat(form.getRedirectUrl()).isEqualTo("/some/relative/url?queryparam=a#hashparam=b");
  }

  @Test
  public void getRedirectUrl_null() {
    TestQuestionForm form = new TestQuestionForm();
    assertThat(form.getRedirectUrl()).isEqualTo("");
  }

  @Test
  public void getRedirectUrl_empty() {
    TestQuestionForm form = new TestQuestionForm();
    form.setRedirectUrl("");
    assertThat(form.getRedirectUrl()).isEqualTo("");
  }

  @Test
  public void getRedirectUrl_absoluteUrl_throws() {
    TestQuestionForm form = new TestQuestionForm();
    form.setRedirectUrl("https://www.example.com");
    assertThatThrownBy(() -> form.getRedirectUrl()).hasMessageContaining("Invalid absolute URL.");
  }

  @Test
  public void getRedirectUrl_otherSchemeUrl_throws() {
    TestQuestionForm form = new TestQuestionForm();
    form.setRedirectUrl("file://foo/bar");
    assertThatThrownBy(() -> form.getRedirectUrl()).hasMessageContaining("Invalid absolute URL.");
  }
}
