package services.settings;

import static com.google.common.base.Preconditions.checkNotNull;
import static services.settings.SettingMode.ADMIN_WRITEABLE;
import static services.settings.SettingsService.CIVIFORM_SETTINGS_ATTRIBUTE_KEY;

import com.google.common.base.Splitter;
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

  private final Config config;
  private static final Logger LOGGER = LoggerFactory.getLogger("SettingsManifest");

  public AbstractSettingsManifest(Config config) {
    this.config = checkNotNull(config);
  }

  /**
   * Returns a map containing the names of the settings in the "Feature Flags" section mapped to
   * their current values.
   */
  public ImmutableSortedMap<String, Boolean> getAllFeatureFlagsSorted(Http.RequestHeader request) {
    ImmutableSortedMap.Builder<String, Boolean> map = ImmutableSortedMap.naturalOrder();

    for (SettingDescription settingDescription : getAllFeatureFlagsSettingDescriptions()) {
      String name = settingDescription.variableName();
      map.put(name, getBool(name, request));
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
  public Optional<String> getSettingDisplayValue(
      Http.RequestHeader request, SettingDescription settingDescription) {
    switch (settingDescription.settingType()) {
      case BOOLEAN:
        return Optional.of(
            String.valueOf(getBool(settingDescription, request)).toUpperCase(Locale.ROOT));
      case INT:
        return getInt(settingDescription, request).map(String::valueOf);
      case LIST_OF_STRINGS:
        return getListOfStrings(settingDescription, request).map(list -> String.join(", ", list));
      case ENUM:
      case STRING:
        return getString(settingDescription, request).map(String::valueOf);
      default:
        throw new IllegalStateException(
            "Unknown setting type: " + settingDescription.settingType());
    }
  }

  /**
   * Retrieve a string representation of the setting suitable for JSON serialization from HOCON
   * config.
   */
  public Optional<String> getSettingSerializationValue(SettingDescription settingDescription) {
    switch (settingDescription.settingType()) {
      case BOOLEAN:
        return getBool(settingDescription).map(String::valueOf);
      case INT:
        return getInt(settingDescription).map(String::valueOf);
      case LIST_OF_STRINGS:
        return getListOfStrings(settingDescription).map(list -> String.join(",", list));
      case ENUM:
      case STRING:
        return getString(settingDescription).map(String::valueOf);
      default:
        throw new IllegalStateException(
            "Unknown setting type: " + settingDescription.settingType());
    }
  }

  /**
   * Gets the config value for the given setting. If the setting is found in the stored writeable
   * settings, the value from the database is returned. Otherwise the value from the application
   * {@link Config} is used..
   */
  private boolean getBool(SettingDescription settingDescription, Http.RequestHeader request) {
    return getBool(settingDescription.variableName(), request);
  }

  protected boolean getBool(String variableName, Http.RequestHeader request) {
    if (!request.attrs().containsKey(CIVIFORM_SETTINGS_ATTRIBUTE_KEY)) {
      LOGGER.warn(
          String.format(
              "Settings not found on request when looking up value for %s", variableName));
      return getBool(variableName);
    }

    var writableSettings = request.attrs().get(CIVIFORM_SETTINGS_ATTRIBUTE_KEY);

    return writableSettings.containsKey(variableName)
        ? writableSettings.get(variableName).equals("true")
        : getBool(variableName);
  }

  protected Optional<Boolean> getBool(SettingDescription settingDescription) {
    return getConfigVal(config::getBoolean, getHoconName(settingDescription));
  }

  public boolean getBool(String variableName) {
    return getConfigVal(config::getBoolean, getHoconName(variableName)).orElse(false);
  }

  protected Optional<String> getString(
      SettingDescription settingDescription, Http.RequestHeader request) {
    return getString(settingDescription.variableName(), request);
  }

  protected Optional<String> getString(String variableName, Http.RequestHeader request) {
    if (!request.attrs().containsKey(CIVIFORM_SETTINGS_ATTRIBUTE_KEY)) {
      LOGGER.warn(
          String.format(
              "Settings not found on request when looking up value for %s", variableName));
      return getString(variableName);
    }

    var writableSettings = request.attrs().get(CIVIFORM_SETTINGS_ATTRIBUTE_KEY);

    return writableSettings.containsKey(variableName)
        ? Optional.of(writableSettings.get(variableName))
        : getString(variableName);
  }

  protected Optional<String> getString(SettingDescription settingDescription) {
    return getConfigVal(config::getString, getHoconName(settingDescription));
  }

  protected Optional<String> getString(String variableName) {
    return getConfigVal(config::getString, getHoconName(variableName));
  }

  protected Optional<Integer> getInt(
      SettingDescription settingDescription, Http.RequestHeader request) {
    return getInt(settingDescription.variableName(), request);
  }

  protected Optional<Integer> getInt(String variableName, Http.RequestHeader request) {
    if (!request.attrs().containsKey(CIVIFORM_SETTINGS_ATTRIBUTE_KEY)) {
      LOGGER.warn(
          String.format(
              "Settings not found on request when looking up value for %s", variableName));
      return getInt(variableName);
    }

    var writableSettings = request.attrs().get(CIVIFORM_SETTINGS_ATTRIBUTE_KEY);

    return writableSettings.containsKey(variableName)
        ? Optional.of(Integer.parseInt(writableSettings.get(variableName)))
        : getInt(variableName);
  }

  protected Optional<Integer> getInt(SettingDescription settingDescription) {
    return getConfigVal(config::getInt, getHoconName(settingDescription));
  }

  protected Optional<Integer> getInt(String variableName) {
    return getConfigVal(config::getInt, getHoconName(variableName));
  }

  protected Optional<ImmutableList<String>> getListOfStrings(
      SettingDescription settingDescription, Http.RequestHeader request) {
    return getListOfStrings(settingDescription.variableName(), request);
  }

  protected Optional<ImmutableList<String>> getListOfStrings(
      String variableName, Http.RequestHeader request) {
    if (!request.attrs().containsKey(CIVIFORM_SETTINGS_ATTRIBUTE_KEY)) {
      LOGGER.warn(
          String.format(
              "Settings not found on request when looking up value for %s", variableName));
      return getListOfStrings(variableName);
    }

    var writableSettings = request.attrs().get(CIVIFORM_SETTINGS_ATTRIBUTE_KEY);

    if (!writableSettings.containsKey(variableName)) {
      return getListOfStrings(variableName);
    }

    return Optional.of(
        ImmutableList.copyOf(Splitter.on(",").split(writableSettings.get(variableName))));
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
