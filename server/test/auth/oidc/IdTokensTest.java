package auth.oidc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import com.nimbusds.jwt.JWT;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.JWTParser;
import com.nimbusds.jwt.PlainJWT;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Date;
import org.junit.Test;

public class IdTokensTest {

  JWT getJwtWithExpiration(int expTimeInSeconds) {
    JWTClaimsSet claims =
        new JWTClaimsSet.Builder()
            .expirationTime(Date.from(Instant.ofEpochSecond(expTimeInSeconds)))
            .build();
    return new PlainJWT(claims);
  }

  @Test
  public void testStoreAndRemove() {
    IdTokens idTokens = new IdTokens();

    // We can use fake values for the tokens since these operations don't need to parse them.
    idTokens.storeIdToken("session1", "token1");
    idTokens.storeIdToken("session2", "token2");
    assertThat(idTokens.getIdToken("session1")).hasValue("token1");
    assertThat(idTokens.getIdToken("session2")).hasValue("token2");

    idTokens.removeIdToken("session1");
    assertThat(idTokens.getIdToken("session1")).isEmpty();
    assertThat(idTokens.getIdToken("session2")).hasValue("token2");
  }

  @Test
  public void testPurgeExpiredTokens() {
    Clock clock = Clock.fixed(Instant.ofEpochSecond(100), ZoneOffset.UTC);
    IdTokens idTokens = new IdTokens();

    idTokens.storeIdToken("session1", getJwtWithExpiration(90).serialize());
    idTokens.storeIdToken("session2", getJwtWithExpiration(99).serialize());
    idTokens.storeIdToken("session3", getJwtWithExpiration(101).serialize());

    assertThat(idTokens.getIdToken("session1")).isNotEmpty();
    assertThat(idTokens.getIdToken("session2")).isNotEmpty();
    assertThat(idTokens.getIdToken("session3")).isNotEmpty();

    idTokens.purgeExpiredIdTokens(clock);
    assertThat(idTokens.getIdToken("session1")).isEmpty();
    assertThat(idTokens.getIdToken("session2")).isEmpty();
    assertThat(idTokens.getIdToken("session3")).isNotEmpty();
    assertThatCode(() -> JWTParser.parse(idTokens.getIdToken("session3").get()))
        .doesNotThrowAnyException();
  }
}
