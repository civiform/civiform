package services.question.exceptions;

import services.Path;

public final class InvalidPathException extends Exception {
  public InvalidPathException(Path pathName) {
    super("Path not found: " + pathName);
  }
}
