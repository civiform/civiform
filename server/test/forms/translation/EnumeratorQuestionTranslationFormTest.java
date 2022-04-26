package forms.translation;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Locale;
import java.util.Optional;
import org.junit.Test;
import services.LocalizedStrings;
import services.question.types.EnumeratorQuestionDefinition;
import services.question.types.QuestionDefinition;

public class EnumeratorQuestionTranslationFormTest {

  @Test
  public void buildsQuestion_newLocale_savesUpdates() throws Exception {
    QuestionDefinition question =
        new EnumeratorQuestionDefinition(
            "test",
            Optional.empty(),
            "desc",
            LocalizedStrings.withDefaultValue("existing"),
            LocalizedStrings.withDefaultValue("existing"),
            LocalizedStrings.withDefaultValue("existing"));

    EnumeratorQuestionTranslationForm form = new EnumeratorQuestionTranslationForm();
    form.setQuestionText("Canadian question text");
    form.setEntityType("Canadian English");

    EnumeratorQuestionDefinition updated =
        (EnumeratorQuestionDefinition) form.builderWithUpdates(question, Locale.CANADA).build();

    assertThat(updated.getEntityType())
        .isEqualTo(LocalizedStrings.of(Locale.US, "existing", Locale.CANADA, "Canadian English"));
    assertThat(updated.getQuestionText())
        .isEqualTo(
            LocalizedStrings.of(Locale.US, "existing", Locale.CANADA, "Canadian question text"));
  }

  @Test
  public void buildsQuestion_existingLocale_savesUpdates() throws Exception {
    QuestionDefinition question =
        new EnumeratorQuestionDefinition(
            "test",
            Optional.empty(),
            "desc",
            LocalizedStrings.of(Locale.FRANCE, "existing"),
            LocalizedStrings.of(Locale.FRANCE, "existing"),
            LocalizedStrings.of(Locale.FRANCE, "existing"));

    EnumeratorQuestionTranslationForm form = new EnumeratorQuestionTranslationForm();
    form.setQuestionText("new text translation");
    form.setQuestionHelpText("new help translation");
    form.setEntityType("new entity translation");

    EnumeratorQuestionDefinition updated =
        (EnumeratorQuestionDefinition) form.builderWithUpdates(question, Locale.FRANCE).build();

    assertThat(updated.getEntityType())
        .isEqualTo(LocalizedStrings.of(Locale.FRANCE, "new entity translation"));
    assertThat(updated.getQuestionText())
        .isEqualTo(LocalizedStrings.of(Locale.FRANCE, "new text translation"));
    assertThat(updated.getQuestionHelpText())
        .isEqualTo(LocalizedStrings.of(Locale.FRANCE, "new help translation"));
  }
}
