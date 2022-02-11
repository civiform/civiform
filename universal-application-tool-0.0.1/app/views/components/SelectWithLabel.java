package views.components;

import static j2html.TagCreator.option;
import static j2html.TagCreator.select;
import static views.HtmlAttributes.ARIA_REQUIRED;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import j2html.attributes.Attr;
import j2html.tags.ContainerTag;
import j2html.tags.Tag;

/** Utility class for rendering a select input field with an optional label. */
public class SelectWithLabel extends FieldWithLabel {

  private ImmutableMap<String, String> options = ImmutableMap.of();
  private ImmutableList<ContainerTag> customOptions = ImmutableList.of();

  public SelectWithLabel() {
    super(select());
  }

  @Override
  public SelectWithLabel addReferenceClass(String referenceClass) {
    referenceClasses.add(referenceClass);
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
  public SelectWithLabel setCustomOptions(ImmutableList<ContainerTag> options) {
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

  @Override
  public ContainerTag getContainer() {
    Tag placeholder = option(placeholderText).withValue("").attr(Attr.HIDDEN);
    if (this.fieldValue.isEmpty()) {
      placeholder.attr(Attr.SELECTED);
    }
    ((ContainerTag) fieldTag).with(placeholder);

    fieldTag.condAttr(isRequired, ARIA_REQUIRED, "true");

    // Either set the options to be custom options or create options from the (text, value) pairs.
    if (!this.customOptions.isEmpty()) {
      this.customOptions.forEach(option -> ((ContainerTag) fieldTag).with(option));
    } else {
      this.options.forEach(
          (text, value) -> {
            Tag optionTag = option(text).withValue(value);
            if (value.equals(this.fieldValue)) {
              optionTag.attr(Attr.SELECTED);
            }
            ((ContainerTag) fieldTag).with(optionTag);
          });
    }

    return super.getContainer();
  }
}
