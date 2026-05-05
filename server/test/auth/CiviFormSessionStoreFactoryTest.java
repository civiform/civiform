package auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static support.FakeRequestBuilder.fakeRequestBuilder;

import com.google.common.collect.ImmutableMap;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import org.junit.Test;
import org.pac4j.core.context.session.SessionStore;
import org.pac4j.play.PlayWebContext;
import play.mvc.Http.Request;

public class CiviFormSessionStoreFactoryTest {

  private static final String SECRET1 = "secret-one";
  private static final String SECRET2 = "secret-two";

  private static PlayWebContext newContextWithSessionFrom(PlayWebContext source) {
    Request requestWithSession = source.supplementRequest(fakeRequestBuilder().build());
    return new PlayWebContext(requestWithSession);
  }

  private static Config createConfig(String secret) {
    return ConfigFactory.parseMap(ImmutableMap.of("play.http.secret.key", secret));
  }

  @Test
  public void deriveAes128Key_generatesConsistentKeysWithSameSecret() throws Exception {
    byte[] key1 = CiviFormSessionStoreFactory.deriveAes128Key(SECRET1);
    byte[] key2 = CiviFormSessionStoreFactory.deriveAes128Key(SECRET1);

    assertThat(key1).isEqualTo(key2);
  }

  @Test
  public void deriveAes128Key_generatesDifferentKeysWithDifferentSecrets() throws Exception {
    byte[] key1 = CiviFormSessionStoreFactory.deriveAes128Key(SECRET1);
    byte[] key2 = CiviFormSessionStoreFactory.deriveAes128Key(SECRET2);

    assertThat(key1).isNotEqualTo(key2);
  }

  @Test
  public void constructor_worksWithSpecialCharactersInSecret() {
    String specialSecret = "secret!@#$%^&*()_+-=[]{}|;':\",./<>?`~";
    Config config = createConfig(specialSecret);
    CiviFormSessionStoreFactory factory = new CiviFormSessionStoreFactory(config);

    assertThat(factory.newSessionStore()).isNotNull();
  }

  @Test
  public void constructor_worksWithUnicodeSecret() {
    String unicodeSecret = "秘密キー密码🔐";
    Config config = createConfig(unicodeSecret);
    CiviFormSessionStoreFactory factory = new CiviFormSessionStoreFactory(config);

    assertThat(factory.newSessionStore()).isNotNull();
  }

  @Test
  public void newSessionStore_calledMultipleTimes_returnsNewInstances() {
    Config config = createConfig(SECRET1);
    CiviFormSessionStoreFactory factory = new CiviFormSessionStoreFactory(config);

    var store1 = factory.newSessionStore();
    var store2 = factory.newSessionStore();
    var store3 = factory.newSessionStore();

    // Each call should return a new instance
    assertThat(store1).isNotSameAs(store2);
    assertThat(store2).isNotSameAs(store3);
    assertThat(store1).isNotSameAs(store3);
  }

  @Test
  public void sessionStore_cookieRoundTrip_sameSecret_canReadSession() {
    CiviFormSessionStoreFactory factory1 = new CiviFormSessionStoreFactory(createConfig(SECRET1));
    CiviFormSessionStoreFactory factory2 = new CiviFormSessionStoreFactory(createConfig(SECRET1));

    SessionStore store1 = factory1.newSessionStore();
    SessionStore store2 = factory2.newSessionStore();

    PlayWebContext context1 = new PlayWebContext(fakeRequestBuilder().build());
    store1.set(context1, "test-key", "test-value");

    PlayWebContext context2 = newContextWithSessionFrom(context1);

    assertThat(store2.get(context2, "test-key").get()).isEqualTo("test-value");
  }

  @Test
  public void sessionStore_cookieRoundTrip_differentSecret_rejectsSession() {
    CiviFormSessionStoreFactory factory1 = new CiviFormSessionStoreFactory(createConfig(SECRET1));
    CiviFormSessionStoreFactory factory2 = new CiviFormSessionStoreFactory(createConfig(SECRET2));

    SessionStore store1 = factory1.newSessionStore();
    SessionStore store2 = factory2.newSessionStore();

    PlayWebContext context1 = new PlayWebContext(fakeRequestBuilder().build());
    store1.set(context1, "test-key", "test-value");

    PlayWebContext context2 = newContextWithSessionFrom(context1);

    assertThatThrownBy(() -> store2.get(context2, "test-key")).isInstanceOf(RuntimeException.class);
  }
}
