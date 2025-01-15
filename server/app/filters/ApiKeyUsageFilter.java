package filters;

import static com.google.common.base.Preconditions.checkNotNull;

import auth.ClientIpResolver;
import auth.ProfileUtils;
import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.Executor;
import javax.inject.Inject;
import javax.inject.Provider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import play.api.libs.concurrent.PekkoSchedulerProvider;
import play.mvc.EssentialAction;
import play.mvc.EssentialFilter;
import play.mvc.Http;
import scala.concurrent.ExecutionContext;
import services.apikey.ApiKeyService;

/**
 * This filter looks for requests with paths that begin with /api and record the usage data for the
 * relevant API key if it has one. The API key update is scheduled to run in a background thread to
 * reduce latency and to ensure issues with recording the usage do not cause API requests to fail.
 * Note that there is no retry logic, so if updating the key's usage info fails it will be
 * inconsistent. This is acceptable since it's not critical that the call count be perfectly
 * accurate.
 */
public class ApiKeyUsageFilter extends EssentialFilter {

  private final PekkoSchedulerProvider pekkoSchedulerProvider;
  private final Provider<ApiKeyService> apiKeyServiceProvider;
  private final Executor exec;
  private final Provider<ProfileUtils> profileUtilsProvider;
  private final ClientIpResolver clientIpResolver;
  private static final Logger LOGGER = LoggerFactory.getLogger(ApiKeyUsageFilter.class);

  @Inject
  public ApiKeyUsageFilter(
      PekkoSchedulerProvider pekkoSchedulerProvider,
      Provider<ApiKeyService> apiKeyServiceProvider,
      Executor exec,
      Provider<ProfileUtils> profileUtilsProvider,
      ClientIpResolver clientIpResolver) {
    this.pekkoSchedulerProvider = checkNotNull(pekkoSchedulerProvider);
    this.apiKeyServiceProvider = checkNotNull(apiKeyServiceProvider);
    this.exec = checkNotNull(exec);
    this.profileUtilsProvider = checkNotNull(profileUtilsProvider);
    this.clientIpResolver = checkNotNull(clientIpResolver);
  }

  @Override
  public EssentialAction apply(EssentialAction next) {
    return EssentialAction.of(
        (Http.RequestHeader request) ->
            next.apply(request)
                .map(
                    result -> {
                      try {
                        if (request.path().startsWith("/api/")) {
                          Optional<String> maybeApiKeyId =
                              profileUtilsProvider.get().currentApiKeyId(request);

                          // If the key ID is not present then the request was not
                          // authenticated and does not need to be recorded.
                          if (maybeApiKeyId.isPresent()) {
                            String remoteAddress = clientIpResolver.resolveClientIp(request);

                            pekkoSchedulerProvider
                                .get()
                                .scheduleOnce(
                                    Duration.ZERO,
                                    () ->
                                        apiKeyServiceProvider
                                            .get()
                                            .recordApiKeyUsage(maybeApiKeyId.get(), remoteAddress),
                                    ExecutionContext.fromExecutor(exec));
                          }
                        }
                      } catch (RuntimeException e) {
                        LOGGER.error("Error updating ApiKey usage: {}", e.toString());
                      }

                      return result;
                    },
                    exec));
  }
}
