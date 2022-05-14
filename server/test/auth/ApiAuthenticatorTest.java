package auth;

import static play.test.Helpers.fakeRequest;

import java.time.Instant;
import models.ApiKey;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.pac4j.core.context.session.SessionStore;
import org.pac4j.core.credentials.UsernamePasswordCredentials;
import org.pac4j.play.PlayWebContext;
import play.mvc.Http;
import repository.ResetPostgres;
import services.apikey.ApiKeyService;

public class ApiAuthenticatorTest extends ResetPostgres {

  private static final SessionStore MOCK_SESSION_STORE = Mockito.mock(SessionStore.class);
  private ApiAuthenticator apiAuthenticator;
  private ApiKeyService apiKeyService;

  @Before
  public void setUp() {
    apiAuthenticator = instanceOf(ApiAuthenticator.class);
    apiKeyService = instanceOf(ApiKeyService.class);
  }

  @Test
  public void validate_success() {
    String keyId = "keyId";
    String secret = "secret";

    new ApiKey()
        .setName("test-key")
        .setKeyId(keyId)
        .setExpiration(Instant.now().plusSeconds(1))
        .setSubnet("1.1.1.1/32")
        .setSaltedKeySecret(apiKeyService.salt(secret))
        .setCreatedBy("test")
        .save();

    Http.Request request = fakeRequest().remoteAddress("1.1.1.1").build();

    apiAuthenticator.validate(
        new UsernamePasswordCredentials(keyId, secret),
        new PlayWebContext(request),
        MOCK_SESSION_STORE);
  }
}
