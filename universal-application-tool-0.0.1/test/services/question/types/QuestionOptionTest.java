package services.question.types;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

import java.util.Locale;
import org.junit.Test;
import services.LocalizedStrings;
import services.question.QuestionOption;

public class QuestionOptionTest {

  @Test
  public void localize_unsupportedLocale_throws() {
    QuestionOption questionOption =
        QuestionOption.create(1L, LocalizedStrings.of(Locale.US, "option 1"));

    Throwable thrown = catchThrowable(() -> questionOption.localize(Locale.CANADA));

    assertThat(thrown).hasMessageContaining("not supported for question option");
  }

  @Test
  public void builder_addsNewLocale() throws Exception {
    QuestionOption.Builder builder = QuestionOption.builder().setId(1L);

    builder.updateOptionText(Locale.CHINESE, "new locale!");

    assertThat(builder.build().optionText().get(Locale.CHINESE)).isEqualTo("new locale!");
  }

  @Test
  public void builder_updatesExistingLocale() throws Exception {
    QuestionOption.Builder builder = QuestionOption.builder().setId(1L);
    Locale locale = Locale.ITALY;

    builder.updateOptionText(locale, "new text for existing locale");

    assertThat(builder.build().optionText().get(locale)).isEqualTo("new text for existing locale");
  }
}
