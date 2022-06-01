package auth;

import models.ApiKey;

/**
 * Thrown when an API request is authenticated but not authorized for the resource it requested.
 * This class is final to ensure that it works with {@link controllers.ErrorHandler} and subclasses
 * aren't thrown with the same expected behavior.
 */
public final class UnauthorizedApiRequestException extends RuntimeException {

  public UnauthorizedApiRequestException(ApiKey apiKey, String resourceIdentifier) {
    super(
        String.format(
            "API key %s does not have access to %s", apiKey.getKeyId(), resourceIdentifier));
  }
}
