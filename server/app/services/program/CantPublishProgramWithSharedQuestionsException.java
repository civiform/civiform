package services.program;

public class CantPublishProgramWithSharedQuestionsException extends Exception {
  public CantPublishProgramWithSharedQuestionsException() {
    super("Program contains shared draft questions and cannot be published individually.");
  }

  /** Returns an error message suitable for displaying to an end-user. */
  public String userFacingMessage() {
    // Message doesn't contain any private info.
    return this.getMessage();
  }
}
