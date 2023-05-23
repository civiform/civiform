package filters;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.collect.ImmutableSet;
import java.util.Locale;
import java.util.concurrent.Executor;
import javax.inject.Inject;
import play.mvc.EssentialAction;
import play.mvc.EssentialFilter;

/** A filter to disable caching our responses. */
public class DisableCachingFilter extends EssentialFilter {
  private final Executor exec;

  // Only cache items processed by the asset pipeline.
  private static final ImmutableSet<String> ASSET_PATH_PREFIXES =
      ImmutableSet.of("/public/", "/assets/", "/favicon.ico");

  // Only cache when Status is OK. https://web.dev/uses-long-cache-ttl/
  private static final ImmutableSet<Integer> OK_STATUS_CODES = ImmutableSet.of(200, 203, 206);

  @Inject
  public DisableCachingFilter(Executor exec) {
    super();
    this.exec = checkNotNull(exec);
  }

  @Override
  public EssentialAction apply(EssentialAction next) {
    return EssentialAction.of(
        request ->
            next.apply(request)
                .map(
                    result -> {
                      final Integer status = result.status();
                      final String path = request.uri().toLowerCase(Locale.ROOT);

                      if (ASSET_PATH_PREFIXES.stream().anyMatch(path::startsWith)
                          && OK_STATUS_CODES.contains(status)) {
                        // Static assets are fingerprinted so we can cache them for 2 weeks.
                        // Even in dev mode where static files also don't change that often
                        // it can add some performance improvement. Improves speed of
                        // browser tests significantly.
                        return result.withHeader(
                            "Cache-Control", "public, max-age=1209600, immutable");
                      }
                      // Don't cache anything else.
                      return result.withHeader(
                          "Cache-Control", "no-store, max-age=0, must-revalidate");
                    },
                    exec));
  }
}
