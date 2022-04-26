package forms.translation;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Locale;
import java.util.Optional;
import org.junit.Test;
import services.LocalizedStrings;
import services.question.types.QuestionDefinition;
import services.question.types.TextQuestionDefinition;
import support.TestQuestionBank;

public class QuestionTranslationFormTest {

  private final TestQuestionBank questionBank = new TestQuestionBank(false);

  @Test
  public void builder_updatesExistingTextTranslations() throws Exception {
    QuestionDefinition question = questionBank.applicantName().getQuestionDefinition();

    QuestionTranslationForm form = new QuestionTranslationForm();
    form.setQuestionText("new text");
    form.setQuestionHelpText("new help text");

    QuestionDefinition updated =
        form.builderWithUpdates(question, LocalizedStrings.DEFAULT_LOCALE).build();

    assertThat(updated.getQuestionText().getDefault()).isEqualTo("new text");
    assertThat(updated.getQuestionHelpText().getDefault()).isEqualTo("new help text");
  }

  @Test
  public void builder_addsTranslationsForNewLocale() throws Exception {
    QuestionDefinition question = questionBank.applicantName().getQuestionDefinition();

    QuestionTranslationForm form = new QuestionTranslationForm();
    form.setQuestionText("new locale");
    form.setQuestionHelpText("new help locale");

    QuestionDefinition updated = form.builderWithUpdates(question, Locale.CANADA).build();

    assertThat(updated.getQuestionText().get(Locale.CANADA)).isEqualTo("new locale");
    assertThat(updated.getQuestionHelpText().get(Locale.CANADA)).isEqualTo("new help locale");
  }

  @Test
  public void builder_noHelpText_onlyUpdatesQuestionText() throws Exception {
    QuestionDefinition question =
        new TextQuestionDefinition(
            "test",
            Optional.empty(),
            "test",
            LocalizedStrings.withDefaultValue("question?"),
            LocalizedStrings.empty());

    QuestionTranslationForm form = new QuestionTranslationForm();
    form.setQuestionText("new locale");
    form.setQuestionHelpText("");

    QuestionDefinition updated = form.builderWithUpdates(question, Locale.CANADA).build();

    assertThat(updated.getQuestionText().get(Locale.CANADA)).isEqualTo("new locale");
    assertThat(updated.getQuestionHelpText()).isEqualTo(LocalizedStrings.empty());
  }
}
