package services.settings;

import static com.google.common.base.Preconditions.checkNotNull;

import auth.CiviFormProfile;
import com.google.auto.value.AutoValue;
import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Maps;
import com.google.common.collect.Streams;
import com.google.inject.Inject;
import controllers.BadRequestException;
import java.util.Optional;
import java.util.concurrent.CompletionStage;
import java.util.stream.Stream;
import models.SettingsGroupModel;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import play.Environment;
import play.libs.typedmap.TypedKey;
import play.libs.typedmap.TypedMap;
import play.mvc.Http;
import repository.SettingsGroupRepository;
import services.ColorUtil;

/**
 * Service management of the resource backed by {@link SettingsGroupModel}.
 *
 * <p>Each time an admin updates the server settings using the admin UI, a SettingsGroup is saved.
 * The latest snapshot is used to provide settings for a given request to the server.
 *
 * <p>On each incoming request, the most recent SettingsGroup is loaded and its settings map stored
 * in the attributes of the incoming {@link play.mvc.Http.Request} object for ease of access
 * throughout the request lifecycle.
 */
public final class SettingsService {

  /** The key used in {@link play.mvc.Http.Request} attributes to store system settings. */
  public static final TypedKey<ImmutableMap<String, String>> CIVIFORM_SETTINGS_ATTRIBUTE_KEY =
      TypedKey.create("CIVIFORM_SETTINGS");

  private static final Logger logger = LoggerFactory.getLogger(SettingsService.class);

  private final SettingsGroupRepository settingsGroupRepository;
  private final SettingsManifest settingsManifest;
  private final Environment environment;

  @Inject
  public SettingsService(
      SettingsGroupRepository settingsGroupRepository,
      SettingsManifest settingsManifest,
      Environment environment) {
    this.settingsGroupRepository = checkNotNull(settingsGroupRepository);
    this.settingsManifest = checkNotNull(settingsManifest);
    this.environment = checkNotNull(environment);
  }

  /**
   * Load settings stored in the database. If the admin has never updated any settings this returns
   * an empty map.
   */
  public CompletionStage<Optional<ImmutableMap<String, String>>> loadSettings() {
    return settingsGroupRepository
        .getCurrentSettings()
        .thenApply(maybeSettingsGroup -> maybeSettingsGroup.map(SettingsGroupModel::getSettings));
  }

  /**
   * Loads the server settings from the database and returns a new request that has the settings in
   * the request attributes. If no settings are found an error is logged and the request argument is
   * returned.
   */
  public CompletionStage<Http.RequestHeader> applySettingsToRequest(Http.RequestHeader request) {
    return loadSettings()
        .thenApply(
            maybeSettings -> {
              if (maybeSettings.isEmpty()) {
                logger.error("No settings found when serving request.");
                return request;
              }

              TypedMap newAttrs =
                  request.attrs().put(CIVIFORM_SETTINGS_ATTRIBUTE_KEY, maybeSettings.get());

              return request.withAttrs(newAttrs);
            });
  }

  /** Update settings stored in the database. */
  public SettingsGroupUpdateResult updateSettings(
      ImmutableMap<String, String> newSettings, CiviFormProfile profile) {
    return updateSettings(newSettings, profile.getAuthorityId().join());
  }

  /**
   * Store a new {@link SettingsGroupModel} in the DB and returns {@code true} if the new settings
   * are different from the current settings. Otherwise returns {@code false} and does NOT insert a
   * new row.
   */
  public SettingsGroupUpdateResult updateSettings(
      ImmutableMap<String, String> newSettings, String papertrail) {
    var maybeExistingSettings = loadSettings().toCompletableFuture().join();

    if (maybeExistingSettings.map(newSettings::equals).orElse(false)) {
      return SettingsGroupUpdateResult.noChange();
    }

    if (maybeExistingSettings.isPresent()) {
      var validationErrors = validateSettings(newSettings, maybeExistingSettings.get());

      if (!validationErrors.isEmpty()) {
        return SettingsGroupUpdateResult.withErrors(validationErrors);
      }
    }

    var newSettingsGroup = new SettingsGroupModel(newSettings, papertrail);
    newSettingsGroup.save();
    // The SettingsCacheMaintainer will eventually clear the cache in all server instances. In
    // tests, we want to clear the cache synchronously to ensure assertions on the new settings
    // pass.
    if (environment.isTest()) {
      settingsGroupRepository.clearCurrentSettingsCache();
    }

    return SettingsGroupUpdateResult.success();
  }

