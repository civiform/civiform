package services.program;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.auto.value.AutoValue;
import services.program.predicate.PredicateDefinition;

/**
 * Defines eligibility configuration on questions in a program.
 *
 * <p>Eligibility rules use the {@link PredicateDefinition} format, and indicate when an applicant
 * is eligible to submit an application based on their answers so far.
 *
 * <p>The {@link PredicateDefinition} condition structure is identical to how visibility predicate
 * data is handled.
 */
@JsonDeserialize(builder = AutoValue_EligibilityDefinition.Builder.class)
@AutoValue
public abstract class EligibilityDefinition {
  // Note: While there is only one member currently, future iterations will add more which requires
  // a wrapping class.

  public static Builder builder() {
    return new AutoValue_EligibilityDefinition.Builder();
  }

  /** A {@link PredicateDefinition} that determines whether the applicant is eligible. */
  @JsonProperty("predicate")
  public abstract PredicateDefinition predicate();

  public abstract Builder toBuilder();

  @AutoValue.Builder
  public abstract static class Builder {
    public abstract EligibilityDefinition build();

    @JsonProperty("predicate")
    public abstract Builder setPredicate(PredicateDefinition def);
  }
}
