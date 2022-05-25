package auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static play.test.Helpers.fakeRequest;

import com.google.inject.Guice;
import com.google.inject.Inject;
import com.google.inject.Injector;
import io.ebean.DB;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.Optional;
import models.ApiKey;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.pac4j.core.context.session.SessionStore;
import org.pac4j.core.credentials.UsernamePasswordCredentials;
import org.pac4j.core.exception.BadCredentialsException;
import org.pac4j.play.PlayWebContext;
import play.Application;
import play.ApplicationLoader;
import play.Environment;
import play.api.inject.BindingKey;
import play.cache.NamedCache;
import play.cache.SyncCacheApi;
import play.inject.guice.GuiceApplicationBuilder;
import play.inject.guice.GuiceApplicationLoader;
import play.mvc.Http;
import play.test.Helpers;
import support.ResourceCreator;

public class ApiAuthenticatorTest {
  @Inject Application application;

  @Inject
  @NamedCache("api-keys")
  SyncCacheApi cacheApi;

  ResourceCreator resourceCreator;
  @Inject ApiAuthenticator apiAuthenticator;

  private static final String keyId = "keyId";
  private static final String secret = "secret";
  private static final String validRawCredentials = keyId + ":" + secret;
  private static final SessionStore MOCK_SESSION_STORE = Mockito.mock(SessionStore.class);
  private ApiKey apiKey;

  @Before
  public void setUp() {
    // Most test classes extend ResetPostgres which accesses an injector via the
    // application provided fakeApplication(). That injector is a play.inject.Injector
    // which makes it much less easy to inject things that are annotated with annotations
    // that take arguments, such as @NamedCache("api-cache'). Guice makes that fairly easy
    // though, hence this test class uses the guice injector directly.
    GuiceApplicationBuilder builder =
        new GuiceApplicationLoader().builder(new ApplicationLoader.Context(Environment.simple()));
    Injector injector = Guice.createInjector(builder.applicationModule());
    injector.injectMembers(this);

    resourceCreator =
        new ResourceCreator(
            new play.inject.Injector() {
              @Override
              public <T> T instanceOf(Class<T> clazz) {
                return injector.getInstance(clazz);
              }

              @Override
              public <T> T instanceOf(BindingKey<T> key) {
                return null;
              }

              @Override
              public play.api.inject.Injector asScala() {
                return null;
              }
            });

    Helpers.start(application);

    DB.getDefault().truncate("api_keys");
    apiKey = resourceCreator.createActiveApiKey("test-key", keyId, secret);
    cacheApi.remove(keyId);
  }

  @After
  public void tearDown() {
    Helpers.stop(application);
  }

  @Test
  public void validate_success() {
    apiAuthenticator.validate(
        new UsernamePasswordCredentials(keyId, secret),
        new PlayWebContext(buildFakeRequest(validRawCredentials)),
        MOCK_SESSION_STORE);

    Optional<Optional<ApiKey>> cacheEntry = cacheApi.get(keyId);
    Optional<ApiKey> cachedMaybeKey = cacheEntry.get();
    assertThat(cachedMaybeKey.get().id).isEqualTo(apiKey.id);
  }

  @Test
  public void validate_invalidKeyId() {
    String rawCredentials = "wrong" + ":" + secret;

    assertBadCredentialsException(
        buildFakeRequest(rawCredentials),
        new UsernamePasswordCredentials("wrong", secret),
        "API key does not exist: wrong");
  }

  @Test
  public void validate_retiredKey() {
    apiKey.retire("test-user");
    apiKey.save();

    assertBadCredentialsException(
        buildFakeRequest(validRawCredentials), "API key is retired: " + keyId);
  }

  @Test
  public void validate_isExpired() {
    // Set the expiration to the past, any time in the past will suffice.
    apiKey.setExpiration(Instant.now().minusSeconds(100));
    apiKey.save();

    assertBadCredentialsException(
        buildFakeRequest(validRawCredentials), "API key is expired: " + keyId);
  }

  @Test
  public void validate_ipNotInSubnet() {
    apiKey.setSubnet("2.2.2.2/30");
    apiKey.save();

    assertBadCredentialsException(
        buildFakeRequest(validRawCredentials),
        "IP 1.1.1.1 not in allowed range for key ID: " + keyId);
  }

  @Test
  public void validate_invalidSecret() {
    var rawCredentials = keyId + ":" + "notthesecret";

    assertBadCredentialsException(
        buildFakeRequest(rawCredentials),
        new UsernamePasswordCredentials(keyId, "notthesecret"),
        "Invalid secret for key ID: " + keyId);
  }

  private void assertBadCredentialsException(Http.Request request, String expectedMessage) {
    assertBadCredentialsException(
        request, new UsernamePasswordCredentials(keyId, secret), expectedMessage);
  }

  private void assertBadCredentialsException(
      Http.Request request, UsernamePasswordCredentials credentials, String expectedMessage) {
    var exception =
        assertThrows(
            BadCredentialsException.class,
            () ->
                apiAuthenticator.validate(
                    credentials, new PlayWebContext(request), MOCK_SESSION_STORE));

    assertThat(exception).hasMessage(expectedMessage);
  }

  private static Http.Request buildFakeRequest(String rawCredentials) {
    String creds =
        Base64.getEncoder().encodeToString(rawCredentials.getBytes(StandardCharsets.UTF_8));
    return fakeRequest().remoteAddress("1.1.1.1").header("Authorization", "Basic " + creds).build();
  }
}
