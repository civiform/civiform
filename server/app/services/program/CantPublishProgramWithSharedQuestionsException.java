package services.program;

/**
 * Exception thrown when attempting to publish an individual draft program but it has draft
 * questions that are shared with other programs. In that case, publishing the questions would
 * affect other programs so this program cannot be published individually.
 */
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
