package services.settings;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigException;
import java.util.Locale;
import java.util.Optional;
import java.util.function.Function;

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
    return getConfigVal(config::getBoolean, getHoconName(settingDescription));
  }

  protected Optional<Boolean> getBool(String variableName) {
    return getConfigVal(config::getBoolean, getHoconName(variableName));
  }

  protected Optional<String> getString(SettingDescription settingDescription) {
    return getConfigVal(config::getString, getHoconName(settingDescription));
  }

  protected Optional<String> getString(String variableName) {
    return getConfigVal(config::getString, getHoconName(variableName));
  }

  protected Optional<Integer> getInt(SettingDescription settingDescription) {
    return getConfigVal(config::getInt, getHoconName(settingDescription));
  }

  protected Optional<Integer> getInt(String variableName) {
    return getConfigVal(config::getInt, getHoconName(variableName));
  }

  protected Optional<ImmutableList<String>> getListOfStrings(
      SettingDescription settingDescription) {
    return getConfigVal(
        name -> ImmutableList.copyOf(config.getStringList(name)), getHoconName(settingDescription));
  }

  protected Optional<ImmutableList<String>> getListOfStrings(String variableName) {
    return getConfigVal(
        name -> ImmutableList.copyOf(config.getStringList(name)), getHoconName(variableName));
  }

  private <T> Optional<T> getConfigVal(Function<String, T> configGetter, String hoconName) {
    try {
      return Optional.of(configGetter.apply(hoconName));
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
