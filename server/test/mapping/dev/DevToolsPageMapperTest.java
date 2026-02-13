package mapping.dev;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.Before;
import org.junit.Test;
import views.dev.DevToolsPageViewModel;

public final class DevToolsPageMapperTest {

  private DevToolsPageMapper mapper;

  @Before
  public void setup() {
    mapper = new DevToolsPageMapper();
  }

  @Test
  public void map_setsIsDev() {
    DevToolsPageViewModel result = mapper.map(true, "csrf-token");

    assertThat(result.isDev()).isTrue();
  }

  @Test
  public void map_setsCsrfToken() {
    DevToolsPageViewModel result = mapper.map(false, "my-csrf-token");

    assertThat(result.getCsrfToken()).isEqualTo("my-csrf-token");
  }

  @Test
  public void map_setsUrls() {
    DevToolsPageViewModel result = mapper.map(false, "token");

    assertThat(result.getSeedProgramsUrl()).isNotEmpty();
    assertThat(result.getSeedQuestionsUrl()).isNotEmpty();
    assertThat(result.getClearUrl()).isNotEmpty();
    assertThat(result.getClearCacheUrl()).isNotEmpty();
    assertThat(result.getRunDurableJobUrl()).isNotEmpty();
    assertThat(result.getIconsUrl()).isNotEmpty();
    assertThat(result.getHomeUrl()).isNotEmpty();
  }

  @Test
  public void map_setsDurableJobOptions() {
    DevToolsPageViewModel result = mapper.map(false, "token");

    assertThat(result.getDurableJobOptions()).isNotEmpty();
  }
}
