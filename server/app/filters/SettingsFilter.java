package filters;

import static com.google.common.base.Preconditions.checkNotNull;

import akka.stream.Materializer;
import com.google.common.collect.ImmutableList;
import javax.inject.Inject;
import javax.inject.Provider;
import play.libs.streams.Accumulator;
import play.mvc.EssentialAction;
import play.mvc.EssentialFilter;
import play.mvc.Http;
import services.settings.SettingsService;

/**
 * Loads the server settings from the database and adds them to the attributes of each incoming
 * request. This caches them for the life of the request for quick access in application code.
 */
public final class SettingsFilter extends EssentialFilter {

  private final Provider<SettingsService> settingsService;
  private final Materializer materializer;

  @Inject
  public SettingsFilter(Provider<SettingsService> settingsService, Materializer materializer) {
    this.settingsService = checkNotNull(settingsService);
    this.materializer = checkNotNull(materializer);
  }

  // The URL paths with these prefixes do not consume the SettingsManifest so
  // requests to them don't need to load it.
  private static final ImmutableList<String> EXCLUDED_PATHS =
      ImmutableList.of("/assets/", "/favicon", "/playIndex");

  @Override
  public EssentialAction apply(EssentialAction next) {
    return EssentialAction.of(
        (Http.RequestHeader request) -> {
          if (EXCLUDED_PATHS.stream().anyMatch(prefix -> request.path().startsWith(prefix))) {
            return next.apply(request);
          }

          return Accumulator.flatten(
              settingsService.get().applySettingsToRequest(request).thenApply(next::apply),
              materializer);
        });
  }
}
