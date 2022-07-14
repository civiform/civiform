package views.components;

import static org.assertj.core.api.Assertions.assertThat;

import com.google.common.collect.ImmutableMap;
import org.junit.Test;

public class SelectWithLabelTest {

  @Test
  public void createSelect_rendersSelect() {
    SelectWithLabel selectWithLabel = new SelectWithLabel().setId("id");
    assertThat(selectWithLabel.getSelectTag().render()).contains("<select");
  }

  @Test
  public void createSelect_rendersOptions() {
    SelectWithLabel selectWithLabel = new SelectWithLabel().setId("id");
    ImmutableMap<String, String> options = ImmutableMap.of("a", "b");
    selectWithLabel.setOptions(options);
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
    SelectWithLabel selectWithLabel = new SelectWithLabel().setId("id");
    ImmutableMap<String, String> options = ImmutableMap.of("a", "b");
    selectWithLabel.setOptions(options);
    selectWithLabel.setValue("b");
    assertThat(selectWithLabel.getSelectTag().render()).contains("<select");
    assertThat(selectWithLabel.getSelectTag().render()).contains("value=\"b\" selected");
    assertThat(selectWithLabel.getSelectTag().render()).doesNotContain("hidden selected");
  }
}
