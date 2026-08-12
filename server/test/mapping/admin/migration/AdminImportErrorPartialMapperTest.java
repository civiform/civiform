package mapping.admin.migration;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.Test;
import views.admin.migration.AdminImportErrorPartialViewModel;

public final class AdminImportErrorPartialMapperTest {

  private final AdminImportErrorPartialMapper mapper = new AdminImportErrorPartialMapper();

  @Test
  public void map_setsTitle() {
    AdminImportErrorPartialViewModel result = mapper.map("Error processing JSON", "bad json");

    assertThat(result.getTitle()).isEqualTo("Error processing JSON");
  }

  @Test
  public void map_setsErrorMessage() {
    AdminImportErrorPartialViewModel result = mapper.map("Error processing JSON", "bad json");

    assertThat(result.getErrorMessage()).isEqualTo("bad json");
  }

  @Test
  public void map_setsTryAgainUrl() {
    AdminImportErrorPartialViewModel result = mapper.map("Error processing JSON", "bad json");

    assertThat(result.getTryAgainUrl()).isEqualTo("/admin/import");
  }
}
