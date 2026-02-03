package auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static support.FakeRequestBuilder.fakeRequestBuilder;

import com.google.common.collect.ImmutableMap;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import org.apache.shiro.crypto.AesCipherService;
import org.junit.Test;
import org.pac4j.core.context.session.SessionStore;
import org.pac4j.core.util.serializer.JavaSerializer;
import org.pac4j.play.PlayWebContext;
import org.pac4j.play.store.PlayCookieSessionStore;
import play.mvc.Http.Request;

public class CiviFormSessionStoreFactoryTest {

  private static PlayWebContext newContextWithSessionFrom(PlayWebContext source) {
    Request requestWithSession = source.supplementRequest(fakeRequestBuilder().build());
    return new PlayWebContext(requestWithSession);
  }

  private static Config createConfig(String secret) {
    return ConfigFactory.parseMap(ImmutableMap.of("play.http.secret.key", secret));
  }

  @Test
  public void deriveAes128Key_generatesConsistentKeysWithSameSecret() throws Exception {
    String secret = "consistent-secret-key";
    byte[] key1 = CiviFormSessionStoreFactory.deriveAes128Key(secret);
    byte[] key2 = CiviFormSessionStoreFactory.deriveAes128Key(secret);

    assertThat(key1).isEqualTo(key2);
  }

  @Test
  public void deriveAes128Key_generatesDifferentKeysWithDifferentSecrets() throws Exception {
    byte[] key1 = CiviFormSessionStoreFactory.deriveAes128Key("secret-one");
    byte[] key2 = CiviFormSessionStoreFactory.deriveAes128Key("secret-two");

    assertThat(key1).isNotEqualTo(key2);
  }

  @Test
  public void deriveLegacyKey_generatesConsistentKeysWithSameSecret() throws Exception {
    String secret = "consistent-secret-key";
    byte[] key1 = CiviFormSessionStoreFactory.deriveLegacyAesKey(secret);
    byte[] key2 = CiviFormSessionStoreFactory.deriveLegacyAesKey(secret);

    assertThat(key1).isEqualTo(key2);
  }

  @Test
  public void deriveLegacyKey_generatesDifferentKeysWithDifferentSecrets() throws Exception {
    byte[] key1 = CiviFormSessionStoreFactory.deriveLegacyAesKey("secret-one");
    byte[] key2 = CiviFormSessionStoreFactory.deriveLegacyAesKey("secret-two");

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
    Config config = createConfig("test-secret");
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
    String secret = "shared-secret";
    CiviFormSessionStoreFactory factory1 = new CiviFormSessionStoreFactory(createConfig(secret));
    CiviFormSessionStoreFactory factory2 = new CiviFormSessionStoreFactory(createConfig(secret));

    SessionStore store1 = factory1.newSessionStore();
    SessionStore store2 = factory2.newSessionStore();

    PlayWebContext context1 = new PlayWebContext(fakeRequestBuilder().build());
    store1.set(context1, "test-key", "test-value");

    PlayWebContext context2 = newContextWithSessionFrom(context1);

    assertThat(store2.get(context2, "test-key")).contains("test-value");
  }

  @Test
  public void sessionStore_cookieRoundTrip_differentSecret_rejectsSession() {
    CiviFormSessionStoreFactory factory1 =
        new CiviFormSessionStoreFactory(createConfig("secret-one"));
    CiviFormSessionStoreFactory factory2 =
        new CiviFormSessionStoreFactory(createConfig("secret-two"));

    SessionStore store1 = factory1.newSessionStore();
    SessionStore store2 = factory2.newSessionStore();

    PlayWebContext context1 = new PlayWebContext(fakeRequestBuilder().build());
    store1.set(context1, "test-key", "test-value");

    PlayWebContext context2 = newContextWithSessionFrom(context1);

    assertThatThrownBy(() -> store2.get(context2, "test-key")).isInstanceOf(RuntimeException.class);
  }

  @Test
  public void sessionStore_cookieRoundTrip_legacyEncrypted_fallsBackToLegacyDecrypter() {
    String secret = "legacy-secret";
    CiviFormSessionStoreFactory factory = new CiviFormSessionStoreFactory(createConfig(secret));

    byte[] legacyKey = CiviFormSessionStoreFactory.deriveLegacyAesKey(secret);

    // Mimic what the old Shiro-based session store would have done to create
    // the cookie value.
    Map<String, Object> values = new HashMap<>();
    values.put("test-key", "test-value");

    JavaSerializer serializer = new JavaSerializer();
    byte[] serialized = serializer.serializeToBytes(values);
    byte[] compressed = PlayCookieSessionStore.compressBytes(serialized);

    AesCipherService cipherService = new AesCipherService();
    byte[] legacyEncrypted = cipherService.encrypt(compressed, legacyKey).getBytes();
    String cookieValue = Base64.getEncoder().encodeToString(legacyEncrypted);

    Request request = fakeRequestBuilder().addSessionValue("pac4j", cookieValue).build();
    PlayWebContext context = new PlayWebContext(request);

    SessionStore store = factory.newSessionStore();

    assertThat(store.get(context, "test-key")).contains("test-value");
  }
}
