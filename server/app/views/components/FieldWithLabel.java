package views.components;

import static com.google.common.base.Preconditions.checkNotNull;
import static j2html.TagCreator.div;
import static j2html.TagCreator.each;
import static j2html.TagCreator.label;
import static j2html.TagCreator.textarea;

import j2html.tags.Tag;
import j2html.tags.specialized.DivTag;
import j2html.tags.specialized.LabelTag;
import j2html.tags.specialized.InputTag;
import j2html.tags.specialized.TextareaTag;

import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import j2html.TagCreator;
import j2html.attributes.Attr;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.OptionalLong;
import org.apache.commons.lang3.RandomStringUtils;
import play.data.validation.ValidationError;
import play.i18n.Messages;
import services.applicant.ValidationErrorMessage;
import views.style.BaseStyles;
import views.style.StyleUtils;
import views.style.Styles;

/** Utility class for rendering an input field with an optional label. */
public class FieldWithLabel {

  private static final ImmutableSet<String> STRING_TYPES =
      ImmutableSet.of("text", "checkbox", "radio", "date", "email");

  protected String fieldName = "";
  protected String fieldType = "text";
  protected String fieldValue = "";
  protected String tagType = "";

  /** For use with fields of type `number`. */
  protected OptionalLong fieldValueNumber = OptionalLong.empty();

  protected OptionalLong minValue = OptionalLong.empty();
  protected OptionalLong maxValue = OptionalLong.empty();

  protected String formId = "";
  protected String id = "";
  protected String labelText = "";
  protected String placeholderText = "";
  protected String screenReaderText = "";
  protected Messages messages;
  protected ImmutableSet<ValidationError> fieldErrors = ImmutableSet.of();
  protected boolean showFieldErrors = true;
  protected boolean checked = false;
  protected boolean disabled = false;
  protected ImmutableList.Builder<String> referenceClassesBuilder = ImmutableList.builder();

  // TAG CREATORS & GETTERS //
  public static FieldWithLabel checkbox() {
    return new FieldWithLabel().setFieldType("checkbox");
  }

  public static FieldWithLabel currency() {
    return new FieldWithLabel()
      .setFieldType("text")
      .setIsCurrency()
      .setTagType("input");
  }

  public static FieldWithLabel radio() {
    return new FieldWithLabel()
      .setFieldType("radio")
      .setTagType("input");
  }

  public static FieldWithLabel input() {
    return new FieldWithLabel()
      .setFieldType("text")
      .setTagType("input");
  }

  public static FieldWithLabel number() {
    return new FieldWithlabel()
      .setFieldType("number")
      .setTagType("input");
  }

  public static FieldWithLabel date() {
    return new FieldWithlabel()
      .setFieldType("date")
      .setTagType("input");
  }

  public static FieldWithLabel textArea() {
    return new FieldWithLabel()
      .setFieldType("text")
      .setTagType("textarea");
  }

  public static FieldWithLabel email() {
    return new FieldWithLabel()
      .setFieldType("email")
      .setTagType("input");
  }

  // ATTRIBUTE SETTERS //
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

  public FieldWithLabel setTagType(String tagType) {
    this.tagType = tagType;
    return this;
  }

  public FieldWithLabel setFieldType(String fieldType) {
    // TODO Just remove this
    //this.fieldTag.attr("type", fieldType);
    // TODO add to tag later
    this.fieldType = fieldType;
    return this;
  }

  public FieldWithLabel setFormId(String formId) {
    // TODO add to tag later
    this.formId = formId;
    return this;
  }

  public FieldWithLabel setId(String inputId) {
    // TODO add to tag later
    this.id = inputId;
    return this;
  }

  FieldWithLabel setIsCurrency() {
    // There is no HTML currency input so we identify these with a custom attribute.
    // TODO add to tag later
    this.setAttribute("currency");
    return this;
  }

  public FieldWithLabel setLabelText(String labelText) {
    // TODO add to tag later
    this.labelText = labelText;
    return this;
  }

  public FieldWithLabel setPlaceholderText(String placeholder) {
    // TODO add to tag later
    this.placeholderText = placeholder;
    return this;
  }

