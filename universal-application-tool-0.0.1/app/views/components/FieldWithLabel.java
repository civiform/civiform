package views.components;

import static com.google.common.base.Preconditions.checkNotNull;
import static j2html.TagCreator.div;
import static j2html.TagCreator.each;
import static j2html.TagCreator.label;
import static j2html.TagCreator.textarea;

import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import j2html.TagCreator;
import j2html.attributes.Attr;
import j2html.tags.ContainerTag;
import j2html.tags.Tag;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.OptionalLong;
import org.apache.commons.lang3.RandomStringUtils;
import play.i18n.Messages;
import services.applicant.ValidationErrorMessage;
import views.style.BaseStyles;
import views.style.StyleUtils;
import views.style.Styles;

/** Utility class for rendering an input field with an optional label. */
public class FieldWithLabel {
  private static final ImmutableSet<String> STRING_TYPES =
      ImmutableSet.of("text", "checkbox", "radio", "date", "email");

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
  protected String screenReaderText = "";
  protected Messages messages;
  protected ImmutableSet<ValidationErrorMessage> fieldErrors = ImmutableSet.of();
  protected boolean showFieldErrors = true;
  protected boolean checked = false;
  protected boolean disabled = false;
  protected ImmutableList.Builder<String> referenceClassesBuilder = ImmutableList.builder();

  public FieldWithLabel(Tag fieldTag) {
    this.fieldTag = checkNotNull(fieldTag);
  }

  public static FieldWithLabel checkbox() {
    Tag fieldTag = TagCreator.input();
    return new FieldWithLabel(fieldTag).setFieldType("checkbox");
  }

  public static FieldWithLabel radio() {
    Tag fieldTag = TagCreator.input();
    return new FieldWithLabel(fieldTag).setFieldType("radio");
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

  public static FieldWithLabel email() {
    Tag fieldTag = TagCreator.input();
    return new FieldWithLabel(fieldTag).setFieldType("email");
  }

  /** Add a reference class from {@link views.style.ReferenceClasses} to this element. */
  public FieldWithLabel addReferenceClass(String referenceClass) {
    referenceClassesBuilder.add(referenceClass);
    return this;
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
    if (!STRING_TYPES.contains(this.fieldType)) {
      throw new RuntimeException(
          String.format(
              "setting a String value is not available on fields of type `%s`", this.fieldType));
    }

    this.fieldValue = value;
    return this;
  }

  public FieldWithLabel setValue(Optional<String> value) {
    if (!STRING_TYPES.contains(this.fieldType)) {
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

  public FieldWithLabel setScreenReaderText(String screenReaderText) {
    this.screenReaderText = screenReaderText;
    return this;
  }

  public FieldWithLabel setFieldErrors(
      Messages messages, ImmutableSet<ValidationErrorMessage> errors) {
    this.messages = messages;
    this.fieldErrors = errors;
    return this;
  }

  public FieldWithLabel showFieldErrors(boolean showFieldErrors) {
    this.showFieldErrors = showFieldErrors;
    return this;
  }

  public ContainerTag getContainer() {
    // In order for the labels to be associated with the fields (mandatory for screen readers)
    // we need an id.  Generate a reasonable one if none is provided.
    if (this.id.isEmpty()) {
      this.id = RandomStringUtils.randomAlphabetic(8);
    }
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
      // We only allow integer input.
      fieldTag.attr("oninput", "n=parseInt(this.value);this.value=Number.isNaN(n)?'':n;");
    } else {
      fieldTag.withValue(this.fieldValue);
    }

    boolean hasFieldErrors = !fieldErrors.isEmpty() && showFieldErrors;
    fieldTag
        .withClasses(
            StyleUtils.joinStyles(
                BaseStyles.INPUT, hasFieldErrors ? BaseStyles.FORM_FIELD_ERROR_BORDER_COLOR : ""))
        .withId(this.id)
        .withName(this.fieldName)
        .condAttr(this.disabled, Attr.DISABLED, "true")
        .withCondPlaceholder(!Strings.isNullOrEmpty(this.placeholderText), this.placeholderText)
        .condAttr(!Strings.isNullOrEmpty(this.formId), Attr.FORM, formId);

    if (this.fieldType.equals("checkbox") || this.fieldType.equals("radio")) {
      return getCheckboxContainer();
    }

    ContainerTag labelTag =
        label()
            .attr(Attr.FOR, this.id)
            // If the text is screen-reader text, then we want the label to be screen-reader
            // only.
            .withClass(labelText.isEmpty() ? Styles.SR_ONLY : BaseStyles.INPUT_LABEL)
            .withText(labelText.isEmpty() ? screenReaderText : labelText);

    return div(labelTag, fieldTag, buildFieldErrorsTag())
        .withClasses(
            StyleUtils.joinStyles(referenceClassesBuilder.build().toArray(new String[0])),
            BaseStyles.FORM_FIELD_MARGIN_BOTTOM);
  }

  /**
   * Swaps the order of the label and field, adds different styles, and possibly adds "checked"
   * attribute.
   */
  private ContainerTag getCheckboxContainer() {
    if (this.checked) {
      fieldTag.attr("checked");
    }

    return label()
        .withClasses(
            StyleUtils.joinStyles(referenceClassesBuilder.build().toArray(new String[0])),
            BaseStyles.CHECKBOX_LABEL,
            BaseStyles.FORM_FIELD_MARGIN_BOTTOM,
            labelText.isEmpty() ? Styles.W_MIN : "")
        .condAttr(!this.id.isEmpty(), Attr.FOR, this.id)
        .with(fieldTag.withClasses(BaseStyles.CHECKBOX))
        .withText(this.labelText);
  }

  private Tag buildFieldErrorsTag() {
    String[] referenceClasses =
        referenceClassesBuilder.build().stream().map(ref -> ref + "-error").toArray(String[]::new);
    return div(each(fieldErrors, error -> div(error.getMessage(messages))))
        .withClasses(
            StyleUtils.joinStyles(referenceClasses),
            StyleUtils.joinStyles(BaseStyles.FORM_ERROR_TEXT_XS, Styles.P_1),
            fieldErrors.isEmpty() || !showFieldErrors ? Styles.HIDDEN : "");
  }
}
