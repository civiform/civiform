package auth.oidc;

import static com.google.common.base.Preconditions.checkNotNull;

import java.time.Clock;
import javax.inject.Inject;

/** Factory for constructing {@link IdTokens} instances. */
public final class IdTokensFactory {
  private final Clock clock;

  // This class is required because SerializedIdTokens instances are constructed by Jackson, so we
  // cannot inject a Clock. Instead, we inject the clock into the factory and then wrap
  // SerializedIdTokens in a class with domain-specific logic.
  @Inject
  public IdTokensFactory(Clock clock) {
    this.clock = checkNotNull(clock);
  }

  /*
   * Creates an instance of {@link IdTokens} based on the provided {@link SerializedIdTokens}.
   */
  public IdTokens create(SerializedIdTokens serializedIdTokens) {
    return new IdTokens(clock, serializedIdTokens);
  }
}
