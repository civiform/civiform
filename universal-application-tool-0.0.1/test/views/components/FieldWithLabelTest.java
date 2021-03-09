package views.components;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.Test;

public class FieldWithLabelTest {

  @Test
  public void createInput_rendersInput() {
    FieldWithLabel fieldWithLabel = FieldWithLabel.createInput("id");
    assertThat(fieldWithLabel.getContainer().render()).contains("<input");
  }

  @Test
  public void createTextArea_rendersTextArea() {
    FieldWithLabel fieldWithLabel = FieldWithLabel.createTextArea("id");
    String fieldHtml = fieldWithLabel.getContainer().render();
    assertThat(fieldHtml).contains("id=\"id\"");
    assertThat(fieldHtml).contains("<textarea");
  }

  @Test
  public void canChangeId() {
    FieldWithLabel fieldWithLabel = FieldWithLabel.createTextArea("id");
    fieldWithLabel.setId("other_id");
    String fieldHtml = fieldWithLabel.getContainer().render();
    assertThat(fieldHtml).contains("id=\"other_id\"");
  }

  @Test
  public void ignoresCallsAfterRender() {
    FieldWithLabel fieldWithLabel = FieldWithLabel.createTextArea("id");
    String fieldHtml = fieldWithLabel.getContainer().render();
    fieldWithLabel.setId("other_id");
    fieldWithLabel.setValue("value");
    assertThat(fieldWithLabel.getContainer().render()).isEqualTo(fieldHtml);
  }
}
