package services.settings;

import com.google.auto.value.AutoValue;
import com.google.common.collect.ImmutableList;

/**
 * Holds a logical group of server settings. May contain subsections. Contents of the section are
 * provided from server/conf/env_var_docs.json
 */
@AutoValue
public abstract class SettingsSection {

  public static SettingsSection create(
      String sectionName,
      String sectionDescription,
      ImmutableList<SettingsSection> subsections,
      ImmutableList<SettingDescription> settings) {
    return new AutoValue_SettingsSection(sectionName, sectionDescription, subsections, settings);
  }

  /** The name of this section, usually in title case. */
  public abstract String sectionName();

  /** A sentence or two describing the section. */
  public abstract String sectionDescription();

  /** This section's subsections. */
  public abstract ImmutableList<SettingsSection> subsections();

  /** Settings in this section. */
  public abstract ImmutableList<SettingDescription> settings();

  /** True if any of this section's or this section's subsections' settings should be displayed. */
  public boolean shouldDisplay() {
    return settings().stream().anyMatch(SettingDescription::shouldDisplay)
        || subsections().stream().anyMatch(SettingsSection::shouldDisplay);
  }
}
