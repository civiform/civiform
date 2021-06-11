package views.components;

import static j2html.TagCreator.option;
import static j2html.TagCreator.select;

import com.google.common.collect.ImmutableMap;
import j2html.attributes.Attr;
import j2html.tags.ContainerTag;
import j2html.tags.Tag;
import java.util.Map;

public class SelectWithLabel extends FieldWithLabel {

  private ImmutableMap<String, String> options = ImmutableMap.of();

  public SelectWithLabel() {
    super(select());
  }

  @Override
  public SelectWithLabel addReferenceClass(String referenceClass) {
    referenceClassesBuilder.add(referenceClass);
    return this;
  }

  public SelectWithLabel setOptions(ImmutableMap<String, String> options) {
    this.options = options;
    return this;
  }

  public SelectWithLabel setOptions(Map.Entry<String, String> option) {
    this.options = ImmutableMap.of(option.getKey(), option.getValue());
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
    for (Map.Entry<String, String> option : this.options.entrySet()) {
      Tag optionTag = option(option.getKey()).withValue(option.getValue());
      if (option.getValue().equals(this.fieldValue)) {
        optionTag.attr(Attr.SELECTED);
      }
      ((ContainerTag) fieldTag).with(optionTag);
    }
    return super.getContainer();
  }
}
