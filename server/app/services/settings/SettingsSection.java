package services.settings;

import com.google.auto.value.AutoValue;
import com.google.common.collect.ImmutableList;

@AutoValue
public abstract class SettingsSection {

  public static SettingsSection create(String sectionName, String sectionDescription, ImmutableList<SettingsSection> subsections, ImmutableList<SettingDescription> settings) {
    return new AutoValue_SettingsSection(sectionName, sectionDescription, subsections, settings);
  }

  public abstract String sectionName();

  public abstract String sectionDescription();

  public abstract ImmutableList<SettingsSection> subsections();

  public abstract ImmutableList<SettingDescription> settings();
}
