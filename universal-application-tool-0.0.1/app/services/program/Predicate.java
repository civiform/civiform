package services.program;

import com.google.auto.value.AutoValue;
import com.google.common.collect.ImmutableSet;

/** String representation of a predicate with variables from the core data model. */
@AutoValue
abstract class Predicate {
  static Predicate create(String function, ImmutableSet<String> variables) {
    return new AutoValue_Predicate(function, variables);
  }

  abstract String function();

  abstract ImmutableSet<String> variables();
}
