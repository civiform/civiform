package services.apikey;

import java.util.Optional;
import models.ApiKey;
import play.data.DynamicForm;

/**
 * Holds state relevant to the result of attempting to create an {@link ApiKey}.
 *
 * <p>If the creation attempt was successful, contains an {@link ApiKey} and the base64 encoded
 * credentials string that allows an API consumer to use the key. Attempting to access either of
 * these values when {@code isSuccessful()} is false will throw a runtime exception.
 *
 * <p>If the creation attempt was not successful, contains a {@link DynamicForm} object that holds
 * validation error messages. Attempting to access the form wen {@code isSuccessful()} is true will
 * throw a runtime exception.
 */
public class ApiKeyCreationResult {
  private final Optional<ApiKey> apiKey;
  private final Optional<String> credentials;
  private final Optional<DynamicForm> form;

  /** Constructs an instance in the case of success. */
  public static ApiKeyCreationResult success(ApiKey apiKey, String credentials) {
    return new ApiKeyCreationResult(
        Optional.of(apiKey), Optional.of(credentials), /* form= */ Optional.empty());
  }

  /** Constructs an instance in the case of failure. */
  public static ApiKeyCreationResult failure(DynamicForm form) {
    return new ApiKeyCreationResult(
        /* apiKey= */ Optional.empty(), /* credentials= */ Optional.empty(), Optional.of(form));
  }

  private ApiKeyCreationResult(
      Optional<ApiKey> apiKey, Optional<String> credentials, Optional<DynamicForm> form) {
    this.apiKey = apiKey;
    this.credentials = credentials;
    this.form = form;
  }

  /** Returns true if the key was created. */
  public boolean isSuccessful() {
    return credentials.isPresent();
  }

  /** Returns the API key if creation was successful. */
  public ApiKey getApiKey() {
    return apiKey.get();
  }

  /** Returns the form with validation errors if creation was not successful. */
  public DynamicForm getForm() {
    return form.get();
  }

  /** Returns the base64 encoded credentials string if creation was successful. */
  public String getCredentials() {
    return credentials.get();
  }
}
