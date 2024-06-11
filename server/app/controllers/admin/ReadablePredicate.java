package controllers.admin;

import com.google.auto.value.AutoValue;
import com.google.common.collect.ImmutableList;
import java.util.Optional;

/**
 * Represents a predicate (eligibility or visibility condition) in a human-readable form for admins.
 */
@AutoValue
public abstract class ReadablePredicate {
  public static ReadablePredicate create(
      String heading, Optional<ImmutableList<String>> conditionList) {
    return new AutoValue_ReadablePredicate(heading, conditionList);
  }

  /**
   * The main heading for the predicate. This could describe the whole predicate, or could just be a
   * preface sentence that explains the {@link #conditionList()}.
   */
  public abstract String heading();

  /**
   * An optional list of conditions required for this predicate.
   *
   * <p>Only present for predicates with multiple AND statements, like "(household size is 2 AND
   * income is <= $20) OR (household size is 3 AND income is <= $30)". Each AND statement will be a
   * separate entry in the returned conditions list, so that example would be a list with two
   * entries: ["household size is 2 and income is <= $20", "household size is 3 and income is <=
   * $30"].
   */
  public abstract Optional<ImmutableList<String>> conditionList();
}
