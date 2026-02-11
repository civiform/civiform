package views.dev.componentcatalog.fragments;

import com.google.common.collect.ImmutableList;

public final class SelectControl {

  public BasicSelect basicSelect() {
    return new BasicSelect(
        "color",
        "color",
        "Favorite Color",
        ImmutableList.of(
            new Option("red", "Red"), new Option("blue", "Blue"), new Option("green", "Green")),
        "blue");
  }

  public RequiredSelect requiredSelect() {
    return new RequiredSelect(
        "country",
        "country",
        "Country",
        ImmutableList.of(
            new Option("us", "United States"),
            new Option("ca", "Canada"),
            new Option("mx", "Mexico")));
  }

  public SelectWithError selectWithError() {
    return new SelectWithError(
        "state",
        "state",
        "State",
        "false",
        "Please select a state",
        ImmutableList.of(
            new Option("ca", "California"),
            new Option("ny", "New York"),
            new Option("tx", "Texas")));
  }

  public GroupedSelect groupedSelect() {
    return new GroupedSelect(
        "food",
        "food",
        "Choose a Food",
        ImmutableList.of(new Option("apple", "Apple"), new Option("banana", "Banana")),
        ImmutableList.of(new Option("carrot", "Carrot"), new Option("broccoli", "Broccoli")));
  }

  public SelectWithData selectWithData() {
    return new SelectWithData(
        "size",
        "size",
        "T-Shirt Size",
        "size-selector",
        "clothing",
        ImmutableList.of(
            new Option("s", "Small"), new Option("m", "Medium"), new Option("l", "Large")),
        "m");
  }

  public record Option(String value, String text) {}

  public record BasicSelect(
      String id, String name, String label, ImmutableList<Option> options, String selectedValue) {}

  public record RequiredSelect(
      String id, String name, String label, ImmutableList<Option> options) {}

  public record SelectWithError(
      String id,
      String name,
      String label,
      String isValid,
      String validationMessage,
      ImmutableList<Option> options) {}

  public record GroupedSelect(
      String id,
      String name,
      String label,
      ImmutableList<Option> fruits,
      ImmutableList<Option> vegetables) {}

  public record SelectWithData(
      String id,
      String name,
      String label,
      String testId,
      String category,
      ImmutableList<Option> options,
      String selectedValue) {}
}
