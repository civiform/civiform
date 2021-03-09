package views.components;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.Test;
import com.google.common.collect.ImmutableList;
import java.util.AbstractMap.SimpleEntry;

public class SelectWithLabelTest {

  @Test
  public void createSelect_rendersSelect() {
    SelectWithLabel selectWithLabel = new SelectWithLabel("id");
    assertThat(selectWithLabel.getContainer().render()).contains("<select");
    assertThat(selectWithLabel.getContainer().render()).doesNotContain("<option");
  }

  @Test
  public void createSelect_rendersOptions() {
    SelectWithLabel selectWithLabel = new SelectWithLabel("id");
    ImmutableList<SimpleEntry<String, String>> options = 
      ImmutableList.of(new SimpleEntry<String,String>("a","b"));
    selectWithLabel.setOptions(options);
    assertThat(selectWithLabel.getContainer().render()).contains("<option");
  }

  @Test
  public void createSelect_rendersSelectedOption() {
    SelectWithLabel selectWithLabel = new SelectWithLabel("id");
    ImmutableList<SimpleEntry<String, String>> options = 
      ImmutableList.of(new SimpleEntry<String,String>("a","b"));
    selectWithLabel.setOptions(options);
    selectWithLabel.setValue("b");
    assertThat(selectWithLabel.getContainer().render()).contains("<select");
    assertThat(selectWithLabel.getContainer().render()).contains("selected");
  }
}
