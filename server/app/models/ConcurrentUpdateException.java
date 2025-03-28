package models;

public class ConcurrentUpdateException extends RuntimeException {
  ConcurrentUpdateException(String message) {
    super(message);
  }
}
