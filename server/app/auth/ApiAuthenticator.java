package auth;

import static com.google.common.base.Preconditions.checkNotNull;

import java.time.Instant;
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

/**
 * Authenticator for API requests based on HTTP basic auth and backed by the {@link ApiKey}
 * resource. Background: https://en.wikipedia.org/wiki/Basic_access_authentication
 *
 * <p>When referenced by a request, {@code ApiKey}s are stored in the "api-keys" named cache, keyed
 * by their key ID. This allows controller code to load the key without the need for a subsequent
 * database query. This is necessary because pac4j creates profiles (which are what's made available
 * to controller code through the request object) in the next step, but we need the API key to
 * perform authentication (this step). The purpose of the cache is to avoid making multiple database
 * calls to retrieve the API key throughout the cycle of the request.
 *
 * <p>Note that at this layer, the request is authenticated, not authorized. All API requests that
 * reach a controller are already authenticated, but it is the controller's responsibility to check
 * that the resource being accessed is authorized for the request's API key.
 *
 * <p>The API key ID and secret are provided as the basic auth username and password, respectively.
 * To be valid, a request must reference a valid API key with its username credential, and further
 * that key must:
 *
 * <ul>
 *   <li>Not be retired.
 *   <li>Have an expiration date in the future.
 *   <li>Have a subnet that includes the IP address of the request.
 *   <li>Have a salted key secret that matches the salted password in the request's basic auth
 *       credentials.
 * </ul>
 */
public class ApiAuthenticator implements Authenticator {

  // The cache expiration time is intended to be long enough reduce database queries from
  // authenticating API calls while being short enough that if an admin retires a key or
  // otherwise edits it, the edits will take effect within a reasonable amount of time.
  // When tuning this value, consider the use-case of an API consumer rapidly paging through
  // a list API, and also consider the admin retiring a key when they've discovered it has
  // been compromised.
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
    ApiKey apiKey =
        syncCacheApi
            .getOrElseUpdate(
                keyId, () -> apiKeyService.get().findByKeyId(keyId), CACHE_EXPIRATION_TIME_SECONDS)
            .orElseThrow(() -> new BadCredentialsException("API key does not exist"));

    if (apiKey.isRetired()) {
      throw new BadCredentialsException("API key is retired: " + keyId);
    }

    if (apiKey.expiredAfter(Instant.now())) {
      throw new BadCredentialsException("API key is expired: " + keyId);
    }

    SubnetUtils allowedSubnet = new SubnetUtils(apiKey.getSubnet());
    // Setting this to true includes the network and broadcast addresses.
    // I.e. /31 and /32 will not be considered included in the subnetwork
    // if this is false.
    allowedSubnet.setInclusiveHostCount(true);

    if (!allowedSubnet.getInfo().isInRange(context.getRemoteAddr())) {
      throw new BadCredentialsException(
          String.format(
              "IP %s not in allowed range for key ID: %s", context.getRemoteAddr(), keyId));
    }

    String saltedCredentialsSecret = apiKeyService.get().salt(credentials.getPassword());
    if (!saltedCredentialsSecret.equals(apiKey.getSaltedKeySecret())) {
      throw new BadCredentialsException("Invalid secret for key ID: " + keyId);
    }
  }
}
