package controllers.dev;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableMap;
import java.util.Locale;
import javax.inject.Inject;
import play.mvc.Controller;
import play.mvc.Http.HeaderNames;
import play.mvc.Http.Request;
import play.mvc.Result;
import services.DeploymentType;
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
  private final boolean isDevOrStaging;

  @Inject
  public FeatureFlagOverrideController(
      SettingsService settingsService, DeploymentType deploymentType) {
    this.settingsService = Preconditions.checkNotNull(settingsService);
    this.isDevOrStaging = deploymentType.isDevOrStaging();
  }

  public Result enable(Request request, String rawFlagName) {
    return updateFlag(request, rawFlagName, "true");
  }

  public Result disable(Request request, String rawFlagName) {
    return updateFlag(request, rawFlagName, "false");
  }

  private Result updateFlag(Request request, String rawFlagName, String newValue) {
    if (!isDevOrStaging) {
      return notFound();
    }
    var flagName = rawFlagName.toUpperCase(Locale.ROOT);
    var currentSettings =
        settingsService.loadSettings().toCompletableFuture().join().orElse(ImmutableMap.of());

    ImmutableMap.Builder<String, String> newSettings = ImmutableMap.builder();

    for (var entry : currentSettings.entrySet()) {
      if (!entry.getKey().equals(flagName)) {
        newSettings.put(entry);
      }
    }

    newSettings.put(flagName, newValue);
    settingsService.updateSettings(newSettings.build(), "dev mode");

    return redirect(request.getHeaders().get(HeaderNames.REFERER).orElse("/"));
  }
}
