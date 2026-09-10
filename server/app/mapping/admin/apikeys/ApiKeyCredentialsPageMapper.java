package mapping.admin.apikeys;

import models.ApiKeyModel;
import views.admin.apikeys.ApiKeyCredentialsPageViewModel;

/** Maps data to the ApiKeyCredentialsPageViewModel for the API key credentials page. */
public final class ApiKeyCredentialsPageMapper {

  /**
   * Maps a newly created API key and its credentials to the view model.
   *
   * @param apiKey the key that was just created
   * @param encodedCredentials the base64 encoded credentials used as a single API token
   * @param keyId the API username
   * @param keySecret the API password, only available at creation time
   */
  public ApiKeyCredentialsPageViewModel map(
      ApiKeyModel apiKey, String encodedCredentials, String keyId, String keySecret) {
    return ApiKeyCredentialsPageViewModel.builder()
        .title("Created API key: " + apiKey.getName())
        .encodedCredentials(encodedCredentials)
        .keyId(keyId)
        .keySecret(keySecret)
        .build();
  }
}
