package views.components;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import java.util.Optional;

public enum QuestionSortOption {
  LAST_MODIFIED("lastmodified", Optional.empty(), Optional.of("Last modified")),
  ADMIN_NAME("adminname", Optional.of("Admin ID A-Z"), Optional.of("Admin ID Z-A")),
  NUM_PROGRAMS("numprograms", Optional.of("Fewest programs"), Optional.of("Most programs"));

  private static final String ASCENDING_SUFFIX = "asc";
  private static final String DESCENDING_SUFFIX = "desc";

  private final String dataAttribute;
  private final Optional<String> displayStringDescending;
  private final Optional<String> displayStringAscending;

  private QuestionSortOption(
      String dataAttribute,
      Optional<String> displayStringAscending,
      Optional<String> displayStringDescending) {
    this.dataAttribute = dataAttribute;
    Preconditions.checkArgument(
        displayStringAscending.isPresent() || displayStringDescending.isPresent());
    this.displayStringAscending = displayStringAscending;
    this.displayStringDescending = displayStringDescending;
  }

  public String getDataAttribute() {
    return dataAttribute;
  }

  ImmutableList<SelectWithLabel.OptionValue> getSelectOptions() {
    ImmutableList.Builder<SelectWithLabel.OptionValue> questionSortOptionsBuilder =
        ImmutableList.builder();
    if (displayStringAscending.isPresent()) {
      questionSortOptionsBuilder.add(
          SelectWithLabel.OptionValue.builder()
              .setLabel(displayStringAscending.get())
              .setValue(formatQuestionSortSelectValue(/* descending= */ false))
              .build());
    }
    if (displayStringDescending.isPresent()) {
      questionSortOptionsBuilder.add(
          SelectWithLabel.OptionValue.builder()
              .setLabel(displayStringDescending.get())
              .setValue(formatQuestionSortSelectValue(/* descending= */ true))
              .build());
    }
    return questionSortOptionsBuilder.build();
  }

  private String formatQuestionSortSelectValue(boolean descending) {
    return String.format(
        "%s-%s", dataAttribute, (descending ? DESCENDING_SUFFIX : ASCENDING_SUFFIX));
  }
}
