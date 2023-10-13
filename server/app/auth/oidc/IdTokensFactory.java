package auth.oidc;

import java.time.Clock;
import javax.inject.Inject;

/** Factory for constructing IdTokens instances. */
public final class IdTokensFactory {
  private Clock clock;

  // This class is required because SerializedIdTokens instances are constructed by Jackson, so we
  // cannot inject a Clock. Instead, we inject the clock into the factory and then wrap
  // SerializedIdTokens in a class with domain-specific logic.
  @Inject
  public IdTokensFactory(Clock clock) {
    this.clock = clock;
  }

  public IdTokens create(SerializedIdTokens serializedIdTokens) {
    return new IdTokens(clock, serializedIdTokens);
  }

  public IdTokens create() {
    return create(new SerializedIdTokens());
  }
}
