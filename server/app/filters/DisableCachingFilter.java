package filters;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.collect.ImmutableList;
import java.util.concurrent.Executor;
import javax.inject.Inject;
import play.mvc.EssentialAction;
import play.mvc.EssentialFilter;

/** A filter to disable caching our responses. */
public class DisableCachingFilter extends EssentialFilter {
  private final Executor exec;

  private final ImmutableList<String> staticAssetPaths =
      ImmutableList.of("/public/", "/assets/", "favicon.ico", "/favicon.ico");

  @Inject private play.Environment environment;

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
                      String path = request.uri();
                      Integer status = result.status();
                      ImmutableList<String> assets = staticAssetPaths;

                      if (environment.isDev()) {
                        // Must revalidate status asset caches in dev mode
                        assets = ImmutableList.<String>of();
                      }
                      if (assets.stream().anyMatch(m -> path.contains(m))) {
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
