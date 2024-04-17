package services.apikey;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Optional;
import models.ApiKeyModel;
import play.data.DynamicForm;

/**
 * Holds state relevant to the result of attempting to create an {@link ApiKeyModel}.
 *
 * <p>If the creation attempt was successful, contains an {@link ApiKeyModel} and the base64 encoded
 * credentials string that allows an API consumer to use the key. Attempting to access either of
 * these values when {@code isSuccessful()} is false will throw a runtime exception.
 *
 * <p>If the creation attempt was not successful, may contain a general errorMessage (for errors
 * which aren't related to a specific form field) and a {@link DynamicForm} object that holds
 * validation error messages specific to fields. Attempting to access the form when {@code
 * isSuccessful()} is true will throw a runtime exception.
 */
public final class ApiKeyCreationResult {
  private final Optional<ApiKeyModel> apiKey;
  private final Optional<String> keyId;
  private final Optional<String> keySecret;
  private final Optional<DynamicForm> form;
  private final Optional<String> errorMessage;

  /** Constructs an instance in the case of success. */
  public static ApiKeyCreationResult success(ApiKeyModel apiKey, String keyId, String keySecret) {
    return new ApiKeyCreationResult(
        Optional.of(apiKey),
        Optional.of(keyId),
        Optional.of(keySecret),
        /* form= */ Optional.empty(),
        /* errorMessage= */ Optional.empty());
  }

  /** Constructs an instance in the case of failure. */
  public static ApiKeyCreationResult failure(DynamicForm form, Optional<String> errorMessage) {
    return new ApiKeyCreationResult(
        /* apiKey= */ Optional.empty(),
        /* keyId= */ Optional.empty(),
        /* keySecret= */ Optional.empty(),
        Optional.of(form),
        errorMessage);
  }

  private ApiKeyCreationResult(
      Optional<ApiKeyModel> apiKey,
      Optional<String> keyId,
      Optional<String> keySecret,
      Optional<DynamicForm> form,
      Optional<String> errorMessage) {
    this.apiKey = apiKey;
    this.keyId = keyId;
    this.keySecret = keySecret;
    this.form = form;
    this.errorMessage = errorMessage;
  }

  /** Returns true if the key was created. */
  public boolean isSuccessful() {
    return keySecret.isPresent() && keyId.isPresent();
  }

  /** Returns the API key if creation was successful. */
  public ApiKeyModel getApiKey() {
    return apiKey.get();
  }

  /** Returns the form with validation errors if creation was not successful. */
  public DynamicForm getForm() {
    return form.get();
  }

  public Optional<String> getErrorMessage() {
    return errorMessage;
  }

  /** Returns the base64 encoded credentials string if creation was successful. */
  public String getEncodedCredentials() {
    return Base64.getEncoder()
        .encodeToString(
            String.format("%s:%s", keyId.get(), keySecret.get()).getBytes(StandardCharsets.UTF_8));
  }

  /** Returns the keyId string if creation was successful. */
  public String getKeyId() {
    return keyId.get();
  }

  /** Returns the keySecret string if creation was successful. */
  public String getKeySecret() {
    return keySecret.get();
  }
}
