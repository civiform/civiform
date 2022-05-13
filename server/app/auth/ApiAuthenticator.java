package auth;

import static com.google.common.base.Preconditions.checkNotNull;

import java.time.Instant;
import javax.inject.Inject;
import models.ApiKey;
import org.apache.commons.net.util.SubnetUtils;
import org.pac4j.core.context.WebContext;
import org.pac4j.core.context.session.SessionStore;
import org.pac4j.core.credentials.Credentials;
import org.pac4j.core.credentials.UsernamePasswordCredentials;
import org.pac4j.core.credentials.authenticator.Authenticator;
import org.pac4j.core.exception.CredentialsException;
import play.cache.NamedCache;
import play.cache.SyncCacheApi;
import services.apikey.ApiKeyService;

public class ApiAuthenticator implements Authenticator {

  private static final int CACHE_EXPIRATION_TIME_SECONDS = 60;

  private final SyncCacheApi syncCacheApi;
  private final ApiKeyService apiKeyService;

  @Inject
  public ApiAuthenticator(@NamedCache("api-keys") SyncCacheApi syncCacheApi, ApiKeyService apiKeyService) {
    this.syncCacheApi = checkNotNull(syncCacheApi);
    this.apiKeyService = checkNotNull(apiKeyService);
  }

  @Override
  public void validate(
      Credentials uncastCredentials, WebContext context, SessionStore sessionStore) {
    if (!(uncastCredentials instanceof UsernamePasswordCredentials)) {
      throw new RuntimeException("ApiAuthenticator must receive UsernamePasswordCredentials.");
    }

    UsernamePasswordCredentials credentials = (UsernamePasswordCredentials) uncastCredentials;
    String keyId = credentials.getPassword();

    // Cache the API key for quick lookup in the controller, also for subsequent requests.
    ApiKey apiKey =
        syncCacheApi.getOrElseUpdate(
            keyId,
            () ->
                apiKeyService
                    .findByKeyId(keyId)
                    .orElseThrow(() -> new CredentialsException("Invalid API key")),
            CACHE_EXPIRATION_TIME_SECONDS);

    if (apiKey.isRetired()) {
      throw new CredentialsException("API key is retired");
    }

    if (apiKey.expiredAfter(Instant.now())) {
      throw new CredentialsException("API key is expired");
    }

    SubnetUtils allowedSubnet = new SubnetUtils(apiKey.getSubnet());
    if (!allowedSubnet.getInfo().isInRange(context.getRemoteAddr())) {
      throw new CredentialsException("IP not in allowed range");
    }

    String saltedCredentialsSecret = apiKeyService.salt(credentials.getPassword());
    if (!saltedCredentialsSecret.equals(apiKey.getSaltedKeySecret())) {
      throw new CredentialsException("Invalid password");
    }
  }
}
