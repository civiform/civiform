package services.program;

import org.junit.Test;
import com.google.common.collect.ImmutableSet;

public class PredicateTest {

  @Test
  public void createPredicate() {
    ImmutableSet<String> variables = ImmutableSet.of("x", "y");
    Predicate.create("x < y", variables);
  }
}
