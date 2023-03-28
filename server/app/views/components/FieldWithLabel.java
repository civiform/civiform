package views.components;

import static j2html.TagCreator.div;
import static j2html.TagCreator.each;
import static j2html.TagCreator.label;

import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import j2html.TagCreator;
import j2html.attributes.Attr;
import j2html.tags.EmptyTag;
import j2html.tags.Tag;
import j2html.tags.attributes.IChecked;
import j2html.tags.attributes.IDisabled;
import j2html.tags.attributes.IName;
import j2html.tags.specialized.DivTag;
import j2html.tags.specialized.InputTag;
import j2html.tags.specialized.LabelTag;
import j2html.tags.specialized.TextareaTag;
import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.OptionalLong;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.StringUtils;
import play.data.validation.ValidationError;
import play.i18n.Messages;
import services.applicant.ValidationErrorMessage;
import views.style.BaseStyles;
import views.style.StyleUtils;

// NON_ABSTRACT_CLASS_ALLOWS_SUBCLASSING FieldWithLabel

/** Utility class for rendering an input field with an optional label. */
public class FieldWithLabel {

  private static final ImmutableSet<String> STRING_TYPES =
      ImmutableSet.of("text", "checkbox", "radio", "date", "email");

  private String fieldName = "";
  private String fieldType = "text";
  protected String fieldValue = "";

  /** For use with fields of type `number`. * */
  protected OptionalLong fieldValueNumber = OptionalLong.empty();
  /** For use with fields of type `number`. */
  protected OptionalLong minValue = OptionalLong.empty();
  /** For use with fields of type `number`. */
  protected OptionalLong maxValue = OptionalLong.empty();

  private String tagType = "";

  /** For use with fields of type `textarea`. */
  private OptionalLong rows = OptionalLong.empty();
  /** For use with fields of type `textarea`. */
  private OptionalLong cols = OptionalLong.empty();

  private String formId = "";
  private String id = "";
  private String labelText = "";
  private Optional<String> autocomplete = Optional.empty();
  protected String placeholderText = "";
  private String screenReaderText = "";
  private Messages messages;
  private ImmutableSet<ValidationError> fieldErrors = ImmutableSet.of();
  private boolean showFieldErrors = true;
  private boolean shouldForceAriaInvalid = false;
  private boolean checked = false;
  private boolean disabled = false;
  protected ImmutableList.Builder<String> referenceClassesBuilder = ImmutableList.builder();
  protected ImmutableList.Builder<String> styleClassesBuilder = ImmutableList.builder();
  private ImmutableList.Builder<String> ariaDescribedByBuilder = ImmutableList.builder();
  private final ImmutableSet.Builder<String> attributesSetBuilder = ImmutableSet.builder();

  private static final String MAX_INPUT_TEXT_LENGTH = "10000";

  private static final class FieldErrorsInfo {
    public String fieldErrorsId;
    public boolean hasFieldErrors;

    public FieldErrorsInfo(String fieldErrorsId, boolean hasFieldErrors) {
      this.fieldErrorsId = fieldErrorsId;
      this.hasFieldErrors = hasFieldErrors;
    }
  }

  /** Make all constructors protected * */
  protected FieldWithLabel() {}

  /** Choose which type of tag (one of these is called first) * */
  public static FieldWithLabel checkbox() {
    return new FieldWithLabel().setTagTypeInput().setFieldType("checkbox");
  }

  public static FieldWithLabel currency() {
    return new FieldWithLabel().setTagTypeInput().setFieldType("text").setIsCurrency();
  }

  public static FieldWithLabel radio() {
    return new FieldWithLabel().setTagTypeInput().setFieldType("radio");
  }

  public static FieldWithLabel input() {
    return new FieldWithLabel().setTagTypeInput().setFieldType("text");
  }

  public static FieldWithLabel number() {
    return new FieldWithLabel().setTagTypeInput().setFieldType("number");
  }

  public static FieldWithLabel date() {
    return new FieldWithLabel().setTagTypeInput().setFieldType("date");
  }

  public static FieldWithLabel textArea() {
    return new FieldWithLabel().setTagTypeTextarea().setFieldType("text");
  }

  public static FieldWithLabel email() {
    return new FieldWithLabel().setTagTypeInput().setFieldType("email");
  }

  /** Add a reference class from {@link views.style.ReferenceClasses} to this element. */
  public FieldWithLabel addReferenceClass(String referenceClass) {
    referenceClassesBuilder.add(referenceClass);
    return this;
  }

  /** Add a class for styling the label. */
  public FieldWithLabel addStyleClass(String styleClass) {
    styleClassesBuilder.add(styleClass);
    return this;
  }

  /** Public setters * */
  public FieldWithLabel setChecked(boolean checked) {
    this.checked = checked;
    return this;
  }

