package mapping.admin.questions;

import static org.assertj.core.api.Assertions.assertThat;

import com.google.common.collect.ImmutableList;
import forms.questions.YesNoQuestionForm;
import org.junit.Test;
import views.admin.questions.YesNoConfig;

public final class YesNoConfigMapperTest {

  @Test
  public void buildYesNoConfig_newQuestion_rendersDefaultsWithLabel() {
    YesNoConfig config = YesNoConfigMapper.buildYesNoConfig(new YesNoQuestionForm());

    assertThat(config.showLabel()).isTrue();
    assertThat(config.options()).hasSize(4);
    assertThat(config.options().stream().map(row -> row.adminName()))
        .containsExactly("yes", "no", "not-sure", "maybe");
    // The default set renders every option checked; "yes" and "no" are
    // required (checked and disabled, with an extra hidden input).
    assertThat(config.options().stream().allMatch(row -> row.checked())).isTrue();
    assertThat(config.options().get(0).required()).isTrue();
    assertThat(config.options().get(1).required()).isTrue();
    assertThat(config.options().get(2).required()).isFalse();
    assertThat(config.options().get(3).required()).isFalse();
    assertThat(config.options().get(0).ariaLabel()).isEqualTo("Admin ID: yes. Option text: Yes.");
  }

  @Test
  public void buildYesNoConfig_existingQuestion_usesFormOptionsWithoutLabel() {
    YesNoQuestionForm form = new YesNoQuestionForm();
    form.setOptionIds(ImmutableList.of(1L, 0L, 2L, 3L));
    form.setOptionAdminNames(ImmutableList.of("yes", "no", "not-sure", "maybe"));
    form.setOptions(ImmutableList.of("Yes", "No", "Not sure", "Maybe"));
    // Only "not-sure" is displayed in addition to the required options.
    form.setDisplayedOptionIds(ImmutableList.of(1L, 0L, 2L));

    YesNoConfig config = YesNoConfigMapper.buildYesNoConfig(form);

    assertThat(config.showLabel()).isFalse();
    assertThat(config.options()).hasSize(4);
    assertThat(config.options().get(2).checked()).isTrue();
    assertThat(config.options().get(3).checked()).isFalse();
    // Required options are always checked even if not listed as displayed.
    assertThat(config.options().get(0).checked()).isTrue();
    assertThat(config.options().get(1).checked()).isTrue();
  }
}
