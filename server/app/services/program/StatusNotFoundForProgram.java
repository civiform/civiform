package services.program;

public class StatusNotFoundForProgram extends Exception {
  public StatusNotFoundForProgram(String programName) {
    super(String.format("status (%s) is not valid for program id %d", programName));
  }
}
