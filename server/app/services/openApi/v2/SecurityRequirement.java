package services.openApi.v2;

import com.google.auto.value.AutoValue;

/**
 * Lists the required security schemes to execute this operation. The object can have multiple
 * security schemes declared in it which are all required (that is, there is a logical AND between
 * the schemes).
 *
 * <p>The name used for each property MUST correspond to a security scheme declared in the Security
 * Definitions.
 *
 * <p>https://swagger.io/specification/v2/#security-requirement-object
 */
@AutoValue
public abstract class SecurityRequirement {
  public abstract SecurityType getSecurityType();

  public static SecurityRequirement.Builder builder(SecurityType securityType) {
    return new AutoValue_SecurityRequirement.Builder().setSecurityType(securityType);
  }

  @AutoValue.Builder
  public abstract static class Builder {
    protected abstract SecurityRequirement.Builder setSecurityType(SecurityType securityType);

    public abstract SecurityRequirement build();
  }
}
