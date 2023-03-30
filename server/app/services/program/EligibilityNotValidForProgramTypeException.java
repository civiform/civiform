package services.program;

/**
 * EligibilityNotValidForProgramType is thrown when eligibility predicates are attempted to be added
 * to a Program that has a ProgramType that cannot have eligibility conditions.
 */
public class EligibilityNotValidForProgramTypeException extends Exception {

  public EligibilityNotValidForProgramTypeException(ProgramType programType) {
    super(
        String.format(
            "Eligibility conditions cannot be set for ProgramType %s", programType.toString()));
  }
}
