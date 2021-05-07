package services.question.types;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

import com.google.common.collect.ImmutableMap;
import java.util.Locale;
import org.junit.Test;
import services.question.QuestionOption;

public class QuestionOptionTest {

  @Test
  public void localize_unsupportedLocale_throws() {
    QuestionOption questionOption =
        QuestionOption.builder()
            .setId(1L)
            .setOptionText(ImmutableMap.of(Locale.US, "option 1"))
            .build();

    Throwable thrown = catchThrowable(() -> questionOption.localize(Locale.CANADA));

    assertThat(thrown).hasMessageContaining("not supported for question option");
  }
}