  private static final ImmutableSet<String> BOOLEAN_VALUES = ImmutableSet.of("true", "false");

  private ImmutableMap<String, SettingsGroupUpdateResult.UpdateError> validateSettings(
      ImmutableMap<String, String> newSettings, ImmutableMap<String, String> existingSettings) {
    ImmutableMap.Builder<String, SettingsGroupUpdateResult.UpdateError> validationErrors =
        ImmutableMap.builder();
    ImmutableList<SettingDescription> settingDescriptions =
        settingsManifest.getAllAdminWriteableSettingDescriptions();

    var different = Maps.difference(newSettings, existingSettings);

    Stream<Pair<SettingDescription, String>> newEntries =
        different.entriesOnlyOnLeft().entrySet().stream()
            .map(
                entry ->
                    Pair.of(
                        getSettingDescription(settingDescriptions, entry.getKey()),
                        entry.getValue()));

    Stream<Pair<SettingDescription, String>> changedEntries =
        different.entriesDiffering().entrySet().stream()
            .map(
                entry ->
                    Pair.of(
                        getSettingDescription(settingDescriptions, entry.getKey()),
                        entry.getValue().leftValue()));

    Streams.concat(newEntries, changedEntries)
        .forEach(
            pair -> {
              SettingDescription settingDescription = pair.getLeft();
              String newValue = pair.getRight();

              if (settingDescription.isRequired() && newValue.isBlank()) {
                validationErrors.put(
                    settingDescription.variableName(),
                    SettingsGroupUpdateResult.UpdateError.create(newValue, "Required"));
                return;
              }

              switch (settingDescription.settingType()) {
                case BOOLEAN -> {
                  if (!BOOLEAN_VALUES.contains(newValue)) {
                    throw new BadRequestException(
                        String.format("Invalid boolean value: %s", newValue));
                  }
                }
                case ENUM -> validateEnum(settingDescription, newValue);
                case INT -> {
                  if (!StringUtils.isNumeric(newValue)) {
                    throw new BadRequestException(String.format("Invalid int value: %s", newValue));
                  }
                }

                  // LIST_OF_STRINGS included here for completeness since errorprone will produce a
                  // warning if a case statement isn't exhaustive.
                case LIST_OF_STRINGS -> {}
                case STRING -> {
                  Optional<SettingsGroupUpdateResult.UpdateError> error =
                      validateString(settingDescription, newValue);

                  if (error.isPresent()) {
                    validationErrors.put(settingDescription.variableName(), error.get());
                    break;
                  }
                  if (settingDescription.variableName().equals("THEME_COLOR_PRIMARY")
                      || settingDescription.variableName().equals("THEME_COLOR_PRIMARY_DARK")) {
                    // Only allow admins to set theme colors that have a contrast ratio of 4.5:1
                    // with white, for accessibility reasons.
                    if (!newValue.isEmpty() && !ColorUtil.contrastsWithWhite(newValue)) {
                      validationErrors.put(
                          settingDescription.variableName(),
                          SettingsGroupUpdateResult.UpdateError.create(
                              newValue,
                              "This color doesn't have enough contrast to be legible with white"
                                  + " text. To meet accessibility requirements, choose a color"
                                  + " with more contrast to white text here:"
                                  + " https://webaim.org/resources/contrastchecker/."));
                    }
                  }
                }
              }
            });

    return validationErrors.build();
  }

  private static SettingDescription getSettingDescription(
      ImmutableList<SettingDescription> settingDescriptions, String variableName) {
    return settingDescriptions.stream()
        .filter((sd) -> sd.variableName().equals(variableName))
        .findFirst()
        .orElseThrow(
            () ->
                new IllegalStateException(
                    String.format(
                        "No SettingDescription found in SettingsManifest for %s", variableName)));
  }

  private static void validateEnum(SettingDescription settingDescription, String value) {
    if (value.isBlank()) {
      return;
    }

    if (!settingDescription.allowableValues().get().contains(value)) {
      throw new BadRequestException(
          String.format(
              "Invalid enum value: %s, must be one of %s",
              value, Joiner.on(", ").join(settingDescription.allowableValues().get())));
    }
  }

