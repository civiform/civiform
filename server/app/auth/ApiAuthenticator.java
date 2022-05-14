package auth;

import static com.google.common.base.Preconditions.checkNotNull;

import java.time.Instant;
import java.util.Optional;
import javax.inject.Inject;
import javax.inject.Provider;
import models.ApiKey;
import org.apache.commons.net.util.SubnetUtils;
import org.pac4j.core.context.WebContext;
import org.pac4j.core.context.session.SessionStore;
import org.pac4j.core.credentials.Credentials;
import org.pac4j.core.credentials.UsernamePasswordCredentials;
import org.pac4j.core.credentials.authenticator.Authenticator;
import org.pac4j.core.exception.BadCredentialsException;
import play.cache.NamedCache;
import play.cache.SyncCacheApi;
import services.apikey.ApiKeyService;

public class ApiAuthenticator implements Authenticator {

  private static final int CACHE_EXPIRATION_TIME_SECONDS = 60;

  private final SyncCacheApi syncCacheApi;
  private final Provider<ApiKeyService> apiKeyService;

  @Inject
  public ApiAuthenticator(
      @NamedCache("api-keys") SyncCacheApi syncCacheApi, Provider<ApiKeyService> apiKeyService) {
    this.syncCacheApi = checkNotNull(syncCacheApi);
    this.apiKeyService = checkNotNull(apiKeyService);
  }

  @Override
  public void validate(Credentials rawCredentials, WebContext context, SessionStore sessionStore) {
    if (!(rawCredentials instanceof UsernamePasswordCredentials)) {
      throw new RuntimeException("ApiAuthenticator must receive UsernamePasswordCredentials.");
    }

    UsernamePasswordCredentials credentials = (UsernamePasswordCredentials) rawCredentials;
    String keyId = credentials.getUsername();

    // Cache the API key for quick lookup in the controller, also for subsequent requests.
    // We intentionally cache the empty optional rather than throwing here so that subsequent
    // requests with the invalid key do not put pressure on the database.
    Optional<ApiKey> maybeApiKey =
        syncCacheApi.getOrElseUpdate(
            keyId, () -> apiKeyService.get().findByKeyId(keyId), CACHE_EXPIRATION_TIME_SECONDS);

    if (!maybeApiKey.isPresent()) {
      throw new BadCredentialsException("API key does not exist");
    }

    ApiKey apiKey = maybeApiKey.get();

    if (apiKey.isRetired()) {
      throw new BadCredentialsException("API key is retired");
    }

    if (apiKey.expiredAfter(Instant.now())) {
      throw new BadCredentialsException("API key is expired");
    }

    SubnetUtils allowedSubnet = new SubnetUtils(apiKey.getSubnet());
    // Setting this to true includes the network and broadcast addresses.
    // I.e. /31 and /32 will not be considered included in the subnetwork
    // if this is false.
    allowedSubnet.setInclusiveHostCount(true);

    if (!allowedSubnet.getInfo().isInRange(context.getRemoteAddr())) {
      throw new BadCredentialsException("IP not in allowed range");
    }

    String saltedCredentialsSecret = apiKeyService.get().salt(credentials.getPassword());
    if (!saltedCredentialsSecret.equals(apiKey.getSaltedKeySecret())) {
      throw new BadCredentialsException("Invalid password");
    }
  }
}
