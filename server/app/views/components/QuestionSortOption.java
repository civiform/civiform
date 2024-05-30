package views.components;

import static com.google.common.base.Preconditions.checkArgument;

import com.google.common.collect.ImmutableList;
import java.util.Optional;

/** Represents a sorting option for a list of Questions. */
public enum QuestionSortOption {
  LAST_MODIFIED(
      /* dataAttribute= */ "lastmodified",
      /* displayStringAscending= */ Optional.empty(),
      /* displayStringDescending= */ Optional.of("Last modified")),
  ADMIN_NAME(
      /* dataAttribute= */ "adminname",
      /* displayStringAscending= */ Optional.of("Admin ID A-Z"),
      /* displayStringDescending= */ Optional.of("Admin ID Z-A")),
  NUM_PROGRAMS(
      /* dataAttribute= */ "numprograms",
      /* displayStringAscending= */ Optional.of("Fewest programs"),
      /* displayStringDescending= */ Optional.of("Most programs")),
  TI_NAME(
      /* dataAttribute= */ "tiname",
      /* displayStringAscending= */ Optional.of("Name A-Z"),
      /* displayStringDescending= */ Optional.of("Name Z-A")),
  NUM_MEMBERS(
      /* dataAttribute= */ "nummember",
      /* displayStringAscending= */ Optional.of("Fewest members"),
      /* displayStringDescending= */ Optional.of("Most members"));

  // Suffix added to the sort option value to indicate if sorting should be in
  // ascending or descending order. The full value will be in the format
  // "<data_attribute_name>-<asc|desc>", e.g. "adminname-desc".
  private static final String ASCENDING_SUFFIX = "asc";
  private static final String DESCENDING_SUFFIX = "desc";

  // Name of the HTML data attribute for this sorting option. This is used as part
  // of the value for the select tag option along with the ascending or descending suffix. It
  // should also be used as the data attribute name in the HTML that renders the question. The value
  // of the data attribute should contain the actual data to sort on.
  private final String dataAttribute;
  // Display string for the select tag option for the option that sorts by this
  // attribute in descending order. Not present if this attribute should not have a descending
  // option.
  private final Optional<String> displayStringDescending;
  // Display string for the select tag option for the option that sorts by this
  // attribute in ascending order. Not present if this attribute should not have an ascending
  // option.
  private final Optional<String> displayStringAscending;

  private QuestionSortOption(
      String dataAttribute,
      Optional<String> displayStringAscending,
      Optional<String> displayStringDescending) {
    this.dataAttribute = dataAttribute;
    checkArgument(displayStringAscending.isPresent() || displayStringDescending.isPresent());
    this.displayStringAscending = displayStringAscending;
    this.displayStringDescending = displayStringDescending;
  }

  public String getDataAttribute() {
    return dataAttribute;
  }

  public ImmutableList<SelectWithLabel.OptionValue> getSelectOptions() {
    ImmutableList.Builder<SelectWithLabel.OptionValue> questionSortOptionsBuilder =
        ImmutableList.builder();
    if (displayStringAscending.isPresent()) {
      questionSortOptionsBuilder.add(
          SelectWithLabel.OptionValue.builder()
              .setLabel(displayStringAscending.get())
              .setValue(String.format("%s-%s", dataAttribute, ASCENDING_SUFFIX))
              .build());
    }
    if (displayStringDescending.isPresent()) {
      questionSortOptionsBuilder.add(
          SelectWithLabel.OptionValue.builder()
              .setLabel(displayStringDescending.get())
              .setValue(String.format("%s-%s", dataAttribute, DESCENDING_SUFFIX))
              .build());
    }
    return questionSortOptionsBuilder.build();
  }
}
