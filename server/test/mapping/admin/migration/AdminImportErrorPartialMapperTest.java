package mapping.admin.migration;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.Test;
import views.admin.migration.AdminImportErrorPartialViewModel;

public final class AdminImportErrorPartialMapperTest {

  private final AdminImportErrorPartialMapper mapper = new AdminImportErrorPartialMapper();

  @Test
  public void map_setsTitleAndSingleLine() {
    AdminImportErrorPartialViewModel result =
        mapper.map("Error processing JSON", "First error. Second error.");

    assertThat(result.getTitle()).isEqualTo("Error processing JSON");
    assertThat(result.getErrorLines()).containsExactly("First error. Second error.");
  }

  @Test
  public void mapWithLineBreaks_setsTitle() {
    AdminImportErrorPartialViewModel result =
        mapper.mapWithLineBreaks("One or more question errors occured:", "An error.");

    assertThat(result.getTitle()).isEqualTo("One or more question errors occured:");
  }

  @Test
  public void mapWithLineBreaks_allowsNullTitle() {
    AdminImportErrorPartialViewModel result =
        mapper.mapWithLineBreaks(/* title= */ null, "An error.");

    assertThat(result.getTitle()).isNull();
  }

  @Test
  public void mapWithLineBreaks_splitsErrorMessageOnSentenceBoundaries() {
    AdminImportErrorPartialViewModel result =
        mapper.mapWithLineBreaks(
            "title", "First error. Second error. Third error has no final period");

    assertThat(result.getErrorLines())
        .containsExactly("First error", "Second error", "Third error has no final period");
  }

  @Test
  public void mapWithLineBreaks_keepsFinalPeriodOfLastSentence() {
    AdminImportErrorPartialViewModel result =
        mapper.mapWithLineBreaks("title", "First error. Second error.");

    assertThat(result.getErrorLines()).containsExactly("First error", "Second error.");
  }

  @Test
  public void mapWithLineBreaks_dropsBlankLines() {
    AdminImportErrorPartialViewModel result =
        mapper.mapWithLineBreaks("title", "First error. Second. ");

    assertThat(result.getErrorLines()).containsExactly("First error", "Second");
  }
}
