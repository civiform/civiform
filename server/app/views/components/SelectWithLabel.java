package views.components;

import static j2html.TagCreator.option;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import j2html.TagCreator;
import j2html.tags.specialized.DivTag;
import j2html.tags.specialized.OptionTag;
import j2html.tags.specialized.SelectTag;

/** Utility class for rendering a select input field with an optional label. */
public class SelectWithLabel extends FieldWithLabel {

  private ImmutableMap<String, String> options = ImmutableMap.of();
  private ImmutableList<OptionTag> customOptions = ImmutableList.of();

  public SelectWithLabel() {
    super();
  }

  @Override
  public SelectWithLabel addReferenceClass(String referenceClass) {
    referenceClassesBuilder.add(referenceClass);
    return this;
  }

  /**
   * Keys are the user-visible text; values are the html {@code value} that is submitted in the
   * form.
   */
  public SelectWithLabel setOptions(ImmutableMap<String, String> options) {
    this.options = options;
    return this;
  }

  /**
   * If you want more flexibility over your options (for example, if you want to add individual
   * classes or other attributes), set custom options here.
   */
  public SelectWithLabel setCustomOptions(ImmutableList<OptionTag> options) {
    this.customOptions = options;
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

  public DivTag getSelectTag() {
    SelectTag fieldTag = TagCreator.select();
    OptionTag placeholder = option(placeholderText).withValue("").isHidden();

    if (this.fieldValue.isEmpty()) {
      placeholder.isSelected();
    }
    fieldTag.with(placeholder);

    // Either set the options to be custom options or create options from the (text, value) pairs.
    if (!this.customOptions.isEmpty()) {
      this.customOptions.forEach(option -> fieldTag.with(option));
    } else {
      this.options.forEach(
          (text, value) -> {
            OptionTag optionTag = option(text).withValue(value);
            if (value.equals(this.fieldValue)) {
              optionTag.isSelected();
            }
            fieldTag.with(optionTag);
          });
    }

    return applyAttrsAndGenLabel(fieldTag);
  }
}
