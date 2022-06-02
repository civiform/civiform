package views.components;

import static org.assertj.core.api.Assertions.assertThat;
import static play.test.Helpers.stubMessagesApi;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import java.util.OptionalInt;
import java.util.OptionalLong;
import org.junit.Test;
import play.data.validation.ValidationError;
import play.i18n.Lang;
import play.i18n.Messages;

public class FieldWithLabelTest {

  private final Messages messages =
      stubMessagesApi().preferred(ImmutableSet.of(Lang.defaultLang()));

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
  public void createEmail_rendersEmailInput() {
    FieldWithLabel fieldWithLabel = FieldWithLabel.email();
    assertThat(fieldWithLabel.getContainer().render()).contains("<input");
    assertThat(fieldWithLabel.getContainer().render()).contains("type=\"email\"");
  }

  @Test
  public void number_setsNoValueByDefault() {
    FieldWithLabel fieldWithLabel = FieldWithLabel.number();
    // No "value"="some-value" attribute. But allow "this.value" in script.
    assertThat(fieldWithLabel.getContainer().render()).doesNotContain(" value");
  }

  @Test
  public void number_setsGivenNumberValue() {
    FieldWithLabel fieldWithLabel = FieldWithLabel.number().setValue(OptionalInt.of(6));
    assertThat(fieldWithLabel.getContainer().render()).contains("value=\"6\"");
  }

  @Test
  public void number_setsMaxValue() {
    FieldWithLabel fieldWithLabel = FieldWithLabel.number().setMax(OptionalLong.of(5L));
    assertThat(fieldWithLabel.getContainer().render()).contains("max=\"5\"");
  }

  @Test
  public void number_setsMinValue() {
    FieldWithLabel fieldWithLabel = FieldWithLabel.number().setMin(OptionalLong.of(1L));
    assertThat(fieldWithLabel.getContainer().render()).contains("min=\"1\"");
  }

  @Test
  public void number_setsInputmodeDecimal() {
    FieldWithLabel fieldWithLabel = FieldWithLabel.number();
    assertThat(fieldWithLabel.getContainer().render()).contains("inputmode=\"decimal\"");
  }

  @Test
  public void number_setsStepAnyDefault() {
    FieldWithLabel fieldWithLabel = FieldWithLabel.number().setMax(OptionalLong.of(5L));
    assertThat(fieldWithLabel.getContainer().render()).contains("step=\"any\"");
  }

  @Test
  public void withNoErrors_DoesNotContainAriaAttributes() {
    FieldWithLabel fieldWithLabel = FieldWithLabel.number().setId("field-id");
    String rendered = fieldWithLabel.getContainer().render();

    assertThat(rendered).doesNotContain("aria-");
  }

  @Test
  public void withFieldErrors() {
    // Verify aria labels for field level errors.
    FieldWithLabel fieldWithLabel =
        FieldWithLabel.number()
            .setId("field-id")
            .setFieldErrors(messages, new ValidationError("", "an error message"));
    String rendered = fieldWithLabel.getContainer().render();

    assertThat(rendered).contains("aria-invalid=\"true\"");
    assertThat(rendered).contains("aria-describedBy=\"field-id-errors\"");
    assertThat(rendered).contains("id=\"field-id-errors\"");
    assertThat(rendered).contains("an error message");
  }

  @Test
  public void withQuestionErrors() {
    // Verify aria labels for passed in question level errors.
    FieldWithLabel fieldWithLabel =
        FieldWithLabel.number()
            .setId("field-id")
            .setAriaDescribedByIds(ImmutableList.of("question-errors", "question-description"))
            .setHasQuestionErrors(true);
    String rendered = fieldWithLabel.getContainer().render();

    assertThat(rendered).contains("aria-invalid=\"true\"");
    assertThat(rendered).contains("aria-describedBy=\"question-errors question-description\"");
  }

  @Test
  public void withFieldAndQuestionErrors() {
    // Verify aria labels are merged properly when both field and question level errors are present.
    FieldWithLabel fieldWithLabel =
        FieldWithLabel.number()
            .setId("field-id")
            .setFieldErrors(messages, new ValidationError("", "an error message"))
            .setAriaDescribedByIds(ImmutableList.of("question-errors", "question-description"))
            .setHasQuestionErrors(true);
    String rendered = fieldWithLabel.getContainer().render();

    assertThat(rendered).contains("aria-invalid=\"true\"");
    assertThat(rendered)
        .contains("aria-describedBy=\"field-id-errors question-errors question-description\"");
    assertThat(rendered).contains("id=\"field-id-errors\"");
    assertThat(rendered).contains("an error message");
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
