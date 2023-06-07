package services.settings;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigException;
import java.util.Locale;
import java.util.Optional;

/** Provides behavior for {@link SettingsManifest}. */
public abstract class AbstractSettingsManifest {

  private final Config config;

  public AbstractSettingsManifest(Config config) {
    this.config = checkNotNull(config);
  }

  public abstract ImmutableMap<String, SettingsSection> getSections();

  public Optional<String> getSettingDisplayValue(SettingDescription settingDescription) {
    switch (settingDescription.settingType()) {
      case BOOLEAN:
        return getBool(settingDescription).map(String::valueOf).map(String::toUpperCase);
      case INT:
        return getInt(settingDescription).map(String::valueOf);
      case LIST_OF_STRINGS:
        return getListOfStrings(settingDescription).map(list -> String.join(", ", list));
      case ENUM:
      case STRING:
        return getString(settingDescription).map(String::valueOf);
      default:
        throw new IllegalStateException(
            "Unknown setting type: " + settingDescription.settingType());
    }
  }

  protected Optional<Boolean> getBool(SettingDescription settingDescription) {
    try {
      return Optional.of(config.getBoolean(getHoconName(settingDescription)));
    } catch (ConfigException.Missing e) {
      return Optional.empty();
    }
  }

  protected Optional<Boolean> getBool(String variableName) {
    try {
      return Optional.of(config.getBoolean(getHoconName(variableName)));
    } catch (ConfigException.Missing e) {
      return Optional.empty();
    }
  }

  protected Optional<String> getString(SettingDescription settingDescription) {
    try {
      return Optional.of(config.getString(getHoconName(settingDescription)));
    } catch (ConfigException.Missing e) {
      return Optional.empty();
    }
  }

  protected Optional<String> getString(String variableName) {
    try {
      return Optional.of(config.getString(getHoconName(variableName)));
    } catch (ConfigException.Missing e) {
      return Optional.empty();
    }
  }

  protected Optional<Integer> getInt(SettingDescription settingDescription) {
    try {
      return Optional.of(config.getInt(getHoconName(settingDescription)));
    } catch (ConfigException.Missing e) {
      return Optional.empty();
    }
  }

  protected Optional<Integer> getInt(String variableName) {
    try {
      return Optional.of(config.getInt(getHoconName(variableName)));
    } catch (ConfigException.Missing e) {
      return Optional.empty();
    }
  }

  protected Optional<ImmutableList<String>> getListOfStrings(
      SettingDescription settingDescription) {
    try {
      return Optional.of(
          ImmutableList.copyOf(config.getStringList(getHoconName(settingDescription))));
    } catch (ConfigException.Missing e) {
      return Optional.empty();
    }
  }

  protected Optional<ImmutableList<String>> getListOfStrings(String variableName) {
    try {
      return Optional.of(ImmutableList.copyOf(config.getStringList(getHoconName(variableName))));
    } catch (ConfigException.Missing e) {
      return Optional.empty();
    }
  }

  private static String getHoconName(String variableName) {
    return variableName.toLowerCase(Locale.ROOT);
  }

  private static String getHoconName(SettingDescription settingDescription) {
    return getHoconName(settingDescription.variableName());
  }
}
