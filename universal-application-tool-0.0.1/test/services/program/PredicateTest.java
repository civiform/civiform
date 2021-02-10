package services.program;

import com.google.common.collect.ImmutableSet;
import org.junit.Test;

public class PredicateTest {

  @Test
  public void createPredicate() {
    ImmutableSet<String> variables = ImmutableSet.of("x", "y");
    Predicate.create("x < y", variables);
  }
}
