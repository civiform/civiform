package auth;

import static play.test.Helpers.fakeRequest;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.pac4j.core.context.session.SessionStore;
import org.pac4j.core.credentials.UsernamePasswordCredentials;
import org.pac4j.play.PlayWebContext;
import play.mvc.Http;
import repository.ResetPostgres;

public class ApiAuthenticatorTest extends ResetPostgres {

  private static final SessionStore MOCK_SESSION_STORE = Mockito.mock(SessionStore.class);
  private ApiAuthenticator apiAuthenticator;

  @Before
  public void setUp() {
    apiAuthenticator = instanceOf(ApiAuthenticator.class);
  }

  @Test
  public void validate_success() {
    String keyId = "keyId";
    String secret = "secret";
    resourceCreator.createActiveApiKey("test-key", keyId, secret);
    String rawCredentials = keyId + ":" + secret;
    String creds =
        Base64.getEncoder().encodeToString(rawCredentials.getBytes(StandardCharsets.UTF_8));

    Http.Request request =
        fakeRequest().remoteAddress("1.1.1.1").header("Authorization", "Basic " + creds).build();

    apiAuthenticator.validate(
        new UsernamePasswordCredentials(keyId, secret),
        new PlayWebContext(request),
        MOCK_SESSION_STORE);
  }
}
