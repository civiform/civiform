package mapping.admin.migration;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.Test;
import views.admin.migration.AdminImportProgramSavedPartialViewModel;

public final class AdminImportProgramSavedPartialMapperTest {

  private final AdminImportProgramSavedPartialMapper mapper =
      new AdminImportProgramSavedPartialMapper();

  @Test
  public void map_setsProgramName() {
    AdminImportProgramSavedPartialViewModel result = mapper.map("my-program", 5L);

    assertThat(result.getProgramName()).isEqualTo("my-program");
  }

  @Test
  public void map_setsViewProgramUrl() {
    AdminImportProgramSavedPartialViewModel result = mapper.map("my-program", 5L);

    assertThat(result.getViewProgramUrl()).isEqualTo("/admin/programs/5/blocks");
  }

  @Test
  public void map_setsImportAnotherProgramUrl() {
    AdminImportProgramSavedPartialViewModel result = mapper.map("my-program", 5L);

    assertThat(result.getImportAnotherProgramUrl()).isEqualTo("/admin/import");
  }
}
