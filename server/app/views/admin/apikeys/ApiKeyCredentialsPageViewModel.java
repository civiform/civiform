package views.admin.apikeys;

import lombok.Builder;
import lombok.Data;
import views.admin.BaseViewModel;

@Data
@Builder
public final class ApiKeyCredentialsPageViewModel implements BaseViewModel {

  private final String keyName;
  private final String encodedCredentials;
  private final String keyId;
  private final String keySecret;
}
