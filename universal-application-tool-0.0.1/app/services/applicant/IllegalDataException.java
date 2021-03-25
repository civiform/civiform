package services.applicant;

import services.Path;

public class IllegalDataException extends Exception {
  public IllegalDataException(Path path) {
    super("Tried to write bad data at path: " + path);
  }
}
