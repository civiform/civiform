package mapping.admin.migration;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.Before;
import org.junit.Test;
import views.admin.migration.AdminImportPageViewModel;

public final class AdminImportPageMapperTest {

  private AdminImportPageMapper mapper;

  @Before
  public void setup() {
    mapper = new AdminImportPageMapper();
  }

  @Test
  public void map_setsMaxTextLength() {
    AdminImportPageViewModel result = mapper.map(512000);

    assertThat(result.getMaxTextLength()).isEqualTo(512000);
  }

  @Test
  public void map_setsUrls() {
    AdminImportPageViewModel result = mapper.map(100);

    assertThat(result.getBackToProgramsUrl()).isNotEmpty();
    assertThat(result.getHxImportProgramUrl()).isNotEmpty();
  }
}
