package mapping.admin.tools;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.Before;
import org.junit.Test;
import views.admin.tools.UrlCheckerViewModel;

public final class UrlCheckerMapperTest {

  private UrlCheckerMapper mapper;

  @Before
  public void setup() {
    mapper = new UrlCheckerMapper();
  }

  @Test
  public void map_returnsViewModel() {
    UrlCheckerViewModel result = mapper.map();

    assertThat(result).isNotNull();
    assertThat(result.testUrlEndpoint()).isNotEmpty();
  }
}
