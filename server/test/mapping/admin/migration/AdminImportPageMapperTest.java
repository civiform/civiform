package mapping.admin.migration;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.Test;
import views.admin.migration.AdminImportPageViewModel;

public final class AdminImportPageMapperTest {

  private final AdminImportPageMapper mapper = new AdminImportPageMapper();

  @Test
  public void map_setsBackUrl() {
    AdminImportPageViewModel result = mapper.map();

    assertThat(result.getBackUrl()).isEqualTo("/admin/programs");
  }

  @Test
  public void map_setsHxImportProgramUrl() {
    AdminImportPageViewModel result = mapper.map();

    assertThat(result.getHxImportProgramUrl()).isEqualTo("/admin/import/hx/program");
  }

  @Test
  public void randomFieldId_generatesEightAlphabeticCharacters() {
    AdminImportPageViewModel result = mapper.map();

    assertThat(result.randomFieldId()).matches("[a-zA-Z]{8}");
  }
}
