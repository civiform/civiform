package controllers.admin;

import com.google.auto.value.AutoValue;
import com.google.common.collect.ImmutableList;
import j2html.tags.UnescapedText;
import java.util.Optional;

/**
 * Represents a predicate (eligibility or visibility condition) in a human-readable form for admins.
 */
@AutoValue
public abstract class ReadablePredicate {
  public static ReadablePredicate create(
      String heading,
      UnescapedText formattedHtmlHeading,
      Optional<ImmutableList<String>> conditionList,
      Optional<ImmutableList<UnescapedText>> formattedHtmlConditionList) {
    return new AutoValue_ReadablePredicate(
        heading, formattedHtmlHeading, conditionList, formattedHtmlConditionList);
  }

  /**
   * The main heading for the predicate as a string. This should only be used when a plain string
   * representation is necessary, such as for PDF Export. Most user-facing application use cases
   * should use {@link #formattedHtmlHeading()} instead. This could describe the whole predicate, or
   * could just be a preface sentence that explains the {@link #conditionList()}.
   */
  public abstract String heading();

  /**
   * The formatted main heading for the predicate in HTML. Identical to {@link #heading()} with the
   * exception that it may have additional HTML formatting such as <strong> tags to emphasize
   * certain parts of the predicate.
   */
  public abstract UnescapedText formattedHtmlHeading();

  /**
   * An optional list of conditions required for this predicate.
   *
   * <p>This should only be used when a plain string representation is necessary, such as for PDF
   * Export. Most user-facing application use cases should use {@link #formattedHtmlConditionList()}
   * instead.
   *
   * <p>Only present for predicates with multiple AND statements, like "(household size is 2 AND
   * income is <= $20) OR (household size is 3 AND income is <= $30)". Each AND statement will be a
   * separate entry in the returned conditions list, so that example would be a list with two
   * entries: ["household size is 2 and income is <= $20", "household size is 3 and income is <=
   * $30"].
   */
  public abstract Optional<ImmutableList<String>> conditionList();

  /**
   * An optional list of formatted conditions required for this predicate. Identical to {@link
   * #conditionList()} with the exception that conditions may have additional HTML formatting such
   * as <strong> tags to emphasize certain parts of the condition.
   */
  public abstract Optional<ImmutableList<UnescapedText>> formattedHtmlConditionList();
}
