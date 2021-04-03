package services.applicant;

import static org.assertj.core.api.Assertions.assertThat;

import com.google.common.collect.ImmutableMap;
import org.junit.Test;
import services.Path;

public class UpdatesTest {

  @Test
  public void generateUpdates_emptyMap_returnsEmptySet() {
    assertThat(Updates.create(ImmutableMap.of()).updates()).isEmpty();
  }

  @Test
  public void generateUpdates_allUniquePaths_convertedToUpdates() {
    String pathOne = "applicant.name";
    String pathTwo = "applicant.color";
    ImmutableMap<String, String> rawUpdates = ImmutableMap.of(pathOne, "Rosa", pathTwo, "blue");

    Updates updates = Updates.create(rawUpdates);

    assertThat(updates.updates())
        .containsExactly(
            Update.create(Path.create(pathOne), "Rosa"),
            Update.create(Path.create(pathTwo), "blue"));
  }

  @Test
  public void generateUpdates_multipleCheckboxes_convertedToList() {
    String checkboxOne = "applicant.color.selection.red";
    String checkboxTwo = "applicant.color.selection.blue";
    String checkboxThree = "applicant.color.selection.light blue";
    ImmutableMap<String, String> rawUpdates =
        ImmutableMap.of(checkboxOne, "on", checkboxTwo, "on", checkboxThree, "on");

    Updates updates = Updates.create(rawUpdates);

    assertThat(updates.updates())
        .containsExactly(
            Update.create(Path.create("applicant.color.selection"), "[red, blue, light blue]"));
  }

  @Test
  public void generateUpdates_mixOfSingleFieldsAndCheckboxes_convertedCorrectly() {
    String pathOne = "applicant.color.selection.red";
    String pathTwo = "applicant.color.selection.blue";
    String pathThree = "applicant.name";
    ImmutableMap<String, String> rawUpdates =
        ImmutableMap.of(pathOne, "on", pathTwo, "on", pathThree, "Velma");

    Updates updates = Updates.create(rawUpdates);

    assertThat(updates.updates())
        .containsExactly(
            Update.create(Path.create("applicant.color.selection"), "[red, blue]"),
            Update.create(Path.create(pathThree), "Velma"));
  }
}
