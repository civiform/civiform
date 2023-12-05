package views.components;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static play.test.Helpers.fakeApplication;

import j2html.attributes.Attr;
import java.util.OptionalInt;
import java.util.OptionalLong;
import java.util.Set;
import org.junit.BeforeClass;
import org.junit.Test;
import play.data.validation.ValidationError;
import play.i18n.Lang;
import play.i18n.Messages;
import play.i18n.MessagesApi;

public class FieldWithLabelTest {

  private static Messages messages;

  @BeforeClass
  public static void startPlay() {
    // Create a fake application here just to get access to the injector.
    // We don't start the fake application so there's no need for an @AfterClass
    // method to stop it.
    messages =
        fakeApplication()
            .injector()
            .instanceOf(MessagesApi.class)
            .preferred(Set.of(Lang.defaultLang()));
  }

  @Test
  public void createInput_rendersInput() {
    FieldWithLabel fieldWithLabel = FieldWithLabel.input().setId("id");
    assertThat(fieldWithLabel.getInputTag().render()).contains("<input");
  }

  @Test
  public void createTextArea_rendersTextArea() {
    FieldWithLabel fieldWithLabel = FieldWithLabel.textArea().setId("id");
    String fieldHtml = fieldWithLabel.getTextareaTag().render();
    assertThat(fieldHtml).contains("id=\"id\"");
    assertThat(fieldHtml).contains("<textarea");
  }

  @Test
  public void createNumber_rendersNumberInput() {
    FieldWithLabel fieldWithLabel = FieldWithLabel.number();
    assertThat(fieldWithLabel.getNumberTag().render()).contains("<input");
    assertThat(fieldWithLabel.getNumberTag().render()).contains("type=\"number\"");
  }

  @Test
  public void createEmail_rendersEmailInput() {
    FieldWithLabel fieldWithLabel = FieldWithLabel.email();
    assertThat(fieldWithLabel.getEmailTag().render()).contains("<input");
    assertThat(fieldWithLabel.getEmailTag().render()).contains("type=\"email\"");
  }

  @Test
  public void createTextArea_setsRandomId() {
    FieldWithLabel fieldWithLabel = FieldWithLabel.textArea().setId("id");
    // Check that we do not have an empty id.
    assertThat(fieldWithLabel.getTextareaTag().render()).doesNotContain("id ");
    assertThat(fieldWithLabel.getTextareaTag().render()).doesNotContain("id=\"\"");
  }

  @Test
  public void createRadio_setsRandomId() {
    FieldWithLabel fieldWithLabel = FieldWithLabel.radio();
    // Check that we do not have an empty id.
    assertThat(fieldWithLabel.getRadioTag().render()).doesNotContain("id ");
    assertThat(fieldWithLabel.getRadioTag().render()).doesNotContain("id=\"\"");
  }

  @Test
  public void createCurrency_setsRandomId() {
    FieldWithLabel fieldWithLabel = FieldWithLabel.currency();
    // Check that we do not have an empty id.
    assertThat(fieldWithLabel.getCurrencyTag().render()).doesNotContain("id ");
    assertThat(fieldWithLabel.getCurrencyTag().render()).doesNotContain("id=\"\"");
  }

  @Test
  public void createInput_setsRandomId() {
    FieldWithLabel fieldWithLabel = FieldWithLabel.input();
    // Check that we do not have an empty id.
    assertThat(fieldWithLabel.getInputTag().render()).doesNotContain("id ");
    assertThat(fieldWithLabel.getInputTag().render()).doesNotContain("id=\"\"");
  }

  @Test
  public void createInput_setsMaxLength() {
    FieldWithLabel fieldWithLabel = FieldWithLabel.input();
    // Check that we set the max length.
    assertThat(fieldWithLabel.getInputTag().render()).contains("maxlength=\"10000\"");
  }

  @Test
  public void createNumber_setsRandomId() {
    FieldWithLabel fieldWithLabel = FieldWithLabel.number();
    // Check that we do not have an empty id.
    assertThat(fieldWithLabel.getNumberTag().render()).doesNotContain("id ");
    assertThat(fieldWithLabel.getNumberTag().render()).doesNotContain("id=\"\"");
  }

