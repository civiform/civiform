package services.question.types;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.Test;
import services.question.exceptions.InvalidQuestionTypeException;

public class QuestionTypeTest {

  @Test
  public void label_regular() {
    assertThat(QuestionType.TEXT.getLabel()).isEqualTo("Text");
  }

  @Test
  public void label_irregular() {
    assertThat(QuestionType.PHONE.getLabel()).isEqualTo("Phone Number");
  }

  @Test
  public void fromLabel_regular() throws InvalidQuestionTypeException {
    assertThat(QuestionType.fromLabel("Text")).isEqualTo(QuestionType.TEXT);
  }

  @Test
  public void fromLabel_irregular() throws InvalidQuestionTypeException {
    assertThat(QuestionType.fromLabel("Phone Number")).isEqualTo(QuestionType.PHONE);
  }

  @Test
  public void of_regular() throws InvalidQuestionTypeException {
    assertThat(QuestionType.of("Text")).isEqualTo(QuestionType.TEXT);
  }

  @Test
  public void of_irregular() throws InvalidQuestionTypeException {
    assertThat(QuestionType.of("Phone")).isEqualTo(QuestionType.PHONE);
  }

  @Test
  public void of_lowercase() throws InvalidQuestionTypeException {
    assertThat(QuestionType.of("text")).isEqualTo(QuestionType.TEXT);
  }
}
