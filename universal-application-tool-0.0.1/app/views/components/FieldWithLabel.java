package views.components;

import static j2html.TagCreator.div;
import static j2html.TagCreator.label;
import static j2html.TagCreator.textarea;

import j2html.TagCreator;
import j2html.attributes.Attr;
import j2html.tags.ContainerTag;
import j2html.tags.Tag;
import views.BaseStyles;
import views.Styles;

public class FieldWithLabel {
  private static final String[] CORE_FIELD_CLASSES = {
    BaseStyles.FIELD_BACKGROUND_COLOR,
    BaseStyles.FIELD_BORDER_COLOR,
    Styles.BLOCK,
    Styles.BORDER,
    Styles.P_2,
    Styles.W_FULL
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

  protected Tag fieldTag;
  protected String fieldValue = "";
  protected String formId = "";
  protected String labelText = "";
  protected String placeholderText = "";
  protected String id = "";

  public FieldWithLabel(Tag fieldTag) {
    this.fieldTag = fieldTag.withClasses(FieldWithLabel.CORE_FIELD_CLASSES);
  }

  public static FieldWithLabel input() {
    Tag fieldTag = TagCreator.input().withType("text");
    return new FieldWithLabel(fieldTag);
  }

  public static FieldWithLabel textArea() {
    Tag fieldTag = textarea().withType("text");
    return new FieldWithLabel(fieldTag);
  }

  public FieldWithLabel setId(String inputId) {
    this.id = inputId;
    return this;
  }

  public FieldWithLabel setFormId(String formId) {
    this.formId = formId;
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
    this.fieldValue = value;
    return this;
  }

  public ContainerTag getContainer() {
    if (fieldTag.getTagName().equals("textarea")) {
      // Have to recreate the field here in case the value is modified.
      ContainerTag textAreaTag =
          textarea()
              .withType("text")
              .withClasses(FieldWithLabel.CORE_FIELD_CLASSES)
              .withText(this.fieldValue);
      fieldTag = textAreaTag;
    } else {
      fieldTag.withValue(this.fieldValue);
    }
    fieldTag.withId(this.id).withName(id).withPlaceholder(this.placeholderText);

    if (formId.length() > 0) {
      fieldTag.attr("form", formId);
    }

    ContainerTag labelTag =
        label()
            .attr(Attr.FOR, this.id)
            .withClasses(FieldWithLabel.CORE_LABEL_CLASSES)
            .withText(this.labelText);
    return div(labelTag, fieldTag).withClasses(Styles.MX_4, Styles.MB_6);
  }
}
