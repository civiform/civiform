package mapping.admin.migration;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.Before;
import org.junit.Test;
import views.admin.migration.AdminExportPageViewModel;

public final class AdminExportPageMapperTest {

  private static final String PROGRAM_JSON = "{ \"program\": { \"adminName\": \"my-program\" } }";

  private AdminExportPageMapper mapper;

  @Before
  public void setup() {
    mapper = new AdminExportPageMapper();
  }

  @Test
  public void map_setsBackUrl() {
    AdminExportPageViewModel result = mapper.map("my-program", PROGRAM_JSON);

    assertThat(result.getBackUrl()).isEqualTo("/admin/programs");
  }

  @Test
  public void map_setsAdminName() {
    AdminExportPageViewModel result = mapper.map("my-program", PROGRAM_JSON);

    assertThat(result.getAdminName()).isEqualTo("my-program");
  }

  @Test
  public void map_setsProgramJson() {
    AdminExportPageViewModel result = mapper.map("my-program", PROGRAM_JSON);

    assertThat(result.getProgramJson()).isEqualTo(PROGRAM_JSON);
  }

  @Test
  public void map_setsDownloadUrlFromAdminName() {
    AdminExportPageViewModel result = mapper.map("my-program", PROGRAM_JSON);

    assertThat(result.getDownloadUrl()).isEqualTo("/admin/export/download/my-program");
  }

  @Test
  public void randomFieldId_isEightAlphabeticCharacters() {
    AdminExportPageViewModel result = mapper.map("my-program", PROGRAM_JSON);

    assertThat(result.randomFieldId()).matches("[A-Za-z]{8}");
  }
}
