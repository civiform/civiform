package views.admin.apikeys;

import lombok.Builder;
import lombok.Data;
import views.BaseViewModel;

/** ViewModel for the page shown once, right after an API key is created (Thymeleaf). */
@Data
@Builder
public final class ApiKeyCredentialsPageViewModel implements BaseViewModel {

  // Page heading, e.g. "Created API key: my-key"
  private final String title;

  // The base64 encoded "<key id>:<key secret>" token
  private final String encodedCredentials;
  private final String keyId;
  private final String keySecret;
}
