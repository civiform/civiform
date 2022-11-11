package services.program;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.auto.value.AutoValue;
import java.util.Optional;
import services.program.predicate.PredicateDefinition;

/**
 * Defines eligibility data on questions in a program.
 *
 * Eligibility rules use the {@code PredicationDefinition} format, and indicate
 * when an applicant is not eligible to submit an application based on their
 * answers so far.
 *
 * The structure is very similar to show/hide predicate data.
 */
@JsonDeserialize(builder = AutoValue_EligibilityDefinition.Builder.class)
@AutoValue
public abstract class EligibilityDefinition {

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
