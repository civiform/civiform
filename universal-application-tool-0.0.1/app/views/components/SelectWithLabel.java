package views.components;

import static j2html.TagCreator.option;
import static j2html.TagCreator.select;

import com.google.common.collect.ImmutableList;
import j2html.attributes.Attr;
import j2html.tags.ContainerTag;
import j2html.tags.Tag;
import java.util.AbstractMap.SimpleEntry;

public class SelectWithLabel extends FieldWithLabel {

  private ImmutableList<SimpleEntry<String, String>> options = ImmutableList.of();

  public SelectWithLabel(String inputId) {
    super(select(), inputId);
  }

  public SelectWithLabel setOptions(ImmutableList<SimpleEntry<String, String>> options) {
    if (this.isRendered) {
      return this;
    }
    this.options = options;
    return this;
  }

  @Override
  public SelectWithLabel setId(String inputId) {
    super.setId(inputId);
    return this;
  }

  @Override
  public SelectWithLabel setLabelText(String labelText) {
    super.setLabelText(labelText);
    return this;
  }

  @Override
  public SelectWithLabel setValue(String value) {
    super.setValue(value);
    return this;
  }

  @Override
  public ContainerTag getContainer() {
    if (!this.isRendered) {
      for (SimpleEntry<String, String> option : this.options) {
        Tag optionTag = option(option.getKey()).withValue(option.getValue());
        if (option.getValue().equals(this.fieldValue)) {
          optionTag.attr(Attr.SELECTED);
        }
        ((ContainerTag) fieldTag).with(optionTag);
      }
    }
    return super.getContainer();
  }
}