  /** Sets a valueless attribute. */
  public FieldWithLabel setAttribute(String attribute) {
    // TODO Add list field to store these before setting them after Tag creation
    // TODO add to tag later
    this.fieldTag.attr(attribute, null);
    return this;
  }

  public FieldWithLabel setMin(OptionalLong value) {
    if (!this.fieldType.equals("number")) {
      throw new RuntimeException(
          "setting an OptionalLong min value is only available on fields of type 'number'");
    }
    // TODO add to tag later
    this.minValue = value;
    return this;
  }

  public FieldWithLabel setMax(OptionalLong value) {
    if (!this.fieldType.equals("number")) {
      throw new RuntimeException(
          "setting an OptionalLong max value is only available on fields of type 'number'");
    }

    // TODO add to tag later
    this.maxValue = value;
    return this;
  }

  public FieldWithLabel setValue(String value) {
    if (!STRING_TYPES.contains(this.fieldType)) {
      throw new RuntimeException(
          String.format(
              "setting a String value is not available on fields of type `%s`", this.fieldType));
    }

    // TODO add to tag later
    this.fieldValue = value;
    return this;
  }

  public FieldWithLabel setValue(Optional<String> value) {
    if (!STRING_TYPES.contains(this.fieldType)) {
      throw new RuntimeException(
          "setting a String value is not available on fields of type 'number'");
    }
    // TODO add to tag later
    value.ifPresent(s -> this.fieldValue = s);
    return this;
  }

  public FieldWithLabel setValue(OptionalInt value) {
    if (!this.fieldType.equals("number")) {
      throw new RuntimeException(
          "setting an OptionalInt value is only available on fields of type `number`");
    }

    // TODO add to tag later
    this.fieldValueNumber =
        value.isPresent() ? OptionalLong.of(value.getAsInt()) : OptionalLong.empty();
    return this;
  }

  public FieldWithLabel setValue(OptionalLong value) {
    if (!this.fieldType.equals("number")) {
      throw new RuntimeException(
          "setting an OptionalLong value is only available on fields of type `number`");
    }

    // TODO add to tag later
    this.fieldValueNumber = value;
    return this;
  }

  public FieldWithLabel setDisabled(boolean disabled) {
    // TODO add to tag later
    this.disabled = disabled;
    return this;
  }

  public FieldWithLabel setScreenReaderText(String screenReaderText) {
    // TODO add to tag later
    this.screenReaderText = screenReaderText;
    return this;
  }

  public FieldWithLabel setFieldErrors(
      Messages messages, ImmutableSet<ValidationErrorMessage> errors) {
    // TODO add to tag later
    this.messages = messages;
    this.fieldErrors =
        errors.stream()
            .map(
                (ValidationErrorMessage vem) ->
                    new ValidationError(vem.key().getKeyName(), vem.key().getKeyName(), vem.args()))
            .collect(ImmutableSet.toImmutableSet());

    return this;
  }

  public FieldWithLabel setFieldErrors(Messages messages, ValidationError error) {
    // TODO add to tag later
    this.messages = messages;
    this.fieldErrors = ImmutableSet.of(error);

    return this;
  }

  public FieldWithLabel showFieldErrors(boolean showFieldErrors) {
    // TODO add to tag later
    this.showFieldErrors = showFieldErrors;
    return this;
  }

  protected DivTag getInputTag(InputTag fieldTag) {
    // In order for the labels to be associated with the fields (mandatory for screen readers)
    // we need an id.  Generate a reasonable one if none is provided.

    // TODO goes into all tag types
    if (this.id.isEmpty()) {
      this.id = RandomStringUtils.randomAlphabetic(8);
    }
    // TODO number tag only
    if (getFieldType().equals("number")) {
      // Setting inputmode to decimal gives iOS users a more accessible keyboard
      fieldTag.attr("inputmode", "decimal");

      // Setting step to any disables the built-in HTML validation so we can use our
      // custom validation message to enforce integers.
      fieldTag.attr("step", "any");

      // Set min and max values for client-side validation
      if (this.minValue.isPresent()) {
        fieldTag.attr("min", minValue.getAsLong());
      }
      if (this.maxValue.isPresent()) {
        fieldTag.attr("max", maxValue.getAsLong());
      }

      // For number types, only set the value if it's present since there is no empty string
      // equivalent for numbers.
      if (this.fieldValueNumber.isPresent()) {
        fieldTag.attr("value", String.valueOf(this.fieldValueNumber.getAsLong()));
      }
    } 
    // TODO NOT textarea or number tag types
    else {
      fieldTag.attr("value", this.fieldValue);
    }

    allTagsSetClassesAndAttrs(fieldTag);
    if (this.fieldType.equals("checkbox") || this.fieldType.equals("radio")) {
      return div(getCheckboxContainer());
    }

    return nonCheckboxRadioFinalBuild(fieldTag);
  }

