package services.settings;

import static org.assertj.core.api.Assertions.assertThat;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import org.junit.Test;

public class SettingsManifestTest {

  private ImmutableMap<String, SettingsSection> sections = ImmutableMap.of(
    "TEST_SECTION", SettingsSection.create("Test Section", "Fake section for testing.",
      ImmutableList.of(SettingsSection.create("Test Subsection", "Fake subsection for testing", ImmutableList.of(), ImmutableList.of(
        SettingDescription.create("Subsection variable", "Fake subsection variable for testing", SettingType.STRING)
      ))),
      ImmutableList.of(
        SettingDescription.create("String variable", "Fake string variable for testing", SettingType.STRING)
      )
    )
  );
  private SettingsManifest testManifest = new SettingsManifest(sections);

  @Test
  public void gettingSections() {
    assertThat(testManifest.getSections()).isEqualTo(sections);
  }
}
