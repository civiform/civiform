package models;

public class ConcurrentUpdateException extends RuntimeException {
  public ConcurrentUpdateException(String message) {
    super(message);
  }
}
