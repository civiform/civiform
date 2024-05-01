package services.question.types;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

import java.util.Locale;
import java.util.OptionalLong;
import org.junit.Test;
import services.LocalizedStrings;
import services.question.LocalizedQuestionOption;
import services.question.QuestionOption;

public class QuestionOptionTest {

  @Test
  public void localize_unsupportedLocale_throws() {
    QuestionOption questionOption =
        QuestionOption.create(1L, "opt1", LocalizedStrings.of(Locale.US, "option 1"));

    Throwable thrown = catchThrowable(() -> questionOption.localize(Locale.CANADA));

    assertThat(thrown).hasMessageContaining("not supported for question option");
  }

  @Test
  public void localizeOrDefault_returnsDefaultForUnsupportedLocale() {
    QuestionOption option =
        QuestionOption.create(1L, "default admin", LocalizedStrings.of(Locale.US, "default"));

    assertThat(option.localizeOrDefault(Locale.CHINESE))
        .isEqualTo(LocalizedQuestionOption.create(1L, 1L, "default admin", "default", Locale.US));
  }

  @Test
  public void localize_localizes() {
    QuestionOption option =
        QuestionOption.builder()
            .setId(123L)
            .setAdminName("test admin")
            .setOptionText(LocalizedStrings.withDefaultValue("test"))
            .setDisplayOrder(OptionalLong.of(1L))
            .build();

    assertThat(option.localize(LocalizedStrings.DEFAULT_LOCALE))
        .isEqualTo(
            LocalizedQuestionOption.create(
                123L, 1L, "test admin", "test", LocalizedStrings.DEFAULT_LOCALE));
  }
}
