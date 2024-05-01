package services.settings;

import static org.assertj.core.api.Assertions.assertThat;

import com.google.common.collect.ImmutableList;
import org.junit.Test;

public class SettingsSectionTest {

  @Test
  public void shouldDisplay() {
    var hiddenSetting =
        SettingDescription.create("HIDDEN_NAME", "", true, SettingType.STRING, SettingMode.HIDDEN);
    var readableSetting =
        SettingDescription.create(
            "READABLE_NAME", "", true, SettingType.STRING, SettingMode.ADMIN_READABLE);

    assertThat(
            SettingsSection.create(
                    "",
                    "",
                    ImmutableList.of(
                        SettingsSection.create(
                            "", "", ImmutableList.of(), ImmutableList.of(readableSetting))),
                    ImmutableList.of(hiddenSetting))
                .shouldDisplay())
        .isTrue();
    assertThat(
            SettingsSection.create(
                    "",
                    "",
                    ImmutableList.of(
                        SettingsSection.create(
                            "", "", ImmutableList.of(), ImmutableList.of(hiddenSetting))),
                    ImmutableList.of(hiddenSetting))
                .shouldDisplay())
        .isFalse();
    assertThat(
            SettingsSection.create(
                    "",
                    "",
                    ImmutableList.of(
                        SettingsSection.create(
                            "", "", ImmutableList.of(), ImmutableList.of(hiddenSetting))),
                    ImmutableList.of(readableSetting))
                .shouldDisplay())
        .isTrue();
  }
}
