package services.question.types;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Locale;
import org.junit.Test;
import support.TestQuestionBank;

public class QuestionDefinitionBuilderTest {

  private static final TestQuestionBank QUESTION_BANK = new TestQuestionBank(false);

  @Test
  public void builder_addsNewTranslations() throws Exception {
    QuestionDefinition question = QUESTION_BANK.applicantName().getQuestionDefinition();

    QuestionDefinitionBuilder builder = new QuestionDefinitionBuilder(question);
    builder.updateQuestionText(Locale.FRENCH, "french");
    builder.updateQuestionHelpText(Locale.FRENCH, "french help");
    question = builder.build();

    assertThat(question.getQuestionText(Locale.FRENCH)).isEqualTo("french");
    assertThat(question.getQuestionHelpText(Locale.FRENCH)).isEqualTo("french help");
  }

  @Test
  public void builder_overwritesExistingTranslation() throws Exception {
    QuestionDefinition question = QUESTION_BANK.applicantName().getQuestionDefinition();

    QuestionDefinitionBuilder builder = new QuestionDefinitionBuilder(question);
    builder.updateQuestionText(Locale.US, "new text");
    builder.updateQuestionHelpText(Locale.US, "new help text");
    question = builder.build();

    assertThat(question.getQuestionText(Locale.US)).isEqualTo("new text");
    assertThat(question.getQuestionHelpText(Locale.US)).isEqualTo("new help text");
  }
}
