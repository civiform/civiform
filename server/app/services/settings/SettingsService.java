package services.settings;

import static com.google.common.base.Preconditions.checkNotNull;

import auth.CiviFormProfile;
import com.google.common.collect.ImmutableMap;
import com.google.inject.Inject;
import java.util.Optional;
import java.util.concurrent.CompletionStage;
import models.SettingsGroup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import play.libs.typedmap.TypedKey;
import play.libs.typedmap.TypedMap;
import play.mvc.Http;
import repository.SettingsGroupRepository;

/**
 * Service management of the resource backed by {@link models.SettingsGroup}.
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

  private static final Logger LOGGER = LoggerFactory.getLogger(SettingsService.class);

  private final SettingsGroupRepository settingsGroupRepository;
  private final SettingsManifest settingsManifest;

  @Inject
  public SettingsService(
      SettingsGroupRepository settingsGroupRepository, SettingsManifest settingsManifest) {
    this.settingsGroupRepository = checkNotNull(settingsGroupRepository);
    this.settingsManifest = checkNotNull(settingsManifest);
  }

  /**
   * Load settings stored in the database. If the admin has never updated any settings this returns
   * an empty map.
   */
  public CompletionStage<Optional<ImmutableMap<String, String>>> loadSettings() {
    return settingsGroupRepository
        .getCurrentSettings()
        .thenApply(maybeSettingsGroup -> maybeSettingsGroup.map(SettingsGroup::getSettings));
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
                LOGGER.error("No settings found when serving request.");
                return request;
              }

              TypedMap newAttrs =
                  request.attrs().put(CIVIFORM_SETTINGS_ATTRIBUTE_KEY, maybeSettings.get());

              return request.withAttrs(newAttrs);
            });
  }

  /** Update settings stored in the database. */
  public boolean updateSettings(ImmutableMap<String, String> newSettings, CiviFormProfile profile) {
    return updateSettings(newSettings, profile.getAuthorityId().join());
  }

  /**
   * Store a new {@link SettingsGroup} in the DB and returns {@code true} if the new settings are
   * different from the current settings. Otherwise returns {@code false} and does NOT insert a new
   * row.
   */
  public boolean updateSettings(ImmutableMap<String, String> newSettings, String papertrail) {
    var maybeExistingSettings = loadSettings().toCompletableFuture().join();

    if (maybeExistingSettings.map(newSettings::equals).orElse(false)) {
      return false;
    }

    var newSettingsGroup = new SettingsGroup(newSettings, papertrail);
    newSettingsGroup.save();

    return true;
  }

  /**
   * Inserts a new {@link SettingsGroup} if it finds admin writeable settings in the {@link
   * SettingsManifest} that are not in the current {@link SettingsGroup}.
   */
  public SettingsGroup migrateConfigValuesToSettingsGroup() {
    Optional<SettingsGroup> maybeExistingSettingsGroup =
        settingsGroupRepository.getCurrentSettings().toCompletableFuture().join();
    Optional<ImmutableMap<String, String>> maybeExistingSettings =
        maybeExistingSettingsGroup.map(SettingsGroup::getSettings);

    ImmutableMap.Builder<String, String> settingsBuilder = ImmutableMap.builder();

    for (var settingDescription : settingsManifest.getAllAdminWriteableSettingDescriptions()) {
      maybeExistingSettings
          .flatMap(
              existingSettings ->
                  Optional.ofNullable(existingSettings.get(settingDescription.variableName())))
          .ifPresentOrElse(
              existingValue ->
                  settingsBuilder.put(settingDescription.variableName(), existingValue),
              () ->
                  settingsManifest
                      .getSettingSerializationValue(settingDescription)
                      .ifPresent(
                          value -> settingsBuilder.put(settingDescription.variableName(), value)));
    }

    var settings = settingsBuilder.build();

    if (maybeExistingSettings.map(settings::equals).orElse(false)) {
      return maybeExistingSettingsGroup.get();
    }

    var group = new SettingsGroup(settings, "system");
    group.save();

    LOGGER.info("Migrated {} settings from config to database.", settings.size());

    return group;
  }
}