  @Test
  public void createDate_setsRandomId() {
    FieldWithLabel fieldWithLabel = FieldWithLabel.date();
    // Check that we do not have an empty id.
    assertThat(fieldWithLabel.getDateTag().render()).doesNotContain("id ");
    assertThat(fieldWithLabel.getDateTag().render()).doesNotContain("id=\"\"");
  }

  @Test
  public void createEmail_setsRandomId() {
    FieldWithLabel fieldWithLabel = FieldWithLabel.email();
    // Check that we do not have an empty id.
    assertThat(fieldWithLabel.getEmailTag().render()).doesNotContain("id ");
    assertThat(fieldWithLabel.getEmailTag().render()).doesNotContain("id=\"\"");
  }

  @Test
  public void number_setsNoValueByDefault() {
    FieldWithLabel fieldWithLabel = FieldWithLabel.number();
    // No "value"="some-value" attribute. But allow "this.value" in script.
    assertThat(fieldWithLabel.getNumberTag().render()).doesNotContain(" value");
  }

  @Test
  public void number_setsGivenNumberValue() {
    FieldWithLabel fieldWithLabel = FieldWithLabel.number().setValue(OptionalInt.of(6));
    assertThat(fieldWithLabel.getNumberTag().render()).contains("value=\"6\"");
  }

  @Test
  public void number_setsMaxValue() {
    FieldWithLabel fieldWithLabel = FieldWithLabel.number().setMax(OptionalLong.of(5L));
    assertThat(fieldWithLabel.getNumberTag().render()).contains("max=\"5\"");
  }

  @Test
  public void number_setsMinValue() {
    FieldWithLabel fieldWithLabel = FieldWithLabel.number().setMin(OptionalLong.of(1L));
    assertThat(fieldWithLabel.getNumberTag().render()).contains("min=\"1\"");
  }

  @Test
  public void number_setsInputmodeDecimal() {
    FieldWithLabel fieldWithLabel = FieldWithLabel.number();
    assertThat(fieldWithLabel.getNumberTag().render()).contains("inputmode=\"decimal\"");
  }

  @Test
  public void number_setsStepAnyDefault() {
    FieldWithLabel fieldWithLabel = FieldWithLabel.number().setMax(OptionalLong.of(5L));
    assertThat(fieldWithLabel.getNumberTag().render()).contains("step=\"any\"");
  }

  @Test
  public void radio_setsName() {
    FieldWithLabel fieldWithLabel = FieldWithLabel.radio().setFieldName("radio_name_222");
    assertThat(fieldWithLabel.getRadioTag().render()).contains("name=\"radio_name_222\"");
  }

  @Test
  public void radio_setsId() {
    FieldWithLabel fieldWithLabel = FieldWithLabel.radio().setId("666");
    assertThat(fieldWithLabel.getRadioTag().render()).contains("id=\"666\"");
  }

  @Test
  public void withNoErrors_DoesNotContainAriaAttributes() {
    FieldWithLabel fieldWithLabel = FieldWithLabel.number().setId("field-id");
    String rendered = fieldWithLabel.getNumberTag().render();

    assertThat(rendered).doesNotContain("aria-");
  }

  @Test
  public void input_withToolTip() {
    FieldWithLabel fieldWithLabel =
        FieldWithLabel.input().setToolTipIcon(Icons.INFO).setToolTipText("Live long and prosper");
    assertThat(fieldWithLabel.getInputTag().render()).contains("Live long and prosper");
    assertThat(fieldWithLabel.getInputTag().render()).contains(Icons.INFO.path);
  }