  public FieldWithLabel setFieldName(String fieldName) {
    this.fieldName = fieldName;
    return this;
  }

  public FieldWithLabel setFieldType(String fieldType) {
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

  FieldWithLabel setIsCurrency() {
    // There is no HTML currency input so we identify these with a custom attribute.
    this.setAttribute("currency");
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

  /**
   * Sets the autocomplete attribute.
   *
   * @param autocomplete this value must come from the predefined list here:
   *     https://www.w3.org/TR/WCAG21/#input-purposes.
   * @return this, for chaining.
   */
  public FieldWithLabel setAutocomplete(Optional<String> autocomplete) {
    this.autocomplete = autocomplete;
    return this;
  }

  /** Sets a valueless attribute. */
  public FieldWithLabel setAttribute(String attribute) {
    this.attributesSetBuilder.add(attribute);
    return this;
  }

  public FieldWithLabel setMin(OptionalLong value) {
    if (!getFieldType().equals("number")) {
      throw new RuntimeException(
          "setting an OptionalLong min value is only available on fields of type 'number'");
    }
    this.minValue = value;
    return this;
  }

  public FieldWithLabel setMax(OptionalLong value) {
    if (!getFieldType().equals("number")) {
      throw new RuntimeException(
          "setting an OptionalLong max value is only available on fields of type 'number'");
    }

    this.maxValue = value;
    return this;
  }

  public FieldWithLabel setRows(OptionalLong value) {
    if (!this.isTagTypeTextarea()) {
      throw new RuntimeException("setting rows is only available on fields of type 'textarea'");
    }

    this.rows = value;
    return this;
  }

  public FieldWithLabel setCols(OptionalLong value) {
    if (!this.isTagTypeTextarea()) {
      throw new RuntimeException("setting cols is only available on fields of type 'textarea'");
    }

    this.cols = value;
    return this;
  }

  public FieldWithLabel setValue(String value) {
    if (!STRING_TYPES.contains(getFieldType())) {
      throw new RuntimeException(
          String.format(
              "setting a String value is not available on fields of type `%s`", this.fieldType));
    }

    this.fieldValue = value;
    return this;
  }

  public FieldWithLabel setValue(Optional<String> value) {
    if (!STRING_TYPES.contains(getFieldType())) {
      throw new RuntimeException(
          "setting a String value is not available on fields of type 'number'");
    }
    value.ifPresent(s -> this.fieldValue = s);
    return this;
  }

  public FieldWithLabel setValue(OptionalInt value) {
    if (!getFieldType().equals("number")) {
      throw new RuntimeException(
          "setting an OptionalInt value is only available on fields of type `number`");
    }

    this.fieldValueNumber =
        value.isPresent() ? OptionalLong.of(value.getAsInt()) : OptionalLong.empty();
    return this;
  }

  public FieldWithLabel setValue(OptionalLong value) {
    if (!getFieldType().equals("number")) {
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

  /** Set the list of HTML tag IDs that should be used for a11y descriptions. */
  public FieldWithLabel setAriaDescribedByIds(ImmutableList<String> ariaDescribedByIds) {
    this.ariaDescribedByBuilder.addAll(ariaDescribedByIds);
    return this;
  }

  /**
   * Forceset the aria-invalid attribute on this field to true. This is useful for when there are
   * question level errors that this field does not know about.
   */
  public FieldWithLabel forceAriaInvalid() {
    this.shouldForceAriaInvalid = true;
    return this;
  }

  public FieldWithLabel setFieldErrors(
      Messages messages, ImmutableSet<ValidationErrorMessage> errors) {
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
    this.messages = messages;
    this.fieldErrors = ImmutableSet.of(error);

    return this;
  }

  public FieldWithLabel setFieldErrors(Messages messages, List<ValidationError> errors) {
    this.messages = messages;
    this.fieldErrors = ImmutableSet.copyOf(errors);

    return this;
  }

  public FieldWithLabel showFieldErrors(boolean showFieldErrors) {
    this.showFieldErrors = showFieldErrors;
    return this;
  }

  /** Attribute getters * */
  public String getFieldType() {
    return this.fieldType;
  }

  /** Internal getters for final tag generation * */
  private InputTag nonNumberGenTagApplyAttrs() {
    InputTag inputFieldTag = TagCreator.input();
    inputFieldTag.withType(getFieldType());
    applyAttributesFromSet(inputFieldTag);
    if (!this.fieldType.equals("number")) {
      inputFieldTag.withValue(this.fieldValue);
    } else {
      throw new RuntimeException("non-number tag expected");
    }

    return inputFieldTag;
  }

  public LabelTag getCheckboxTag() {
    InputTag inputFieldTag = nonNumberGenTagApplyAttrs();
    return checkboxApplyAttrsAndGenLabel(inputFieldTag);
  }

  private DivTag getNonNumberInputTag() throws RuntimeException {
    InputTag inputFieldTag = TagCreator.input();
    inputFieldTag.withType(getFieldType());
    applyAttributesFromSet(inputFieldTag);
    if (!this.fieldType.equals("number")) {
      inputFieldTag.withValue(this.fieldValue);
    } else {
      throw new RuntimeException("non-number tag expected");
    }
    return applyAttrsAndGenLabel(inputFieldTag);
  }

  /** Public final tag getters * */
  public DivTag getTextareaTag() throws RuntimeException {
    if (isTagTypeTextarea()) {
      TextareaTag textareaFieldTag = TagCreator.textarea();
      applyAttributesFromSet(textareaFieldTag);
      textareaFieldTag.withText(this.fieldValue);
      if (this.rows.isPresent()) {
        textareaFieldTag.withRows(String.valueOf(this.rows.getAsLong()));
      }
      if (this.cols.isPresent()) {
        textareaFieldTag.withCols(String.valueOf(this.cols.getAsLong()));
      }
      return applyAttrsAndGenLabel(textareaFieldTag);
    }

    throw new RuntimeException("needs to be textarea tag");
  }

  public LabelTag getRadioTag() {
    return getCheckboxTag();
  }

  public DivTag getCurrencyTag() {
    return getNonNumberInputTag();
  }

  public DivTag getInputTag() {
    return getNonNumberInputTag();
  }

  public DivTag getNumberTag() throws RuntimeException {
    InputTag inputFieldTag = TagCreator.input();
    inputFieldTag.withType(getFieldType());
    applyAttributesFromSet(inputFieldTag);
    if (this.fieldType.equals("number")) {
      numberTagApplyAttrs(inputFieldTag);
    } else {
      throw new RuntimeException("number tag expected");
    }
    return applyAttrsAndGenLabel(inputFieldTag);
  }

  public DivTag getDateTag() {
    return getNonNumberInputTag();
  }

  public DivTag getEmailTag() {
    return getNonNumberInputTag();
  }

  protected void applyAttributesFromSet(Tag fieldTag) {
    this.attributesSetBuilder.build().forEach(attr -> fieldTag.attr(attr, null));
  }

  private DivTag buildFieldErrorsTag(String id) {
    String[] referenceClasses =
        referenceClassesBuilder.build().stream().map(ref -> ref + "-error").toArray(String[]::new);
    return div(each(fieldErrors, error -> div(error.format(messages))))
        .withId(id)
        .withClasses(
            StyleUtils.joinStyles(referenceClasses),
            StyleUtils.joinStyles(BaseStyles.FORM_ERROR_TEXT_XS, "p-1"),
            fieldErrors.isEmpty() || !showFieldErrors ? "hidden" : "");
  }

  private void genRandIdIfEmpty() {
    // In order for the labels to be associated with the fields (mandatory for screen readers)
    // we need an id.  Generate a reasonable one if none is provided.
    if (this.id.isEmpty()) {
      this.id = RandomStringUtils.randomAlphabetic(8);
    }
  }

  private LabelTag genLabelTag() {

    return label()
        .withFor(this.id)
        // If the text is screen-reader text, then we want the label to be screen-reader
        // only.
        .withClass(labelText.isEmpty() ? "sr-only" : BaseStyles.INPUT_LABEL)
        .withText(labelText.isEmpty() ? screenReaderText : labelText);
  }

  private DivTag buildBaseContainer(Tag fieldTag, Tag labelTag, String fieldErrorsId) {
    return div(
            labelTag,
            div(fieldTag, buildFieldErrorsTag(fieldErrorsId)).withClasses("flex", "flex-col"))
        .withClasses(
            StyleUtils.joinStyles(referenceClassesBuilder.build().toArray(new String[0])),
            BaseStyles.FORM_FIELD_MARGIN_BOTTOM);
  }

  private boolean hasFieldErrors() {
    return !fieldErrors.isEmpty() && showFieldErrors;
  }

  protected void numberTagApplyAttrs(InputTag fieldTag) {
    // Setting inputmode to decimal gives iOS users a more accessible keyboard
    fieldTag.attr("inputmode", "decimal");

    // Setting step to any disables the built-in HTML validation so we can use our
    // custom validation message to enforce integers.
    fieldTag.withStep("any");

    // Set min and max values for client-side validation
    if (this.minValue.isPresent()) {
      fieldTag.withMin(String.valueOf(minValue.getAsLong()));
    }
    if (this.maxValue.isPresent()) {
      fieldTag.withMax(String.valueOf(maxValue.getAsLong()));
    }

    // For number types, only set the value if it's present since there is no empty string
    // equivalent for numbers.
    if (this.fieldValueNumber.isPresent()) {
      fieldTag.withValue(String.valueOf(this.fieldValueNumber.getAsLong()));
    }
  }

  private <T extends Tag<T> & IName<T> & IDisabled<T>> void generalApplyAttrsClassesToTag(
      T fieldTag, boolean hasFieldErrors) {
    // Here we use `.condAttr` instead of the more typesafe methods in 3 instances  here
    // since not all types of the `fieldTag` argument passed to this have those attributes.
    //
    // Adding useless attributes does not hurt the DOM, and helps us avoid putting those calls
    // before the calls to this method, thus simplifying the code.
    fieldTag
        .withClasses(
            StyleUtils.joinStyles(hasFieldErrors ? BaseStyles.INPUT_WITH_ERROR : BaseStyles.INPUT))
        .withId(this.id)
        .withName(this.fieldName)
        .withCondDisabled(this.disabled)
        .condAttr(this.autocomplete.isPresent(), Attr.AUTOCOMPLETE, this.autocomplete.orElse(""))
        .condAttr(
            !Strings.isNullOrEmpty(this.placeholderText), Attr.PLACEHOLDER, this.placeholderText)
        .condAttr(!Strings.isNullOrEmpty(this.formId), Attr.FORM, formId);
  }

  protected <T extends Tag<T> & IName<T> & IDisabled<T>>
      FieldErrorsInfo applyAttrsGenFieldErrorsInfo(T fieldTag) {
    String fieldErrorsId = String.format("%s-errors", this.id);
    boolean hasFieldErrors = hasFieldErrors();

    FieldErrorsInfo fieldErrorsInfo = new FieldErrorsInfo(fieldErrorsId, hasFieldErrors);
    if (fieldErrorsInfo.hasFieldErrors) {
      ImmutableList.Builder<String> tempBuilder = ImmutableList.builder();
      // Add error to front.
      tempBuilder.add(fieldErrorsId);
      tempBuilder.addAll(ariaDescribedByBuilder.build());
      ariaDescribedByBuilder = tempBuilder;
    }
    ImmutableList<String> ariaIds = ariaDescribedByBuilder.build();

    fieldTag.condAttr(!ariaIds.isEmpty(), "aria-describedby", StringUtils.join(ariaIds, " "));
    fieldTag.condAttr(
        shouldForceAriaInvalid || fieldErrorsInfo.hasFieldErrors, "aria-invalid", "true");
    fieldTag.attr("maxlength", MAX_INPUT_TEXT_LENGTH);

    generalApplyAttrsClassesToTag(fieldTag, hasFieldErrors);

    return fieldErrorsInfo;
  }

  protected <T extends EmptyTag<T> & IChecked<T> & IName<T> & IDisabled<T>>
      LabelTag checkboxApplyAttrsAndGenLabel(T fieldTag) throws RuntimeException {
    genRandIdIfEmpty();
    // Apply attributes
    applyAttrsGenFieldErrorsInfo(fieldTag);

    // Generate label / container
    if (getFieldType().equals("checkbox") || getFieldType().equals("radio")) {
      return getCheckboxContainer(fieldTag);
    }
    throw new RuntimeException("needs to be a checkbox or radio type for this method");
  }

  protected <T extends Tag<T> & IName<T> & IDisabled<T>> DivTag applyAttrsAndGenLabel(T fieldTag) {
    genRandIdIfEmpty();
    // Apply attributes
    FieldErrorsInfo fieldErrorsInfo = applyAttrsGenFieldErrorsInfo(fieldTag);
    LabelTag labelTag = genLabelTag();
    // Generate label / container
    return buildBaseContainer(fieldTag, labelTag, fieldErrorsInfo.fieldErrorsId);
  }

  /**
   * Swaps the order of the label and field, adds different styles, and possibly adds "checked"
   * attribute.
   */
  private <T extends EmptyTag<T> & IChecked<T>> LabelTag getCheckboxContainer(T fieldTag) {
    if (this.checked) {
      fieldTag.isChecked();
    }

    return label()
        .withClasses(
            StyleUtils.joinStyles(referenceClassesBuilder.build().toArray(new String[0])),
            StyleUtils.joinStyles(styleClassesBuilder.build().toArray(new String[0])),
            BaseStyles.CHECKBOX_LABEL,
            BaseStyles.FORM_FIELD_MARGIN_BOTTOM,
            labelText.isEmpty() ? "w-min" : "")
        .withCondFor(!this.id.isEmpty(), this.id)
        .with(fieldTag.withClasses(BaseStyles.CHECKBOX))
        .withText(this.labelText);
  }

  private FieldWithLabel setTagTypeInput() {
    tagType = "input";
    return this;
  }

  private FieldWithLabel setTagTypeTextarea() {
    tagType = "textarea";
    return this;
  }

  protected boolean isTagTypeInput() {
    return tagType.equals("input");
  }

  protected boolean isTagTypeTextarea() {
    return tagType.equals("textarea");
  }
}
