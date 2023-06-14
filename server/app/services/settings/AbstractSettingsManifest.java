package services.settings;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigException;
import featureflags.FeatureFlag;
import java.util.Locale;
import java.util.Optional;
import java.util.function.Function;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import play.mvc.Http;

/** Provides behavior for {@link SettingsManifest}. */
public abstract class AbstractSettingsManifest {

  private static final Logger LOGGER = LoggerFactory.getLogger(AbstractSettingsManifest.class);

  private final Config config;

  public AbstractSettingsManifest(Config config) {
    this.config = checkNotNull(config);
  }

  public boolean getBool(String flagName, Http.Request request) {
    Boolean configValue = getBool(flagName);

    if (!overridesEnabled()) {
      return configValue;
    }

    Optional<Boolean> sessionValue = request.session().get(flagName).map(Boolean::parseBoolean);
    if (sessionValue.isPresent()) {
      LOGGER.warn("Returning override ({}) for feature flag: {}", sessionValue.get(), flagName);
      return sessionValue.get();
    }

    return configValue;
  }

  /** Returns the current setting for {@code flag} from {@link Config} if present. */
  public Optional<Boolean> getFlagEnabledFromConfig(FeatureFlag flag) {
    if (!config.hasPath(flag.toString())) {
      LOGGER.warn("Feature flag requested for unconfigured flag: {}", flag);
      return Optional.empty();
    }
    return Optional.of(config.getBoolean(flag.toString()));
  }

  public boolean overridesEnabled() {
    return config.hasPath(FeatureFlag.FEATURE_FLAG_OVERRIDES_ENABLED.toString())
        && config.getBoolean(FeatureFlag.FEATURE_FLAG_OVERRIDES_ENABLED.toString());
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

  protected boolean getBool(String variableName) {
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
