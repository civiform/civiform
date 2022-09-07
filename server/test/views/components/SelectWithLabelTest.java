package views.components;

import static org.assertj.core.api.Assertions.assertThat;

import com.google.common.collect.ImmutableList;
import org.junit.Test;

public class SelectWithLabelTest {

  @Test
  public void createSelect_rendersSelect() {
    SelectWithLabel selectWithLabel = new SelectWithLabel().setId("id");
    assertThat(selectWithLabel.getSelectTag().render()).contains("<select");
  }

  @Test
  public void createSelect_rendersOptions() {
    SelectWithLabel selectWithLabel =
        new SelectWithLabel()
            .setId("id")
            .setOptions(
                ImmutableList.of(
                    SelectWithLabel.OptionValue.builder().setLabel("a").setValue("b").build()));
    assertThat(selectWithLabel.getSelectTag().render()).contains("<option");
  }

  @Test
  public void createSelect_rendersPlaceholder() {
    String placeholderText = "Placeholder text";
    SelectWithLabel selectWithLabel =
        new SelectWithLabel().setId("id").setPlaceholderText(placeholderText);
    assertThat(selectWithLabel.getSelectTag().render()).contains(placeholderText);
    assertThat(selectWithLabel.getSelectTag().render()).contains("hidden selected");
  }

  @Test
  public void createSelect_rendersSelectedOption() {
    SelectWithLabel selectWithLabel =
        new SelectWithLabel()
            .setId("id")
            .setValue("b")
            .setOptions(
                ImmutableList.of(
                    SelectWithLabel.OptionValue.builder().setLabel("a").setValue("b").build()));
    assertThat(selectWithLabel.getSelectTag().render()).contains("<select");
    assertThat(selectWithLabel.getSelectTag().render()).contains("value=\"b\" selected");
    assertThat(selectWithLabel.getSelectTag().render()).doesNotContain("hidden selected");
    assertThat(selectWithLabel.getSelectTag().render()).doesNotContain("<optgroup>");
  }

  @Test
  public void createSelect_withOptGroups() {
    SelectWithLabel selectWithLabel =
        new SelectWithLabel()
            .setId("id")
            .setOptionGroups(
                ImmutableList.of(
                    SelectWithLabel.OptionGroup.builder()
                        .setLabel("First group label")
                        .setOptions(
                            ImmutableList.of(
                                SelectWithLabel.OptionValue.builder()
                                    .setLabel("a")
                                    .setValue("b")
                                    .build()))
                        .build(),
                    SelectWithLabel.OptionGroup.builder()
                        .setLabel("First group label")
                        .setOptions(
                            ImmutableList.of(
                                SelectWithLabel.OptionValue.builder()
                                    .setLabel("c")
                                    .setValue("d")
                                    .build()))
                        .build()));
    assertThat(selectWithLabel.getSelectTag().render()).contains("<select");
    assertThat(selectWithLabel.getSelectTag().render())
        .contains("<optgroup label=\"First group label\">");
    assertThat(selectWithLabel.getSelectTag().render())
        .contains("<optgroup label=\"Second group label\">");
    assertThat(selectWithLabel.getSelectTag().render()).contains("<option");
  }
}
