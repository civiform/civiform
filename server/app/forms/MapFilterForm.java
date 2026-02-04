package forms;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import lombok.Getter;
import lombok.Setter;

/** Form for handling map question filter data binding. */
@Setter
@Getter
public final class MapFilterForm {
  private String possibleKeys;
  private List<MapQuestionForm.Setting> filters;

  public MapFilterForm() {
    possibleKeys = "";
    filters = new ArrayList<>();
  }

  public MapFilterForm(String possibleKeys, List<MapQuestionForm.Setting> filters) {
    this.possibleKeys = possibleKeys;
    this.filters = new ArrayList<>(filters);
  }

  /**
   * Parses the possibleKeys string into a list of individual keys. Handles comma-separated values
   * and removes brackets and whitespace.
   */
  public List<String> getParsedPossibleKeys() {
    return Optional.ofNullable(possibleKeys)
        .map(string -> string.replaceAll("[\\[\\]]", "").trim())
        .filter(s -> !s.isEmpty())
        .map(
            cleanString ->
                Arrays.stream(cleanString.split(","))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .toList())
        .orElse(List.of());
  }
}
