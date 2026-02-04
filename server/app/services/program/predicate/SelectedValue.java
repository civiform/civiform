package services.program.predicate;

import com.google.auto.value.AutoOneOf;
import com.google.common.collect.ImmutableSet;

// Represents a user-entered value (or set of values) in the admin expanded predicate view.
@AutoOneOf(SelectedValue.Kind.class)
public abstract class SelectedValue {
  public record ValuePair(String first, String second) {}

  public enum Kind {
    SINGLE,
    PAIR,
    MULTIPLE
  }

  public abstract Kind getKind();

  public abstract String single();

  public abstract ValuePair pair();

  public abstract ImmutableSet<String> multiple();

  public static SelectedValue single(String s) {
    return AutoOneOf_SelectedValue.single(s);
  }

  public static SelectedValue pair(ValuePair s) {
    return AutoOneOf_SelectedValue.pair(s);
  }

  public static SelectedValue multiple(ImmutableSet<String> s) {
    return AutoOneOf_SelectedValue.multiple(s);
  }
}
