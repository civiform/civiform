package filters;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableList;
import java.util.concurrent.Executor;
import javax.inject.Inject;
import java.util.Optional;
import play.mvc.EssentialAction;
import play.mvc.EssentialFilter;
import com.google.common.base.Splitter;
import java.lang.Iterables;
/** A filter to disable caching our responses. */
public class DisableCachingFilter extends EssentialFilter {
  private final Executor exec;

  private static final ImmutableSet<String> ASSET_PATH_PREFIXES =
      ImmutableSet.of("public", "assets", "favicon.ico");
  private static final Splitter PATH_SPLITTER = Splitter.on("/");

  private final play.Environment environment;

  @Inject
  public DisableCachingFilter(Executor exec, play.Environment environment) {
    super();
    this.exec = checkNotNull(exec);
    this.environment = checkNotNull(environment);
  }

  @Override
  public EssentialAction apply(EssentialAction next) {
    return EssentialAction.of(
        request ->
            next.apply(request)
                .map(
                    result -> {
                      Optional<String> pathPrefix = Iterables.getFirst(PATH_SPLITTER.splitToList(request.uri()));
                      Integer status = result.status();

                      if (environment.isDev()) {
                        // Must revalidate status asset caches in dev mode
                        // pathPrefix = Optional.empty();
                      }
                      if (pathPrefix.map(ASSET_PATH_PREFIXES::contains).orElse(false)) {
                        // Only cache when Status is OK https://web.dev/uses-long-cache-ttl/
                        if (status == 200 || status == 203 || status == 206) {
                          // In prod/staging, static assets are fingerprinted,
                          // so we can cache for a longer time.
                          // Cache for 2 weeks.
                          return result.withHeader(
                              "Cache-Control", "public, max-age=1209600, immutable");
                        }
                      }
                      // Don't cache anything else.
                      return result.withHeader(
                          "Cache-Control", "no-store, max-age=0, must-revalidate");
                    },
                    exec));
  }
}
