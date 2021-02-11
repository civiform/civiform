package services.program;

import org.junit.Test;

public class PredicateTest {

  @Test
  public void createPredicate() {
    Predicate.create("x < y");
  }
}
