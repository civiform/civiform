package filters;

import static com.google.common.base.Preconditions.checkNotNull;

import auth.ProfileUtils;
import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.Executor;
import javax.inject.Inject;
import javax.inject.Provider;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import play.api.libs.concurrent.AkkaSchedulerProvider;
import play.mvc.EssentialAction;
import play.mvc.EssentialFilter;
import play.mvc.Http;
import scala.concurrent.ExecutionContext;
import services.apikey.ApiKeyService;

/**
 * This filter looks for requests with paths that begin with /api and record the usage data for the
 * relevant API key if it has one. The API key update is scheduled to run in a background thread through to
 * reduce latency and to ensure issues with recording the usage do not cause API requests to fail.
 */
public class ApiKeyUsageFilter extends EssentialFilter {

  private final AkkaSchedulerProvider akkaSchedulerProvider;
  private final Provider<ApiKeyService> apiKeyServiceProvider;
  private final Executor exec;
  private final Provider<ProfileUtils> profileUtilsProvider;
  private static final Logger LOGGER = LoggerFactory.getLogger(ApiKeyUsageFilter.class);

  @Inject
  public ApiKeyUsageFilter(
      AkkaSchedulerProvider akkaSchedulerProvider,
      Provider<ApiKeyService> apiKeyServiceProvider,
      Executor exec,
      Provider<ProfileUtils> profileUtilsProvider) {
    this.akkaSchedulerProvider = checkNotNull(akkaSchedulerProvider);
    this.apiKeyServiceProvider = checkNotNull(apiKeyServiceProvider);
    this.exec = checkNotNull(exec);
    this.profileUtilsProvider = checkNotNull(profileUtilsProvider);
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
                            String remoteAddress = request.remoteAddress();

                            akkaSchedulerProvider
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
                        LOGGER.error("Error updating ApiKey usage: %s", e.toString());
                      }

                      return result;
                    },
                    exec));
  }
}
