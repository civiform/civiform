package models;

public final class ConcurrentUpdateException extends RuntimeException {
  public ConcurrentUpdateException(String message) {
    super(message);
  }
}
