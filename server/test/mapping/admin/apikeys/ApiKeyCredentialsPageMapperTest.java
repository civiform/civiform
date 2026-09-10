package mapping.admin.apikeys;

import static org.assertj.core.api.Assertions.assertThat;

import models.ApiKeyModel;
import org.junit.Before;
import org.junit.Test;
import views.admin.apikeys.ApiKeyCredentialsPageViewModel;

public final class ApiKeyCredentialsPageMapperTest {

  private ApiKeyCredentialsPageMapper mapper;

  @Before
  public void setup() {
    mapper = new ApiKeyCredentialsPageMapper();
  }

  private ApiKeyCredentialsPageViewModel map() {
    ApiKeyModel apiKey = new ApiKeyModel().setName("Test API key");

    return mapper.map(apiKey, "encoded-credentials", "key-id", "key-secret");
  }

  @Test
  public void map_setsTitleFromKeyName() {
    assertThat(map().getTitle()).isEqualTo("Created API key: Test API key");
  }

  @Test
  public void map_setsCredentials() {
    ApiKeyCredentialsPageViewModel result = map();

    assertThat(result.getEncodedCredentials()).isEqualTo("encoded-credentials");
    assertThat(result.getKeyId()).isEqualTo("key-id");
    assertThat(result.getKeySecret()).isEqualTo("key-secret");
  }
}
