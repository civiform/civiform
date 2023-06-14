package services.settings;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSortedMap;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigException;
import java.util.Locale;
import java.util.Optional;
import java.util.function.Function;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import play.mvc.Http;

/** Provides behavior for {@link SettingsManifest}. */
public abstract class AbstractSettingsManifest {
  public static final String FEATURE_FLAG_SETTING_SECTION_NAME = "Feature Flags";

  private static final Logger LOGGER = LoggerFactory.getLogger(AbstractSettingsManifest.class);

  private final Config config;

  public AbstractSettingsManifest(Config config) {
    this.config = checkNotNull(config);
  }

  /**
   * Returns a map containing the names of the settings in the "Feature Flags" section mapped to
   * their current values.
   */
  public ImmutableSortedMap<String, Boolean> getAllFeatureFlagsSorted(Http.Request request) {
    ImmutableSortedMap.Builder<String, Boolean> map = ImmutableSortedMap.naturalOrder();

    for (SettingDescription settingDescription : getAllFeatureFlagsSettingDescriptions()) {
      String name = settingDescription.variableName();
      map.put(name, getBool(name, request));
    }

    return map.build();
  }

  private ImmutableList<SettingDescription> getAllFeatureFlagsSettingDescriptions() {
    if (!getSections().containsKey(FEATURE_FLAG_SETTING_SECTION_NAME)) {
      return ImmutableList.of();
    }

    return getSettingDescriptions(getSections().get(FEATURE_FLAG_SETTING_SECTION_NAME));
  }

  private ImmutableList<SettingDescription> getSettingDescriptions(SettingsSection section) {
    ImmutableList.Builder<SettingDescription> result = ImmutableList.builder();

    result.addAll(section.settings());
    for (SettingsSection subsection : section.subsections()) {
      result.addAll(getSettingDescriptions(subsection));
    }

    return result.build();
  }

  /** True if the "FEATURE_FLAG_OVERRIDES_ENABLED" config value is present and true. */
  public boolean overridesEnabled() {
    return getBool("FEATURE_FLAG_OVERRIDES_ENABLED");
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

  /**
   * Gets the config value for the given setting name. If overrides are enabled and the request
   * contains an override for the setting, returns that value, otherwise uses the value from the
   * application config.
   */
  public boolean getBool(String settingName, Http.Request request) {
    Boolean configValue = getBool(settingName);

    if (!overridesEnabled()) {
      return configValue;
    }

    Optional<Boolean> sessionValue = request.session().get(settingName).map(Boolean::parseBoolean);
    if (sessionValue.isPresent()) {
      LOGGER.warn("Returning override ({}) for feature flag: {}", sessionValue.get(), settingName);
      return sessionValue.get();
    }

    return configValue;
  }

  protected Optional<Boolean> getBool(SettingDescription settingDescription) {
    return getConfigVal(config::getBoolean, getHoconName(settingDescription));
  }

  public boolean getBool(String variableName) {
    return getConfigVal(config::getBoolean, getHoconName(variableName)).orElse(false);
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
