package filters;

import static com.google.common.base.Preconditions.checkNotNull;

import akka.stream.Materializer;
import javax.inject.Inject;
import play.libs.streams.Accumulator;
import play.mvc.EssentialAction;
import play.mvc.EssentialFilter;
import play.mvc.Http;
import services.settings.SettingsManifest;
import services.settings.SettingsService;

/**
 * Loads the server settings from the database and adds them to the attributes of each incoming
 * request. This caches them for the life of the request for quick access in application code.
 */
public final class SettingsFilter extends EssentialFilter {

  private final SettingsService settingsService;
  private final Materializer materializer;
  private final SettingsManifest settingsManifest;

  @Inject
  public SettingsFilter(
      SettingsService settingsService,
      Materializer materializer,
      SettingsManifest settingsManifest) {
    this.settingsService = checkNotNull(settingsService);
    this.materializer = checkNotNull(materializer);
    this.settingsManifest = checkNotNull(settingsManifest);
  }

  @Override
  public EssentialAction apply(EssentialAction next) {
    if (!settingsManifest.getAdminSettingsPanelEnabled()) {
      return EssentialAction.of(next::apply);
    }

    return EssentialAction.of(
        (Http.RequestHeader request) ->
            Accumulator.flatten(
                settingsService
                    .applySettingsToRequest(request)
                    .thenApply(modifiedRequest -> next.apply(modifiedRequest)),
                materializer));
  }
}
