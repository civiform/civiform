package services.apikey;

/**
 * ApiKeyNotFoundException is thrown when an ApiKey cannot be found by the specified identifier.
 * This class is final to ensure that it works with {@link controllers.ErrorHandler} and subclasses
 * aren't thrown with the same expected behavior.
 */
public final class ApiKeyNotFoundException extends Exception {

  public ApiKeyNotFoundException(Long databaseId) {
    super("ApiKey not found for database ID: " + databaseId);
  }

  public ApiKeyNotFoundException(String keyId) {
    super("ApiKey not found for key ID: " + keyId);
  }
}