  protected void allTagsSetClassesAndAttrs(Tag fieldTag) {

    boolean hasFieldErrors = !fieldErrors.isEmpty() && showFieldErrors;
    fieldTag
        .withClasses(
            StyleUtils.joinStyles(
                BaseStyles.INPUT, hasFieldErrors ? BaseStyles.FORM_FIELD_ERROR_BORDER_COLOR : ""))
        .withId(this.id)
        .attr("name", this.fieldName)
        .condAttr(this.disabled, Attr.DISABLED, "true")
        .condAttr(!Strings.isNullOrEmpty(this.placeholderText), Attr.PLACEHOLDER, this.placeholderText)
        .condAttr(!Strings.isNullOrEmpty(this.formId), Attr.FORM, formId);
  }

  protected DivTag nonCheckboxRadioFinalBuild(Tag fieldTag) {
    LabelTag labelTag =
        label()
            .attr(Attr.FOR, this.id)
            // If the text is screen-reader text, then we want the label to be screen-reader
            // only.
            .withClass(labelText.isEmpty() ? Styles.SR_ONLY : BaseStyles.INPUT_LABEL)
            .withText(labelText.isEmpty() ? screenReaderText : labelText);

    return div(
            labelTag,
            div(fieldTag, buildFieldErrorsTag()).withClasses(Styles.FLEX, Styles.FLEX_COL))
        .withClasses(
            StyleUtils.joinStyles(referenceClassesBuilder.build().toArray(new String[0])),
            BaseStyles.FORM_FIELD_MARGIN_BOTTOM);
  }

  protected DivTag getTextareaTag(TextareaTag fieldTag) {
    // In order for the labels to be associated with the fields (mandatory for screen readers)
    // we need an id.  Generate a reasonable one if none is provided.

    // TODO goes into all tag types
    if (this.id.isEmpty()) {
      this.id = RandomStringUtils.randomAlphabetic(8);
    }
    // TODO textarea tag only
    if (fieldTag.getTagName().equals("textarea")) {
      // Have to recreate the field here in case the value is modified.
      TextareaTag textAreaTag = textarea().attr("type", "text").withText(this.fieldValue);
    }

    allTagsSetClassesAndAttrs(fieldTag);

    return nonCheckboxRadioFinalBuild(fieldTag);
  }

  public DivTag getContainer() {
    TextareaTag textareaFieldTag;
    InputTag inputFieldTag;

    if (getTagType().equals("textarea")) {
      textareaFieldTag = textarea();
      textareaFieldTag.withType(getTagType());
      return getTextareaTag(textareaFieldTag);
    }

    inputFieldTag = input();
    inputFieldTag.withType(getTagType());
    return getInputTag(inputFieldTag);
  }

  /**
   * Swaps the order of the label and field, adds different styles, and possibly adds "checked"
   * attribute.
   */
  private LabelTag getCheckboxContainer() {
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

  private DivTag buildFieldErrorsTag() {
    String[] referenceClasses =
        referenceClassesBuilder.build().stream().map(ref -> ref + "-error").toArray(String[]::new);
    return div(each(fieldErrors, error -> div(error.format(messages))))
        .withClasses(
            StyleUtils.joinStyles(referenceClasses),
            StyleUtils.joinStyles(BaseStyles.FORM_ERROR_TEXT_XS, Styles.P_1),
            fieldErrors.isEmpty() || !showFieldErrors ? Styles.HIDDEN : "");
  }
}
