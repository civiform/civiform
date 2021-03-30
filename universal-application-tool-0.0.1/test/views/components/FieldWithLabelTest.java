package views.components;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.Test;

import java.util.OptionalInt;

public class FieldWithLabelTest {

  @Test
  public void createInput_rendersInput() {
    FieldWithLabel fieldWithLabel = FieldWithLabel.input().setId("id");
    assertThat(fieldWithLabel.getContainer().render()).contains("<input");
  }

  @Test
  public void createTextArea_rendersTextArea() {
    FieldWithLabel fieldWithLabel = FieldWithLabel.textArea().setId("id");
    String fieldHtml = fieldWithLabel.getContainer().render();
    assertThat(fieldHtml).contains("id=\"id\"");
    assertThat(fieldHtml).contains("<textarea");
  }

  @Test
  public void createNumber_rendersNumberInput() {
    FieldWithLabel fieldWithLabel = FieldWithLabel.number();
    assertThat(fieldWithLabel.getContainer().render()).contains("<input");
    assertThat(fieldWithLabel.getContainer().render()).contains("type=\"number\"");
  }

  @Test
  public void number_setsNoValueByDefault() {
    FieldWithLabel fieldWithLabel = FieldWithLabel.number();
    assertThat(fieldWithLabel.getContainer().render()).doesNotContain("value");
  }

  @Test
  public void number_setsGivenNumberValue() {
//    FieldWithLabel fieldWithLabel = FieldWithLabel.number().setValue(OptionalInt.of(6));
    FieldWithLabel fieldWithLabel = FieldWithLabel.number().setValue("6");
    assertThat(fieldWithLabel.getContainer().render()).contains("value=\"6\"");
  }

  @Test
  public void canChangeId() {
    FieldWithLabel fieldWithLabel = FieldWithLabel.textArea().setId("id");
    fieldWithLabel.setId("other_id");
    String fieldHtml = fieldWithLabel.getContainer().render();
    assertThat(fieldHtml).contains("id=\"other_id\"");
  }

  @Test
  public void canEditAfterRender() {
    FieldWithLabel fieldWithLabel = FieldWithLabel.textArea().setId("id");
    String fieldHtml = fieldWithLabel.getContainer().render();
    assertThat(fieldWithLabel.getContainer().render()).isEqualTo(fieldHtml);
    fieldWithLabel.setId("other_id");
    String otherFieldHtml = fieldWithLabel.getContainer().render();
    assertThat(otherFieldHtml).isNotEqualTo(fieldHtml);
    fieldWithLabel.setValue("value");
    assertThat(fieldWithLabel.getContainer().render()).isNotEqualTo(otherFieldHtml);
    assertThat(fieldWithLabel.getContainer().render()).isNotEqualTo(fieldHtml);
    fieldWithLabel.setId("id");
    fieldWithLabel.setValue("");
    assertThat(fieldWithLabel.getContainer().render()).isEqualTo(fieldHtml);
  }
}
