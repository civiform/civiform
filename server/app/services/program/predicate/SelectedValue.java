package services.program.predicate;

import com.google.auto.value.AutoOneOf;
import com.google.common.collect.ImmutableSet;

// Represents a user-entered value (or set of values) in the admin expanded predicate view.
@AutoOneOf(SelectedValue.Kind.class)
public abstract class SelectedValue {
  public enum Kind {
    SINGLE,
    MULTIPLE
  }

  public abstract Kind getKind();

  public abstract String single();

  public abstract ImmutableSet<String> multiple();

  public static SelectedValue single(String s) {
    return AutoOneOf_SelectedValue.single(s);
  }

  public static SelectedValue multiple(ImmutableSet<String> s) {
    return AutoOneOf_SelectedValue.multiple(s);
  }
}
