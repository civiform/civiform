package services.question.types;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

import java.util.Locale;
import java.util.Optional;
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
        .isEqualTo(
            LocalizedQuestionOption.create(
                /* id= */ 1L,
                /* order= */ 1L,
                /* adminName= */ "default admin",
                /* optionText= */ "default",
                /* displayInAnswerOptions= */ Optional.empty(),
                /* locale= */ Locale.US));
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
                /* id= */ 123L,
                /* order= */ 1L,
                /* adminName= */ "test admin",
                /* optionText= */ "test",
                /* displayInAnswerOptions= */ Optional.empty(),
                /* locale= */ LocalizedStrings.DEFAULT_LOCALE));
  }

  @Test
  public void create_withDisplayOrder_preservesDisplayOrderWhenLocalized() {
    // This test verifies that Yes/No question options display in the same order
    // after create, edit and in UI
    QuestionOption yesOption =
        QuestionOption.create(
            /* id= */ 1L, // YES = 1 (true)
            /* displayOrder= */ 0L, // Shows first
            /* adminName= */ "yes",
            /* optionText= */ LocalizedStrings.of(Locale.US, "Yes"),
            /* displayInAnswerOptions= */ Optional.of(true));

    QuestionOption noOption =
        QuestionOption.create(
            /* id= */ 0L, // NO = 0 (false)
            /* displayOrder= */ 1L, // Shows second
            /* adminName= */ "no",
            /* optionText= */ LocalizedStrings.of(Locale.US, "No"),
            /* displayInAnswerOptions= */ Optional.of(true));

    QuestionOption notSureOption =
        QuestionOption.create(
            /* id= */ 2L, // NOT_SURE = 2
            /* displayOrder= */ 2L, // Shows third
            /* adminName= */ "not-sure",
            /* optionText= */ LocalizedStrings.of(Locale.US, "Not sure"),
            /* displayInAnswerOptions= */ Optional.of(true));

    QuestionOption maybeOption =
        QuestionOption.create(
            /* id= */ 3L, // MAYBE = 3
            /* displayOrder= */ 3L, // Shows fourth
            /* adminName= */ "maybe",
            /* optionText= */ LocalizedStrings.of(Locale.US, "Maybe"),
            /* displayInAnswerOptions= */ Optional.of(true));

    // Verify that displayOrder is preserved (not replaced with empty)
    assertThat(yesOption.displayOrder()).isEqualTo(OptionalLong.of(0L));
    assertThat(noOption.displayOrder()).isEqualTo(OptionalLong.of(1L));
    assertThat(notSureOption.displayOrder()).isEqualTo(OptionalLong.of(2L));
    assertThat(maybeOption.displayOrder()).isEqualTo(OptionalLong.of(3L));

    // Verify that when localized, the displayOrder is used (not the id)
    LocalizedQuestionOption localizedYes = yesOption.localize(Locale.US);
    LocalizedQuestionOption localizedNo = noOption.localize(Locale.US);
    LocalizedQuestionOption localizedNotSure = notSureOption.localize(Locale.US);
    LocalizedQuestionOption localizedMaybe = maybeOption.localize(Locale.US);

    // Critical assertion: displayOrder should be used for order, not id
    assertThat(localizedYes.order()).isEqualTo(0L); // displayOrder=0, not id=1
    assertThat(localizedNo.order()).isEqualTo(1L); // displayOrder=1, not id=0
    assertThat(localizedNotSure.order()).isEqualTo(2L); // displayOrder=2, same as id=2
    assertThat(localizedMaybe.order()).isEqualTo(3L); // displayOrder=3, same as id=3
  }
}
