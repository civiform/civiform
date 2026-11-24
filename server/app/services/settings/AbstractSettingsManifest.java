package services.settings;

import static com.google.common.base.Preconditions.checkNotNull;
import static services.settings.SettingMode.ADMIN_WRITEABLE;

import com.google.common.base.Splitter;
import com.google.common.base.Supplier;
import com.google.common.base.Suppliers;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSortedMap;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigException;
import java.util.Locale;
import java.util.Optional;
import java.util.function.Function;
import javax.inject.Provider;
import models.SettingsGroupModel;
import repository.SettingsGroupRepository;

/** Provides behavior for {@link SettingsManifest}. */
public abstract class AbstractSettingsManifest {
  public static final String FEATURE_FLAG_SETTING_SECTION_NAME = "Feature Flags";

  private final Config config;
  private final Supplier<Optional<ImmutableMap<String, String>>> storedSettings;

  /**
   * Creates a manifest that only reads from the application {@link Config}. Settings an admin has
   * stored in the database are ignored.
   */
  public AbstractSettingsManifest(Config config) {
    this.config = checkNotNull(config);
    this.storedSettings = Optional::empty;
  }

  /**
   * Creates a manifest that reads admin writeable settings from the current {@link
   * SettingsGroupModel}, falling back to the application {@link Config}.
   *
   * <p>The repository is resolved on first use, since the manifest can be constructed before the
   * database is available.
   */
  public AbstractSettingsManifest(
      Config config, Provider<SettingsGroupRepository> settingsGroupRepositoryProvider) {
    this.config = checkNotNull(config);
    checkNotNull(settingsGroupRepositoryProvider);

    Supplier<SettingsGroupRepository> settingsGroupRepository =
        Suppliers.memoize(settingsGroupRepositoryProvider::get);
    this.storedSettings =
        () ->
            settingsGroupRepository
                .get()
                .getCurrentSettingsSync()
                .map(SettingsGroupModel::getSettings);
  }

  /**
   * Returns a map containing the names of the settings in the "Feature Flags" section mapped to
   * their current values.
   */
  public ImmutableSortedMap<String, Boolean> getAllFeatureFlagsSorted() {
    ImmutableSortedMap.Builder<String, Boolean> map = ImmutableSortedMap.naturalOrder();

    for (SettingDescription settingDescription : getAllFeatureFlagsSettingDescriptions()) {
      String name = settingDescription.variableName();
      map.put(name, getWriteableBool(name));
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
    return Optional.of(getSections())
        .map(m -> m.get(FEATURE_FLAG_SETTING_SECTION_NAME))
        .map(this::getSettingDescriptions)
        .orElseGet(ImmutableList::of);
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
   * Retrieve a string representation of the setting suitable for display in the UI from the stored
   * settings or HOCON config.
   */
  public Optional<String> getSettingDisplayValue(SettingDescription settingDescription) {
    String variableName = settingDescription.variableName();

    return switch (settingDescription.settingType()) {
      case BOOLEAN ->
          Optional.of(String.valueOf(getWriteableBool(variableName)).toUpperCase(Locale.ROOT));
      case INT -> getWriteableInt(variableName).map(String::valueOf);
      case LIST_OF_STRINGS ->
          getWriteableListOfStrings(variableName).map(list -> String.join(", ", list));
      case ENUM, STRING -> getWriteableString(variableName);
    };
  }

  /**
   * Retrieve a string representation of the setting suitable for JSON serialization from HOCON
   * config.
   */
  public Optional<String> getSettingSerializationValue(SettingDescription settingDescription) {
    return switch (settingDescription.settingType()) {
      case BOOLEAN -> getBool(settingDescription).map(String::valueOf);
      case INT -> getInt(settingDescription).map(String::valueOf);
      case LIST_OF_STRINGS ->
          getListOfStrings(settingDescription).map(list -> String.join(",", list));
      case ENUM, STRING -> getString(settingDescription).map(String::valueOf);
    };
  }

  /**
   * Gets the value for the given admin writeable setting. If the setting is found in the stored
   * writeable settings, the value from the database is returned. Otherwise the value from the
   * application {@link Config} is used.
   */
  protected boolean getWriteableBool(String variableName) {
    return getStoredValue(variableName)
        .map(value -> value.equals("true"))
        .orElseGet(() -> getBool(variableName));
  }

  protected Optional<Boolean> getBool(SettingDescription settingDescription) {
    return getConfigVal(config::getBoolean, getHoconName(settingDescription));
  }

  public boolean getBool(String variableName) {
    return getConfigVal(config::getBoolean, getHoconName(variableName)).orElse(false);
  }

  /** See {@link #getWriteableBool(String)}. */
  protected Optional<String> getWriteableString(String variableName) {
    return getStoredValue(variableName).or(() -> getString(variableName));
  }

  protected Optional<String> getString(SettingDescription settingDescription) {
    return getConfigVal(config::getString, getHoconName(settingDescription));
  }

  protected Optional<String> getString(String variableName) {
    return getConfigVal(config::getString, getHoconName(variableName));
  }

  /** See {@link #getWriteableBool(String)}. */
  protected Optional<Integer> getWriteableInt(String variableName) {
    return getStoredValue(variableName).map(Integer::parseInt).or(() -> getInt(variableName));
  }

  protected Optional<Integer> getInt(SettingDescription settingDescription) {
    return getConfigVal(config::getInt, getHoconName(settingDescription));
  }

  protected Optional<Integer> getInt(String variableName) {
    return getConfigVal(config::getInt, getHoconName(variableName));
  }

  /** See {@link #getWriteableBool(String)}. */
  protected Optional<ImmutableList<String>> getWriteableListOfStrings(String variableName) {
    return getStoredValue(variableName)
        .map(value -> ImmutableList.copyOf(Splitter.on(",").split(value)))
        .or(() -> getListOfStrings(variableName));
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

  /** Looks up the value an admin stored for the setting, if there is one. */
  private Optional<String> getStoredValue(String variableName) {
    return storedSettings.get().map(settings -> settings.get(variableName));
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
