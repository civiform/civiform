package services.settings;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.typesafe.config.Config;
import java.util.Locale;
import javax.inject.Inject;

public abstract class AbstractSettingsManifest {

  private final Config config;

  @Inject
  public AbstractSettingsManifest(Config config) {
    this.config = checkNotNull(config);
  }

  public abstract ImmutableMap<String, SettingsSection> getSections();

  protected boolean getBool(SettingDescription settingDescription) {
    return config.getBoolean(getHoconName(settingDescription));
  }

  protected boolean getBool(String variableName) {
    return config.getBoolean(getHoconName(variableName));
  }

  protected String getString(SettingDescription settingDescription) {
    return config.getString(getHoconName(settingDescription));
  }

  protected String getString(String variableName) {
    return config.getString(getHoconName(variableName));
  }

  protected int getInt(SettingDescription settingDescription) {
    return config.getInt(getHoconName(settingDescription));
  }

  protected int getInt(String variableName) {
    return config.getInt(getHoconName(variableName));
  }

  protected ImmutableList<String> getListOfStrings(SettingDescription settingDescription) {
    return ImmutableList.copyOf(config.getStringList(getHoconName(settingDescription)));
  }

  protected ImmutableList<String> getListOfStrings(String variableName) {
    return ImmutableList.copyOf(config.getStringList(getHoconName(variableName)));
  }

  private static String getHoconName(String variableName) {
    return variableName.toLowerCase(Locale.ROOT);
  }

  private static String getHoconName(SettingDescription settingDescription) {
    return getHoconName(settingDescription.variableName());
  }
}
