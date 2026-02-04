package auth.oidc;

import com.google.auto.value.AutoValue;
import com.google.common.collect.ImmutableList;
import java.util.Optional;

/**
 * Holds the string names for standard claims attribute names to allow for passing around easier.
 * Since some different OIDC system tend to have slightly different naming in cases this will be
 * populated by the individual auth client provider.
 */
@AutoValue
public abstract class StandardClaimsAttributeNames {
  public static StandardClaimsAttributeNames.Builder builder() {
    return new AutoValue_StandardClaimsAttributeNames.Builder();
  }

  public abstract String email();

  public abstract Optional<String> locale();

  public abstract ImmutableList<String> names();

  public abstract Optional<String> phoneNumber();

  @AutoValue.Builder
  public abstract static class Builder {
    public abstract Builder setEmail(String email);

    public abstract Builder setLocale(Optional<String> locale);

    public abstract Builder setNames(ImmutableList<String> names);

    public abstract Builder setPhoneNumber(Optional<String> phoneNumber);

    public abstract StandardClaimsAttributeNames build();
  }
}
