package mapping.admin.migration;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.Test;
import views.admin.migration.AdminImportErrorListPartialViewModel;

public final class AdminImportErrorListPartialMapperTest {

  private final AdminImportErrorListPartialMapper mapper = new AdminImportErrorListPartialMapper();

  @Test
  public void map_setsTitle() {
    AdminImportErrorListPartialViewModel result =
        mapper.map("One or more question errors occured:", "An error.");

    assertThat(result.getTitle()).isEqualTo("One or more question errors occured:");
  }

  @Test
  public void map_allowsNullTitle() {
    AdminImportErrorListPartialViewModel result = mapper.map(/* title= */ null, "An error.");

    assertThat(result.getTitle()).isNull();
  }

  @Test
  public void map_splitsErrorMessageOnSentenceBoundaries() {
    AdminImportErrorListPartialViewModel result =
        mapper.map("title", "First error. Second error. Third error has no final period");

    assertThat(result.getErrorLines())
        .containsExactly("First error", "Second error", "Third error has no final period");
  }

  @Test
  public void map_keepsFinalPeriodOfLastSentence() {
    AdminImportErrorListPartialViewModel result = mapper.map("title", "First error. Second error.");

    assertThat(result.getErrorLines()).containsExactly("First error", "Second error.");
  }

  @Test
  public void map_dropsBlankLines() {
    AdminImportErrorListPartialViewModel result = mapper.map("title", "First error. Second. ");

    assertThat(result.getErrorLines()).containsExactly("First error", "Second");
  }

  @Test
  public void map_setsTryAgainUrl() {
    AdminImportErrorListPartialViewModel result = mapper.map("title", "An error.");

    assertThat(result.getTryAgainUrl()).isEqualTo("/admin/import");
  }
}