  @Test
  public void throwsExceptionWhenOnlyToolTipTextOrIconIsSet() {
    FieldWithLabel fieldWithLabelTextOnly =
        FieldWithLabel.input().setToolTipText("Live long and prosper");
    FieldWithLabel fieldWithLabelIconOnly = FieldWithLabel.input().setToolTipIcon(Icons.INFO);
    String expectedMessage = "Tool tip text and icon must both be defined";
    assertThatThrownBy(() -> fieldWithLabelTextOnly.getInputTag())
        .isInstanceOf(RuntimeException.class)
        .hasMessage(expectedMessage);
    assertThatThrownBy(() -> fieldWithLabelIconOnly.getInputTag())
        .isInstanceOf(RuntimeException.class)
        .hasMessage(expectedMessage);
  }

  @Test
  public void withErrors() {
    FieldWithLabel fieldWithLabel =
        FieldWithLabel.number()
            .setId("field-id")
            .setFieldErrors(
                messages, new ValidationError("error.internalServerTitle", "an error message"));
    String rendered = fieldWithLabel.getNumberTag().render();

    assertThat(rendered).contains("aria-invalid=\"true\"");
    assertThat(rendered).contains("aria-describedby=\"field-id-errors\"");
    assertThat(rendered).contains("id=\"field-id-errors\"");
    assertThat(rendered).contains("an error message");
  }

  @Test
  public void canChangeId() {
    FieldWithLabel fieldWithLabel = FieldWithLabel.textArea().setId("id");
    fieldWithLabel.setId("other_id");
    String fieldHtml = fieldWithLabel.getTextareaTag().render();
    assertThat(fieldHtml).contains("id=\"other_id\"");
  }

  @Test
  public void textArea_canSetRowsCols() {
    // should raise an error on failure
    FieldWithLabel fieldWithLabel =
        FieldWithLabel.textArea()
            .setId("id")
            .setRows(OptionalLong.of(8))
            .setCols(OptionalLong.of(5));

    String renderedContent = fieldWithLabel.getTextareaTag().render();
    assertThat(renderedContent).contains("rows=\"8\"");
    assertThat(renderedContent).contains("cols=\"5\"");
  }

  @Test
  public void canEditAfterRender() {
    FieldWithLabel fieldWithLabel = FieldWithLabel.textArea().setId("id");
    String fieldHtml = fieldWithLabel.getTextareaTag().render();
    assertThat(fieldWithLabel.getTextareaTag().render()).isEqualTo(fieldHtml);
    fieldWithLabel.setId("other_id");
    String otherFieldHtml = fieldWithLabel.getTextareaTag().render();
    assertThat(otherFieldHtml).isNotEqualTo(fieldHtml);
    fieldWithLabel.setValue("value");
    assertThat(fieldWithLabel.getTextareaTag().render()).isNotEqualTo(otherFieldHtml);
    assertThat(fieldWithLabel.getTextareaTag().render()).isNotEqualTo(fieldHtml);
    fieldWithLabel.setId("id");
    fieldWithLabel.setValue("");
    assertThat(fieldWithLabel.getTextareaTag().render()).isEqualTo(fieldHtml);
  }

  @Test
  public void whenFocusOnError_autofocusIsPresent() {
    FieldWithLabel fieldWithLabel = FieldWithLabel.input();
    fieldWithLabel.focusOnError();
    String rendered = fieldWithLabel.getInputTag().render();

    assertThat(rendered).contains(Attr.AUTOFOCUS);
  }

  @Test
  public void whenNotFocusOnError_autofocusIsNotPresent() {
    FieldWithLabel fieldWithLabel = FieldWithLabel.input();
    String rendered = fieldWithLabel.getInputTag().render();

    assertThat(rendered).doesNotContain(Attr.AUTOFOCUS);
  }

  @Test
  public void whenFocusOnInput_autofocusIsPresent() {
    FieldWithLabel fieldWithLabel = FieldWithLabel.input();
    fieldWithLabel.focusOnInput();
    String rendered = fieldWithLabel.getInputTag().render();

    assertThat(rendered).contains(Attr.AUTOFOCUS);
  }

  @Test
  public void whenNotFocusOnInput_autofocusIsNotPresent() {
    FieldWithLabel fieldWithLabel = FieldWithLabel.input();
    String rendered = fieldWithLabel.getInputTag().render();

    assertThat(rendered).doesNotContain(Attr.AUTOFOCUS);
  }
}
