package views.components;

import static j2html.TagCreator.option;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import j2html.TagCreator;
import j2html.attributes.Attr;
import j2html.tags.Tag;
import j2html.tags.specialized.DivTag;
import j2html.tags.specialized.LabelTag;
import j2html.tags.specialized.OptionTag;
import j2html.tags.specialized.SelectTag;

/** Utility class for rendering a select input field with an optional label. */
public class SelectWithLabel extends FieldWithLabel {

  private static final Logger logger = LoggerFactory.getLogger(SelectWithLabel.class);
  private ImmutableMap<String, String> options = ImmutableMap.of();
  private ImmutableList<OptionTag> customOptions = ImmutableList.of();

  public SelectWithLabel() {
    super();
    this.setFieldType("text");
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

  @Override
  protected void applyAttributesFromList(Tag fieldTag) {
    // Same as the corresponding superclass method except without the line
    // line that applies the `type="text"` html attribute
    this.attributesListBuilder.build().forEach(attr -> fieldTag.attr(attr, null));
  }

  protected DivTag getSelectTagContainer(SelectTag fieldTag) {
    genRandIdIfEmpty();
    if (this.fieldType.equals("number")) {
      numberTagApplyAttrs(fieldTag);
      // For number types, only set the value if it's present since there is no empty string
      // equivalent for numbers.
      if (this.fieldValueNumber.isPresent()) {
        fieldTag.attr("value", String.valueOf(this.fieldValueNumber.getAsLong()));
      }
    } else {
      fieldTag.attr("value", this.fieldValue);
    }

    String fieldErrorsId = String.format("%s-errors", this.id);
    boolean hasFieldErrors = getHasFieldErrors();
    if (hasFieldErrors) {
      fieldTag.attr("aria-invalid", "true");
      fieldTag.attr("aria-describedBy", fieldErrorsId);
    }

    generalApplyAttrsClassesToTag(fieldTag, hasFieldErrors);

    if (this.fieldType.equals("checkbox") || this.fieldType.equals("radio")) {
      return super.getCheckboxContainer(fieldTag);
    }

    LabelTag labelTag = super.genLabelTag();

    return super.wrapInDivTag(fieldTag, labelTag, fieldErrorsId);
  }

  @Override
  public DivTag getContainer() {
    SelectTag fieldTag = TagCreator.select();
    OptionTag placeholder = option(placeholderText).withValue("").attr(Attr.HIDDEN);

    if (this.fieldValue.isEmpty()) {
      placeholder.attr(Attr.SELECTED);
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
              optionTag.attr(Attr.SELECTED);
            }
            fieldTag.with(optionTag);
          });
    }

    return super.getTagContainer(fieldTag);
  }
}
