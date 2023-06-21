package filters;

import static com.google.common.base.Preconditions.checkNotNull;

import akka.stream.Materializer;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import javax.inject.Inject;
import play.libs.streams.Accumulator;
import play.mvc.EssentialAction;
import play.mvc.EssentialFilter;
import play.mvc.Http;
import services.settings.SettingsService;

class SettingsFilter extends EssentialFilter {

  private final SettingsService settingsService;
  private final Materializer materializer;

  @Inject
  public SettingsFilter(SettingsService settingsService, Materializer materializer) {
    this.settingsService = checkNotNull(settingsService);
    this.materializer = checkNotNull(materializer);
  }

  @Override
  public EssentialAction apply(EssentialAction next) {
    return EssentialAction.of(
        (Http.RequestHeader request) ->
            Accumulator.flatten(
                settingsService
                    .applySettingsToRequest(request)
                    .thenApply(unused -> next.apply(request)),
                materializer));
  }

  public CompletionStage<Integer> foo() {
    return CompletableFuture.completedFuture("foo").thenApply(unused -> 12);
  }
}
