package auth;

import static org.assertj.core.api.Assertions.assertThat;
import static support.FakeRequestBuilder.fakeRequestBuilder;

import com.google.common.collect.ImmutableMap;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectOutputStream;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import java.util.zip.GZIPInputStream;
import org.junit.Test;
import org.pac4j.core.context.session.SessionStore;
import org.pac4j.play.PlayWebContext;
import org.pac4j.play.store.JdkAesDataEncrypter;
import org.pac4j.play.store.PlayCookieSessionStore;
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
  public void sessionStore_cookieRoundTrip_differentSecret_invalidatesSession() {
    CiviFormSessionStoreFactory factory1 = new CiviFormSessionStoreFactory(createConfig(SECRET1));
    CiviFormSessionStoreFactory factory2 = new CiviFormSessionStoreFactory(createConfig(SECRET2));

    SessionStore store1 = factory1.newSessionStore();
    SessionStore store2 = factory2.newSessionStore();

    PlayWebContext context1 = new PlayWebContext(fakeRequestBuilder().build());
    store1.set(context1, "test-key", "test-value");

    PlayWebContext context2 = newContextWithSessionFrom(context1);

    // A cookie encrypted under a different secret can't be decrypted; the store treats it as
    // no session and drops the unreadable cookie rather than failing the request.
    assertThat(store2.get(context2, "test-key")).isEmpty();
    Request supplemented = context2.supplementRequest(fakeRequestBuilder().build());
    assertThat(supplemented.session().get("pac4j")).isEmpty();
  }

  @Test
  public void newSessionStore_legacyJavaSerializedCookie_isTreatedAsEmptySession()
      throws Exception {
    CiviFormSessionStoreFactory factory = new CiviFormSessionStoreFactory(createConfig(SECRET1));

    // Mimic what a deploy predating the JsonSerializer migration would have written: a
    // Java-serialized value map, correctly encrypted with the modern key. With the fallback
    // JavaSerializer removed, this deserializes to null and reads as an empty session.
    Map<String, Object> values = new HashMap<>();
    values.put("test-key", "test-value");

    ByteArrayOutputStream serialized = new ByteArrayOutputStream();
    try (ObjectOutputStream oos = new ObjectOutputStream(serialized)) {
      oos.writeObject(values);
    }
    byte[] compressed = PlayCookieSessionStore.compressBytes(serialized.toByteArray());

    byte[] aesKey = CiviFormSessionStoreFactory.deriveAes128Key(SECRET1);
    JdkAesDataEncrypter encrypter = new JdkAesDataEncrypter(aesKey);
    byte[] encrypted = encrypter.encrypt(compressed);
    String cookieValue = Base64.getEncoder().encodeToString(encrypted);

    Request request = fakeRequestBuilder().addSessionValue("pac4j", cookieValue).build();
    PlayWebContext context = new PlayWebContext(request);

    SessionStore store = factory.newSessionStore();

    assertThat(store.get(context, "test-key")).isEmpty();
  }

  @Test
  public void newSessionStore_writesJsonNotJavaSerializedBytes() throws Exception {
    CiviFormSessionStoreFactory factory = new CiviFormSessionStoreFactory(createConfig(SECRET1));
    SessionStore store = factory.newSessionStore();

    PlayWebContext context = new PlayWebContext(fakeRequestBuilder().build());
    store.set(context, "test-key", "test-value");

    Request requestWithSession = context.supplementRequest(fakeRequestBuilder().build());
    String cookieValue = requestWithSession.session().get("pac4j").orElseThrow();

    byte[] aesKey = CiviFormSessionStoreFactory.deriveAes128Key(SECRET1);
    JdkAesDataEncrypter encrypter = new JdkAesDataEncrypter(aesKey);
    byte[] encrypted = Base64.getDecoder().decode(cookieValue);
    byte[] compressed = encrypter.decrypt(encrypted);
    byte[] plaintext;
    try (GZIPInputStream gzip = new GZIPInputStream(new ByteArrayInputStream(compressed))) {
      plaintext = gzip.readAllBytes();
    }

    // pac4j's JsonSerializer uses Jackson with DefaultTyping.NON_FINAL, which wraps a
    // Map<String, Object> as a tagged JSON array: ["java.util.HashMap", {...}]. So the first
    // byte is '[' (0x5B). Negative-assert against the Java serialization magic
    // (0xAC 0xED) to guard against accidental reintroduction of JavaSerializer.
    assertThat(plaintext[0]).isEqualTo((byte) '[');
    assertThat(plaintext[0]).isNotEqualTo((byte) 0xAC);
    assertThat(plaintext[1]).isNotEqualTo((byte) 0xED);
  }

  @Test
  public void newSessionStore_roundTripsPopulatedCiviFormProfileData() throws Exception {
    CiviFormSessionStoreFactory factory = new CiviFormSessionStoreFactory(createConfig(SECRET1));
    SessionStore store = factory.newSessionStore();

    CiviFormProfileData profileData = new CiviFormProfileData();
    profileData.setId("42");
    profileData.addAttribute("emailKey", "user@example.com");
    profileData.addAttribute("longAttr", 12345L);
    String uuid = UUID.randomUUID().toString();
    profileData.addAttribute("uuidAttr", uuid);
    profileData.setRoles(Set.of("ROLE_APPLICANT", "ROLE_ADMIN"));
    profileData.setClientName("test-client");
    profileData.setLinkedId("linked-123");

    PlayWebContext context1 = new PlayWebContext(fakeRequestBuilder().build());
    store.set(context1, "profile-key", profileData);

    Request requestWithSession = context1.supplementRequest(fakeRequestBuilder().build());
    String cookieValue = requestWithSession.session().get("pac4j").orElseThrow();

    // Play's cookie size limit is ~4KB. Leave headroom for headers / additional session keys
    // so the JSON-format cookie doesn't push us against the limit once profiles grow.
    assertThat(cookieValue.length()).isLessThan(3500);

    PlayWebContext context2 = new PlayWebContext(requestWithSession);
    Optional<Object> readBack = store.get(context2, "profile-key");
    assertThat(readBack).isPresent();
    CiviFormProfileData restored = (CiviFormProfileData) readBack.get();

    assertThat(restored.getId()).isEqualTo("42");
    assertThat(restored.getAttribute("emailKey")).isEqualTo("user@example.com");
    assertThat(restored.getAttribute("longAttr")).isEqualTo(12345L);
    assertThat(restored.getAttribute("uuidAttr")).isEqualTo(uuid);
    assertThat(restored.getRoles()).containsExactlyInAnyOrder("ROLE_APPLICANT", "ROLE_ADMIN");
    assertThat(restored.getClientName()).isEqualTo("test-client");
    assertThat(restored.getLinkedId()).isEqualTo("linked-123");
  }

  @Test
  public void sessionStore_undecryptableCookie_invalidatesSessionAndDropsCookie() {
    CiviFormSessionStoreFactory factory = new CiviFormSessionStoreFactory(createConfig(SECRET1));

    // Mimic a cookie written by the old Shiro-based encryption scheme (or any cookie the modern
    // key can't decrypt). The length is deliberately not a multiple of the AES block size so
    // decryption is guaranteed to fail rather than depending on the padding check.
    byte[] garbage = new byte[47];
    new Random(42).nextBytes(garbage);
    String cookieValue = Base64.getEncoder().encodeToString(garbage);

    Request request = fakeRequestBuilder().addSessionValue("pac4j", cookieValue).build();
    PlayWebContext context = new PlayWebContext(request);

    SessionStore store = factory.newSessionStore();

    assertThat(store.get(context, "test-key")).isEmpty();
    // The unreadable cookie is removed so it isn't re-processed on every request.
    Request supplemented = context.supplementRequest(fakeRequestBuilder().build());
    assertThat(supplemented.session().get("pac4j")).isEmpty();
  }

  @Test
  public void sessionStore_invalidatedSession_canBeWrittenToAgain() {
    CiviFormSessionStoreFactory factory = new CiviFormSessionStoreFactory(createConfig(SECRET1));

    byte[] garbage = new byte[47];
    new Random(42).nextBytes(garbage);
    String cookieValue = Base64.getEncoder().encodeToString(garbage);

    Request request = fakeRequestBuilder().addSessionValue("pac4j", cookieValue).build();
    PlayWebContext context = new PlayWebContext(request);

    SessionStore store = factory.newSessionStore();

    // A write over an unreadable cookie replaces it with a fresh, readable session.
    store.set(context, "test-key", "test-value");

    PlayWebContext context2 = newContextWithSessionFrom(context);
    assertThat(store.get(context2, "test-key").get()).isEqualTo("test-value");
  }
}
