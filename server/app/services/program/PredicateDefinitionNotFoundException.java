package services.program;

/**
 * PredicateDefinitionNotFoundException is thrown when the specified predicate definition is not
 * found in this block of this program.
 */
public class PredicateDefinitionNotFoundException extends Exception {
  public PredicateDefinitionNotFoundException(
      long programId, long blockDefinitionId, long predicateDefinitionId) {
    super(
        "Predicate "
            + predicateDefinitionId
            + " not found in Program (ID "
            + programId
            + ") for block definition ID "
            + blockDefinitionId);
  }
}
