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

  @Test
  public void supportsOptionScores_trueForCheckboxDropdownRadio() {
    assertThat(QuestionType.supportsOptionScores(QuestionType.CHECKBOX)).isTrue();
    assertThat(QuestionType.supportsOptionScores(QuestionType.DROPDOWN)).isTrue();
    assertThat(QuestionType.supportsOptionScores(QuestionType.RADIO_BUTTON)).isTrue();
  }

  @Test
  public void supportsOptionScores_falseForYesNo() {
    // Yes/No is a multi-option type but must never support scoring.
    assertThat(QuestionType.YES_NO.isMultiOptionType()).isTrue();
    assertThat(QuestionType.supportsOptionScores(QuestionType.YES_NO)).isFalse();
  }

  @Test
  public void supportsOptionScores_falseForAllOtherTypes() {
    for (QuestionType type : QuestionType.values()) {
      if (type == QuestionType.CHECKBOX
          || type == QuestionType.DROPDOWN
          || type == QuestionType.RADIO_BUTTON) {
        continue;
      }
      assertThat(QuestionType.supportsOptionScores(type)).isFalse();
    }
  }
}
