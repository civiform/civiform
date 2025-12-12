package services.settings;

import static com.google.common.base.Preconditions.checkNotNull;
import static services.settings.SettingMode.ADMIN_WRITEABLE;

import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSortedMap;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigException;
import java.util.Locale;
import java.util.Optional;
import java.util.function.Function;
import models.SettingsGroupModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import play.cache.SyncCacheApi;

/** Provides behavior for {@link SettingsManifest}. */
public abstract class AbstractSettingsManifest {
  public static final String FEATURE_FLAG_SETTING_SECTION_NAME = "Feature Flags";

  private final Config config;
  private final SyncCacheApi settingsCache;
  private static final Logger logger = LoggerFactory.getLogger("SettingsManifest");

  public AbstractSettingsManifest(Config config, SyncCacheApi settingsCache) {
    this.config = checkNotNull(config);
    this.settingsCache = checkNotNull(settingsCache);
  }

  /**
   * Returns a map containing the names of the settings in the "Feature Flags" section mapped to
   * their current values.
   */
  public ImmutableSortedMap<String, Boolean> getAllFeatureFlagsSorted() {
    ImmutableSortedMap.Builder<String, Boolean> map = ImmutableSortedMap.naturalOrder();

    for (SettingDescription settingDescription : getAllFeatureFlagsSettingDescriptions()) {
      String name = settingDescription.variableName();
      map.put(name, getBool(name));
    }

    return map.build();
  }

  public ImmutableList<SettingDescription> getAllSettingDescriptions() {
    return getSections().values().stream()
        .flatMap(section -> getSettingDescriptions(section).stream())
        .collect(ImmutableList.toImmutableList());
  }

  protected ImmutableList<SettingDescription> getAllAdminWriteableSettingDescriptions() {
    return getSections().values().stream()
        .flatMap(section -> getSettingDescriptions(section).stream())
        .filter(settingDescription -> settingDescription.settingMode().equals(ADMIN_WRITEABLE))
        .collect(ImmutableList.toImmutableList());
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

  public abstract ImmutableMap<String, SettingsSection> getSections();

  /**
   * Retrieve a string representation of the setting suitable for display in the UI from the request
   * attributes or HOCON config.
   */
  public Optional<String> getSettingDisplayValue(SettingDescription settingDescription) {
    String variableName = settingDescription.variableName();

    return switch (settingDescription.settingType()) {
      case BOOLEAN ->
          Optional.of(String.valueOf(getBool(settingDescription)).toUpperCase(Locale.ROOT));
      case INT -> getInt(variableName).map(String::valueOf);
      case LIST_OF_STRINGS ->
          getListOfStrings(variableName).map(list -> String.join(", ", list));
      case ENUM, STRING -> getString(variableName).map(String::valueOf);
    };
  }

  /**
   * Retrieve a string representation of the setting suitable for JSON serialization from HOCON
   * config.
   */
  public Optional<String> getSettingSerializationValue(SettingDescription settingDescription) {
    String variableName = settingDescription.variableName();

    return switch (settingDescription.settingType()) {
      case BOOLEAN -> getBool(settingDescription).map(String::valueOf);
      case INT -> getInt(variableName).map(String::valueOf);
      case LIST_OF_STRINGS ->
          getListOfStrings(variableName).map(list -> String.join(",", list));
      case ENUM, STRING -> getString(variableName).map(String::valueOf);
    };
  }

  public boolean getBool(String variableName) {
    Optional<Optional<SettingsGroupModel>> optionalCacheEntry =
        settingsCache.get("current-settings");

    if (optionalCacheEntry.isEmpty()) {
      logNoCurrentSettings(variableName);
      return getConfigVal(config::getBoolean, getHoconName(variableName)).orElse(false);
    }

    Optional<SettingsGroupModel> optionalSettingsGroupModel = optionalCacheEntry.get();

    if (optionalSettingsGroupModel.isEmpty()) {
      logValueNotInCachedSettings(variableName);
      return getConfigVal(config::getBoolean, getHoconName(variableName)).orElse(false);
    }

    String settingValue = optionalSettingsGroupModel.get().getSettings().get(variableName);

    if (settingValue == null) {
      return getConfigVal(config::getBoolean, getHoconName(variableName)).orElse(false);
    }

    return Boolean.parseBoolean(settingValue);
  }

  private Optional<Boolean> getBool(SettingDescription settingDescription) {
    return Optional.of(getBool(getHoconName(settingDescription.variableName())));
  }

  protected Optional<String> getString(String variableName) {
    Optional<Optional<SettingsGroupModel>> optionalCacheEntry =
        settingsCache.get("current-settings");

    if (optionalCacheEntry.isEmpty()) {
      logNoCurrentSettings(variableName);
      var z = getConfigVal(config::getString, getHoconName(variableName));
      logger.error("aaaa: {}", z);
      return z;
    }

    Optional<SettingsGroupModel> optionalSettingsGroupModel = optionalCacheEntry.get();

    if (optionalSettingsGroupModel.isEmpty()) {
      logValueNotInCachedSettings(variableName);
      return getConfigVal(config::getString, getHoconName(variableName));
    }

    String settingValue = optionalSettingsGroupModel.get().getSettings().get(variableName);

    if (settingValue == null) {
      return getConfigVal(config::getString, getHoconName(variableName));
    }

    return Optional.of(settingValue);
  }

  protected Optional<Integer> getInt(String variableName) {
    Optional<Optional<SettingsGroupModel>> optionalCacheEntry =
        settingsCache.get("current-settings");

    if (optionalCacheEntry.isEmpty()) {
      logNoCurrentSettings(variableName);
      return getConfigVal(config::getInt, getHoconName(variableName));
    }

    Optional<SettingsGroupModel> optionalSettingsGroupModel = optionalCacheEntry.get();

    if (optionalSettingsGroupModel.isEmpty()) {
      logValueNotInCachedSettings(variableName);
      return getConfigVal(config::getInt, getHoconName(variableName));
    }

    String settingValue = optionalSettingsGroupModel.get().getSettings().get(variableName);

    if (settingValue == null) {
      return getConfigVal(config::getInt, getHoconName(variableName));
    }

    return Optional.of(Integer.parseInt(settingValue));
  }

  protected Optional<ImmutableList<String>> getListOfStrings(String variableName) {
    Optional<Optional<SettingsGroupModel>> optionalCacheEntry =
        settingsCache.get("current-settings");

    if (optionalCacheEntry.isEmpty()) {
      logNoCurrentSettings(variableName);
      return getConfigVal(
          name -> ImmutableList.copyOf(config.getStringList(name)), getHoconName(variableName));
    }

    Optional<SettingsGroupModel> optionalSettingsGroupModel = optionalCacheEntry.get();

    if (optionalSettingsGroupModel.isEmpty()) {
      logValueNotInCachedSettings(variableName);
      return getConfigVal(
          name -> ImmutableList.copyOf(config.getStringList(name)), getHoconName(variableName));
    }

    String settingValue = optionalSettingsGroupModel.get().getSettings().get(variableName);

    if (settingValue == null) {
      return getConfigVal(
          name -> ImmutableList.copyOf(config.getStringList(name)), getHoconName(variableName));
    }

    return Optional.of(ImmutableList.copyOf(Splitter.on(",").split(settingValue)));
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

  private void logNoCurrentSettings(String variableName) {
    logger.warn("The settingsCache does not have 'current-settings'. No cache entry for: {}", variableName);
  }

  private void logValueNotInCachedSettings(String variableName) {
    logger.warn("Settings not found in model when looking up value for {}", variableName);
  }
}
