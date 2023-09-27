package auth;

import static org.assertj.core.api.Assertions.assertThat;

import com.nimbusds.jwt.JWT;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.PlainJWT;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import org.junit.Test;

public class IdTokensTest {

  JWT getJwt(int expTimeInSeconds) {
    JWTClaimsSet claims =
        new JWTClaimsSet.Builder()
            .expirationTime(Date.from(Instant.ofEpochSecond(expTimeInSeconds)))
            .build();
    return new PlainJWT(claims);
  }

  @Test
  public void testStoreAndRemove() {
    Clock clock = Clock.fixed(Instant.ofEpochSecond(100), ZoneOffset.UTC);
    Map<String, String> idTokenMap = new HashMap<>();
    IdTokens idTokens = new IdTokens(clock, idTokenMap);

    // We can use fake values for the tokens since these operations don't need to parse them.
    idTokens.storeIdToken("s1", "token1");
    idTokens.storeIdToken("s2", "token2");
    assertThat(idTokens.getIdToken("s1")).hasValue("token1");
    assertThat(idTokens.getIdToken("s2")).hasValue("token2");

    idTokens.removeIdToken("s1");
    assertThat(idTokens.getIdToken("s1")).isEmpty();
    assertThat(idTokens.getIdToken("s2")).hasValue("token2");
  }

  @Test
  public void testPurgeExpiredTokens() {
    Clock clock = Clock.fixed(Instant.ofEpochSecond(100), ZoneOffset.UTC);

    Map<String, String> idTokenMap = new HashMap<>();
    idTokenMap.put("s1", getJwt(99).serialize());
    idTokenMap.put("s2", getJwt(100).serialize());
    idTokenMap.put("s3", getJwt(101).serialize());
    IdTokens idTokens = new IdTokens(clock, idTokenMap);

    assertThat(idTokens.getIdToken("s1")).isNotEmpty();
    assertThat(idTokens.getIdToken("s2")).isNotEmpty();
    assertThat(idTokens.getIdToken("s3")).isNotEmpty();

    idTokens.purgeExpiredIdTokens();
    assertThat(idTokens.getIdToken("s1")).isEmpty();
    assertThat(idTokens.getIdToken("s2")).isEmpty();
    assertThat(idTokens.getIdToken("s3")).isNotEmpty();
  }
}
