package mapping.admin.apikeys;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.Before;
import org.junit.Test;
import views.admin.apikeys.ApiKeyCredentialsPageViewModel;

public final class ApiKeyCredentialsPageMapperTest {

  private ApiKeyCredentialsPageMapper mapper;

  @Before
  public void setup() {
    mapper = new ApiKeyCredentialsPageMapper();
  }

  @Test
  public void map_setsAllFields() {
    ApiKeyCredentialsPageViewModel result =
        mapper.map("Test Key", "encoded-creds", "key-id-123", "key-secret-456");

    assertThat(result.getKeyName()).isEqualTo("Test Key");
    assertThat(result.getEncodedCredentials()).isEqualTo("encoded-creds");
    assertThat(result.getKeyId()).isEqualTo("key-id-123");
    assertThat(result.getKeySecret()).isEqualTo("key-secret-456");
  }
}
