package views.components;

import static j2html.TagCreator.option;

import com.google.auto.value.AutoValue;
import com.google.common.collect.ImmutableList;
import j2html.TagCreator;
import j2html.tags.specialized.DivTag;
import j2html.tags.specialized.OptionTag;
import j2html.tags.specialized.SelectTag;
import views.style.ReferenceClasses;

/** Utility class for rendering a select input field with an optional label. */
public final class SelectWithLabel extends FieldWithLabel {

  private ImmutableList<OptionValue> options = ImmutableList.of();
  private ImmutableList<OptionTag> customOptions = ImmutableList.of();

  @Override
  public SelectWithLabel addReferenceClass(String referenceClass) {
    referenceClassesBuilder.add(referenceClass);
    return this;
  }

  /** Sets the options associated with the select element. */
  public SelectWithLabel setOptions(ImmutableList<OptionValue> options) {
    this.options = options;
    return this;
  }

  /**
   * If you want more flexibility over your options (for example, if you want to add individual
   * classes or other attributes), set custom options here.
   */
  public SelectWithLabel setCustomOptions(ImmutableList<OptionTag> customOptions) {
    this.customOptions = customOptions;
    return this;
  }

  @Override
  public SelectWithLabel setFieldName(String fieldName) {
    super.setFieldName(fieldName);
    return this;
  }

  @Override
  public SelectWithLabel setFormId(String formId) {
    super.setFormId(formId);
    return this;
  }

  @Override
  public SelectWithLabel setId(String fieldId) {
    super.setId(fieldId);
    return this;
  }

  @Override
  public SelectWithLabel setLabelText(String labelText) {
    super.setLabelText(labelText);
    return this;
  }

  @Override
  public SelectWithLabel setPlaceholderText(String placeholder) {
    super.setPlaceholderText(placeholder);
    return this;
  }

  @Override
  public SelectWithLabel setValue(String value) {
    super.setValue(value);
    return this;
  }

  @Override
  public SelectWithLabel setDisabled(boolean disabled) {
    super.setDisabled(disabled);
    return this;
  }

  @Override
  public SelectWithLabel setAriaDescribedByIds(ImmutableList<String> ariaDescribedByIds) {
    super.setAriaDescribedByIds(ariaDescribedByIds);
    return this;
  }

  @Override
  public SelectWithLabel forceAriaInvalid() {
    super.forceAriaInvalid();
    return this;
  }

  public DivTag getSelectTag() {
    SelectTag fieldTag = TagCreator.select();
    OptionTag placeholder = option(placeholderText).withValue("").isHidden();

    if (this.fieldValue.isEmpty()) {
      placeholder.isSelected();
    }
    fieldTag.with(placeholder);

    // Either set the options to be custom options or create options from the (text, value) pairs.
    if (!customOptions.isEmpty()) {
      customOptions.forEach(option -> fieldTag.with(option));
    } else {
      fieldTag.with(
          options.stream()
              .map(
                  optionData -> {
                    return option(optionData.label())
                        .withClasses(
                            ReferenceClasses.MULTI_OPTION_QUESTION_OPTION,
                            ReferenceClasses.MULTI_OPTION_VALUE)
                        .withValue(optionData.value())
                        .withCondSelected(optionData.value().equals(fieldValue));
                  }));
    }

    return applyAttrsAndGenLabel(fieldTag);
  }

  @AutoValue
  public abstract static class OptionValue {
    /** The user-visible option text. */
    public abstract String label();

    /** The HTML value that is submitted in the form. */
    public abstract String value();

    public static Builder builder() {
      return new AutoValue_SelectWithLabel_OptionValue.Builder();
    }

    @AutoValue.Builder
    public abstract static class Builder {
      public abstract Builder setLabel(String v);

      public abstract Builder setValue(String v);

      public abstract OptionValue build();
    }
  }
}
