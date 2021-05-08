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
import views.style.ReferenceClasses;
import views.style.StyleUtils;
import views.style.Styles;

public class FieldWithLabel {
  private static final String[] CORE_FIELD_CLASSES = {
    BaseStyles.FIELD_BORDER_COLOR,
    Styles.BG_GRAY_50,
    Styles.BLOCK,
    Styles.BORDER,
    Styles.P_2,
    Styles.W_FULL
  };

  private static final String[] CORE_CHECKBOX_FIELD_CLASSES = {
    Styles.H_4, Styles.W_4, Styles.MR_3, Styles.MB_2
  };

  private static final String[] CORE_CHECKBOX_LABEL_CLASSES = {
    Styles.TEXT_GRAY_600, Styles.ALIGN_TEXT_TOP, Styles.FONT_BOLD, Styles.TEXT_XS, Styles.UPPERCASE
  };

  private static final String[] CORE_LABEL_CLASSES = {
    BaseStyles.LABEL_BACKGROUND_COLOR,
    BaseStyles.LABEL_TEXT_COLOR,
    Styles.BLOCK,
    Styles.FONT_BOLD,
    Styles.TEXT_XS,
    Styles._MX_1,
    Styles.MB_2,
    Styles.UPPERCASE
  };

  private static final String[] FLOATED_FIELD_CLASSES = {
    Styles.OUTLINE_NONE,
    Styles.PX_3,
    Styles.PT_6,
    Styles.PB_2,
    Styles.M_AUTO,
    Styles.BORDER,
    BaseStyles.FIELD_BORDER_COLOR,
    Styles.ROUNDED_LG,
    Styles.W_FULL,
    Styles.TEXT_LG,
    Styles.PLACEHOLDER_GRAY_400,
    StyleUtils.focus(Styles.BORDER_BLUE_500)
  };

  private static final String[] FLOATED_LABEL_CLASSES = {
    Styles.ABSOLUTE,
    Styles.POINTER_EVENTS_NONE,
    Styles.TEXT_GRAY_600,
    Styles.TOP_1,
    Styles.LEFT_3,
    Styles.TEXT_XS,
    Styles.PX_1,
    Styles.PY_2
  };

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
  protected boolean floatLabel = false;
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

  public FieldWithLabel setFloatLabel(boolean floatLabel) {
    this.floatLabel = floatLabel;
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
      // For number types, only set the value if it's present since there is no of string
      // equivalent for numbers.
      if (this.fieldValueNumber.isPresent()) {
        fieldTag.withValue(String.valueOf(this.fieldValueNumber.getAsLong()));
      }
    } else {
      fieldTag.withValue(this.fieldValue);
    }

    String fieldTagClasses = StyleUtils.joinStyles(CORE_FIELD_CLASSES);
    if (!fieldErrors.isEmpty()) {
      fieldTagClasses = StyleUtils.joinStyles(fieldTagClasses, BaseStyles.FIELD_ERROR_BORDER_COLOR);
    }

    if (Strings.isNullOrEmpty(this.id)) this.id = this.fieldName;

    fieldTag
        .withClasses(fieldTagClasses)
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
            .condAttr(!Strings.isNullOrEmpty(this.id), Attr.FOR, this.id)
            .withClasses(FieldWithLabel.CORE_LABEL_CLASSES)
            .withText(this.labelText);

    if (this.floatLabel) {
      fieldTagClasses = StyleUtils.joinStyles(FieldWithLabel.FLOATED_FIELD_CLASSES);
      if (!fieldErrors.isEmpty()) {
        fieldTagClasses =
            StyleUtils.joinStyles(fieldTagClasses, BaseStyles.FIELD_ERROR_BORDER_COLOR);
      }

      fieldTag.withClasses(fieldTagClasses);
      labelTag.withClasses(FieldWithLabel.FLOATED_LABEL_CLASSES);

      return div()
          .with(
              div(fieldTag, labelTag, buildFieldErrorsTag())
                  .withClasses(ReferenceClasses.FLOATED_LABEL, Styles.MB_4, Styles.RELATIVE));
    }

    return div(labelTag, fieldTag, buildFieldErrorsTag()).withClasses(Styles.MX_4, Styles.MB_4);
  }

  /** Swaps the order of the label and field, possibly adds, and adds different styles. */
  private ContainerTag getCheckboxContainer() {
    fieldTag.withClasses(CORE_CHECKBOX_FIELD_CLASSES);
    if (this.checked) {
      fieldTag.attr("checked");
    }

    ContainerTag labelTag =
        label()
            .withClasses(CORE_CHECKBOX_LABEL_CLASSES)
            .condAttr(!Strings.isNullOrEmpty(this.id), Attr.FOR, this.id)
            .withText(this.labelText);

    return div(fieldTag, labelTag).withClasses(Styles.M_4, Styles.MB_1);
  }

  private Tag buildFieldErrorsTag() {
    return div(each(fieldErrors, error -> div(error.getMessage(messages))))
        .withClasses(StyleUtils.joinStyles(BaseStyles.FORM_ERROR_TEXT, Styles.PX_2));
  }
}
