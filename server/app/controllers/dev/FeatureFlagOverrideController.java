package controllers.dev;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableMap;
import java.util.Locale;
import java.util.Optional;
import javax.inject.Inject;
import models.SettingsGroupModel;
import play.cache.NamedCache;
import play.cache.SyncCacheApi;
import play.mvc.Controller;
import play.mvc.Http.HeaderNames;
import play.mvc.Http.Request;
import play.mvc.Result;
import services.settings.SettingsManifest;
import services.settings.SettingsService;

/**
 * Allows for overriding of feature flags by an Admin via HTTP request.
 *
 * <p>Overrides are stored in the session cookie and used by {@link SettingsManifest} to control
 * system behavior
 */
public final class FeatureFlagOverrideController extends Controller {

  private final SettingsService settingsService;
  private final SyncCacheApi settingsCache;

  @Inject
  public FeatureFlagOverrideController(
      SettingsService settingsService,
      @NamedCache("civiform-settings") SyncCacheApi settingsCache) {
    this.settingsService = Preconditions.checkNotNull(settingsService);
    this.settingsCache = Preconditions.checkNotNull(settingsCache);
  }

  public Result enable(Request request, String rawFlagName) {
    return updateFlag(request, rawFlagName, "true");
  }

  public Result disable(Request request, String rawFlagName) {
    return updateFlag(request, rawFlagName, "false");
  }

  private Result updateFlag(Request request, String rawFlagName, String newValue) {
    var flagName = rawFlagName.toUpperCase(Locale.ROOT);

    Optional<Optional<SettingsGroupModel>> optionalCacheEntry =
        settingsCache.get("current-settings");

    if (optionalCacheEntry.isEmpty()) {
      throw new IllegalStateException("No cached settings found");
    }

    Optional<SettingsGroupModel> optionalSettingsGroupModel = optionalCacheEntry.get();

    if (optionalSettingsGroupModel.isEmpty()) {
      throw new IllegalStateException("No cached settings found");
    }

    SettingsGroupModel cachedSettings = optionalSettingsGroupModel.get();
    var currentSettings = cachedSettings.getSettings();

    ImmutableMap.Builder<String, String> newSettings = ImmutableMap.builder();

    for (var entry : currentSettings.entrySet()) {
      if (!entry.getKey().equals(flagName)) {
        newSettings.put(entry);
      }
    }

    newSettings.put(flagName, newValue);
    settingsService.updateSettings(newSettings.build(), "dev mode");

    return redirect(request.header(HeaderNames.REFERER).orElse("/"));
  }
}
