package mapping.admin.migration;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.Before;
import org.junit.Test;
import views.admin.migration.AdminExportPageViewModel;

public final class AdminExportPageMapperTest {

  private AdminExportPageMapper mapper;

  @Before
  public void setup() {
    mapper = new AdminExportPageMapper();
  }

  @Test
  public void map_setsAllFields() {
    AdminExportPageViewModel result = mapper.map("test-program", "{\"json\": true}");

    assertThat(result.getProgramAdminName()).isEqualTo("test-program");
    assertThat(result.getProgramJson()).isEqualTo("{\"json\": true}");
    assertThat(result.getDownloadJsonUrl()).contains("test-program");
    assertThat(result.getBackToProgramsUrl()).isNotEmpty();
  }
}
