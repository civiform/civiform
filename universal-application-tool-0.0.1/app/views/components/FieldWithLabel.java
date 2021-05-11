package views.components;

import static j2html.TagCreator.div;
import static j2html.TagCreator.each;
import static j2html.TagCreator.label;
import static j2html.TagCreator.textarea;

import com.google.common.base.Strings;
import com.google.common.collect.ImmutableSet;
import j2html.TagCreator;
import j2html.attributes.Attr;
import j2html.tags.ContainerTag;
import j2html.tags.Tag;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.OptionalLong;
import play.i18n.Messages;
import services.applicant.ValidationErrorMessage;
import views.style.BaseStyles;
import views.style.StyleUtils;
import views.style.Styles;

public class FieldWithLabel {
  protected Tag fieldTag;
  protected String fieldName = "";
  protected String fieldType = "text";
  protected String fieldValue = "";

  /** For use with fields of type `number`. */
  protected OptionalLong fieldValueNumber = OptionalLong.empty();

  protected String formId = "";
  protected String id = "";
  protected String labelText = "";
  protected String placeholderText = "";
  protected Messages messages;
  protected ImmutableSet<ValidationErrorMessage> fieldErrors = ImmutableSet.of();
  protected boolean checked = false;
  protected boolean disabled = false;

  public FieldWithLabel(Tag fieldTag) {
    this.fieldTag = fieldTag;
  }

  public static FieldWithLabel checkbox() {
    Tag fieldTag = TagCreator.input();
    return new FieldWithLabel(fieldTag).setFieldType("checkbox");
  }

  public static FieldWithLabel input() {
    Tag fieldTag = TagCreator.input();
    return new FieldWithLabel(fieldTag).setFieldType("text");
  }

  public static FieldWithLabel number() {
    Tag fieldTag = TagCreator.input();
    return new FieldWithLabel(fieldTag).setFieldType("number");
  }

  public static FieldWithLabel date() {
    Tag fieldTag = TagCreator.input();
    return new FieldWithLabel(fieldTag).setFieldType("date");
  }

  public static FieldWithLabel textArea() {
    Tag fieldTag = textarea();
    return new FieldWithLabel(fieldTag).setFieldType("text");
  }

  public FieldWithLabel setChecked(boolean checked) {
    this.checked = checked;
    return this;
  }

  public FieldWithLabel setFieldName(String fieldName) {
    this.fieldName = fieldName;
    return this;
  }

  public FieldWithLabel setFieldType(String fieldType) {
    this.fieldTag.withType(fieldType);
    this.fieldType = fieldType;
    return this;
  }

  public FieldWithLabel setFormId(String formId) {
    this.formId = formId;
    return this;
  }

  public FieldWithLabel setId(String inputId) {
    this.id = inputId;
    return this;
  }

  public FieldWithLabel setLabelText(String labelText) {
    this.labelText = labelText;
    return this;
  }

  public FieldWithLabel setPlaceholderText(String placeholder) {
    this.placeholderText = placeholder;
    return this;
  }

  public FieldWithLabel setValue(String value) {
    if (!this.fieldType.equals("text")
        && !this.fieldType.equals("checkbox")
        && !this.fieldType.equals("date")) {
      throw new RuntimeException(
          String.format(
              "setting a String value is not available on fields of type `%s`", this.fieldType));
    }

    this.fieldValue = value;
    return this;
  }

  public FieldWithLabel setValue(Optional<String> value) {
    if (this.fieldType.equals("number")) {
      throw new RuntimeException(
          "setting a String value is not available on fields of type 'number'");
    }
    value.ifPresent(s -> this.fieldValue = s);
    return this;
  }

  public FieldWithLabel setValue(OptionalInt value) {
    if (!this.fieldType.equals("number")) {
      throw new RuntimeException(
          "setting an Optional<Integer> value is only available on fields of type `number`");
    }

    this.fieldValueNumber =
        value.isPresent() ? OptionalLong.of(value.getAsInt()) : OptionalLong.empty();
    return this;
  }

  public FieldWithLabel setValue(OptionalLong value) {
    if (!this.fieldType.equals("number")) {
      throw new RuntimeException(
          "setting an OptionalLong value is only available on fields of type `number`");
    }

    this.fieldValueNumber = value;
    return this;
  }

  public FieldWithLabel setDisabled(boolean disabled) {
    this.disabled = disabled;
    return this;
  }

  public FieldWithLabel setFieldErrors(
      Messages messages, ImmutableSet<ValidationErrorMessage> errors) {
    this.messages = messages;
    this.fieldErrors = errors;
    return this;
  }

  public ContainerTag getContainer() {
    if (fieldTag.getTagName().equals("textarea")) {
      // Have to recreate the field here in case the value is modified.
      ContainerTag textAreaTag = textarea().withType("text").withText(this.fieldValue);
      fieldTag = textAreaTag;
    } else if (this.fieldType.equals("number")) {
      // For number types, only set the value if it's present since there is no empty string
      // equivalent for numbers.
      if (this.fieldValueNumber.isPresent()) {
        fieldTag.withValue(String.valueOf(this.fieldValueNumber.getAsLong()));
      }
    } else {
      fieldTag.withValue(this.fieldValue);
    }

    // Need to assign an ID in order to properly associate the label with this input field.
    if (Strings.isNullOrEmpty(this.id)) this.id = this.fieldName;

    fieldTag
        .withClasses(
            StyleUtils.joinStyles(
                BaseStyles.INPUT,
                fieldErrors.isEmpty() ? "" : BaseStyles.FORM_FIELD_ERROR_BORDER_COLOR))
        .withCondId(!Strings.isNullOrEmpty(this.id), this.id)
        .withName(this.fieldName)
        .condAttr(this.disabled, "disabled", "true")
        .withCondPlaceholder(!Strings.isNullOrEmpty(this.placeholderText), this.placeholderText)
        .condAttr(!Strings.isNullOrEmpty(this.formId), "form", formId);

    if (this.fieldType.equals("checkbox")) {
      return getCheckboxContainer();
    }

    ContainerTag labelTag =
        label()
            .condAttr(shouldSetLabelFor(), Attr.FOR, this.id)
            .withClasses(labelText.isEmpty() ? "" : BaseStyles.INPUT_LABEL)
            .withText(labelText);

    return div(labelTag, fieldTag, buildFieldErrorsTag())
        .withClasses(BaseStyles.FORM_FIELD_MARGIN_BOTTOM);
  }

  /** Swaps the order of the label and field, possibly adds, and adds different styles. */
  private ContainerTag getCheckboxContainer() {
    fieldTag.withClasses(
        labelText.isEmpty() ? BaseStyles.CHECKBOX_WITH_NO_LABEL : BaseStyles.CHECKBOX);

    if (this.checked) {
      fieldTag.attr("checked");
    }

    return label()
        .withClasses(
            BaseStyles.CHECKBOX_LABEL,
            BaseStyles.FORM_FIELD_MARGIN_BOTTOM,
            labelText.isEmpty() ? Styles.W_MIN : "")
        .condAttr(shouldSetLabelFor(), Attr.FOR, this.id)
        .with(fieldTag)
        .withText(this.labelText);
  }

  private Tag buildFieldErrorsTag() {
    return div(each(fieldErrors, error -> div(error.getMessage(messages))))
        .withClasses(
            fieldErrors.isEmpty()
                ? ""
                : StyleUtils.joinStyles(BaseStyles.FORM_ERROR_TEXT, Styles.P_1));
  }

  private boolean shouldSetLabelFor() {
    return !this.id.isEmpty() && !this.labelText.isEmpty();
  }
}