  private static Optional<SettingsGroupUpdateResult.UpdateError> validateString(
      SettingDescription settingDescription, String value) {
    if (settingDescription.validationRegex().isPresent()
        && !settingDescription.validationRegex().get().matcher(value).matches()) {
      return Optional.of(
          SettingsGroupUpdateResult.UpdateError.create(
              value,
              String.format(
                  "Invalid input, must match %s", settingDescription.validationRegex().get())));
    }

    return Optional.empty();
  }

  /**
   * Inserts a new {@link SettingsGroupModel} if it finds admin writeable settings in the {@link
   * SettingsManifest} that are not in the current {@link SettingsGroupModel}.
   */
  public SettingsGroupModel migrateConfigValuesToSettingsGroup() {
    Optional<SettingsGroupModel> maybeExistingSettingsGroup =
        settingsGroupRepository.getCurrentSettings().toCompletableFuture().join();
    Optional<ImmutableMap<String, String>> maybeExistingSettings =
        maybeExistingSettingsGroup.map(SettingsGroupModel::getSettings);

    ImmutableMap.Builder<String, String> settingsBuilder = ImmutableMap.builder();

    for (var settingDescription : settingsManifest.getAllAdminWriteableSettingDescriptions()) {
      String serializedSettingValue =
          maybeExistingSettings
              .flatMap(
                  existingSettings ->
                      Optional.ofNullable(existingSettings.get(settingDescription.variableName())))
              .or(() -> settingsManifest.getSettingSerializationValue(settingDescription))
              .orElseGet(() -> getDefaultValue(settingDescription));

      settingsBuilder.put(settingDescription.variableName(), serializedSettingValue);
    }

    var settings = settingsBuilder.build();

    if (maybeExistingSettings.map(settings::equals).orElse(false)) {
      return maybeExistingSettingsGroup.get();
    }

    var group = new SettingsGroupModel(settings, "system");
    group.save();
    // The SettingsCacheMaintainer will eventually clear the cache in all server instances. In
    // tests, we want to clear the cache synchronously to ensure assertions on the new settings
    // pass.
    if (environment.isTest()) {
      settingsGroupRepository.clearCurrentSettingsCache();
    }

    logger.info("Migrated {} settings from config to database.", settings.size());

    return group;
  }

  private static String getDefaultValue(SettingDescription settingDescription) {
    return switch (settingDescription.settingType()) {
      case INT -> "0";
      case ENUM -> settingDescription.allowableValues().get().stream().findFirst().get();
      case LIST_OF_STRINGS, STRING -> "CHANGE ME";
      case BOOLEAN -> "false";
    };
  }

  /** Represents the result of an update attempt. */
  @AutoValue
  public abstract static class SettingsGroupUpdateResult {

    /**
     * Creates a result representing success, where a new {@link SettingsGroupModel} was inserted.
     */
    public static SettingsGroupUpdateResult success() {
      return new AutoValue_SettingsService_SettingsGroupUpdateResult(
          /* errorMessages= */ Optional.empty(), /* updated= */ true);
    }

    /**
     * Creates a result representing validation failure, where a new {@link SettingsGroupModel} was
     * NOT inserted and the admin should address the errors.
     */
    public static SettingsGroupUpdateResult withErrors(
        ImmutableMap<String, UpdateError> errorMessages) {
      return new AutoValue_SettingsService_SettingsGroupUpdateResult(
          Optional.of(errorMessages), /* updated= */ false);
    }

    /**
     * Creates a result representing failure where a new {@link SettingsGroupModel} was NOT inserted
     * due to the admin not changing any values.
     */
    public static SettingsGroupUpdateResult noChange() {
      return new AutoValue_SettingsService_SettingsGroupUpdateResult(
          /* errorMessages= */ Optional.empty(), /* updated= */ false);
    }

    /** Validation error messages for the attempted update. */
    public abstract Optional<ImmutableMap<String, UpdateError>> errorMessages();

    /** True if the update completed successfully, inserting a new {@link SettingsGroupModel}. */
    public abstract boolean updated();

    /** True if there are validation error messages. */
    public boolean hasErrors() {
      return errorMessages().isPresent();
    }

    /** A validation error for updating a setting value. */
    @AutoValue
    public abstract static class UpdateError {

      public static UpdateError create(String updatedValue, String errorMessage) {
        return new AutoValue_SettingsService_SettingsGroupUpdateResult_UpdateError(
            updatedValue, errorMessage);
      }

      /** The new value of the setting that failed validation. */
      public abstract String updatedValue();

      /** An error message describing why the updated value failed validation. */
      public abstract String errorMessage();
    }
  }
}
