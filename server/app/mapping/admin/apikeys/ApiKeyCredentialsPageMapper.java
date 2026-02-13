package mapping.admin.apikeys;

import views.admin.apikeys.ApiKeyCredentialsPageViewModel;

/** Maps data to the ApiKeyCredentialsPageViewModel. */
public final class ApiKeyCredentialsPageMapper {

  public ApiKeyCredentialsPageViewModel map(
      String keyName, String encodedCredentials, String keyId, String keySecret) {
    return ApiKeyCredentialsPageViewModel.builder()
        .keyName(keyName)
        .encodedCredentials(encodedCredentials)
        .keyId(keyId)
        .keySecret(keySecret)
        .build();
  }
}
