package mapping.admin.apikeys;

import static org.assertj.core.api.Assertions.assertThat;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import services.DateConverter;
import views.admin.apikeys.ApiKeyIndexPageViewModel;

public final class ApiKeyIndexPageMapperTest {

  private ApiKeyIndexPageMapper mapper;
  private DateConverter dateConverter;

  @Before
  public void setup() {
    mapper = new ApiKeyIndexPageMapper();
    dateConverter = Mockito.mock(DateConverter.class);
  }

  @Test
  public void map_setsSelectedStatus() {
    ApiKeyIndexPageViewModel result =
        mapper.map("Active", ImmutableList.of(), ImmutableSet.of(), dateConverter);

    assertThat(result.getSelectedStatus()).isEqualTo("Active");
  }

  @Test
  public void map_setsUrls() {
    ApiKeyIndexPageViewModel result =
        mapper.map("Active", ImmutableList.of(), ImmutableSet.of(), dateConverter);

    assertThat(result.getNewKeyUrl()).isNotEmpty();
    assertThat(result.getActiveUrl()).isNotEmpty();
    assertThat(result.getRetiredUrl()).isNotEmpty();
    assertThat(result.getExpiredUrl()).isNotEmpty();
  }

  @Test
  public void map_withNoApiKeys_returnsEmptyList() {
    ApiKeyIndexPageViewModel result =
        mapper.map("Active", ImmutableList.of(), ImmutableSet.of(), dateConverter);

    assertThat(result.getApiKeys()).isEmpty();
  }
}
