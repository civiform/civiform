package services.settings;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.Test;

public class SettingDescriptionTest {

  @Test
  public void shouldDisplay() {
    assertThat(
            SettingDescription.create(
                    "HIDDEN_NAME", "", true, SettingType.STRING, SettingMode.HIDDEN)
                .shouldDisplay())
        .isFalse();
    assertThat(
            SettingDescription.create(
                    "READABLE_NAME", "", true, SettingType.STRING, SettingMode.ADMIN_READABLE)
                .shouldDisplay())
        .isTrue();
  }
}
