package views.components;

import static j2html.TagCreator.div;
import static j2html.TagCreator.input;
import static j2html.TagCreator.label;
import static j2html.TagCreator.textarea;

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
  protected String labelText = "";
  protected String id;

  public FieldWithLabel(Tag fieldTag, String inputId) {
    this.id = inputId;
    this.fieldTag = fieldTag.withId(id).withName(id).withClasses(FieldWithLabel.CORE_FIELD_CLASSES);
  }

  public static FieldWithLabel createInput(String inputId) {
    Tag fieldTag = input().withType("text");
    return new FieldWithLabel(fieldTag, inputId);
  }

  public static FieldWithLabel createTextArea(String inputId) {
    Tag fieldTag = textarea().withType("text");
    return new FieldWithLabel(fieldTag, inputId);
  }

  public FieldWithLabel setId(String inputId) {
    fieldTag.withId(inputId).withName(inputId);
    return this;
  }

  public FieldWithLabel setLabelText(String labelText) {
    this.labelText = labelText;
    return this;
  }

  public FieldWithLabel setPlaceholderText(String placeholder) {
    fieldTag.withPlaceholder(placeholder);
    return this;
  }

  public FieldWithLabel setValue(String value) {
    this.fieldValue = value;
    return this;
  }

  public ContainerTag getContainer() {
    ContainerTag labelTag =
        label()
            .attr(Attr.FOR, this.id)
            .withClasses(FieldWithLabel.CORE_LABEL_CLASSES)
            .withText(this.labelText);
    return div(labelTag, fieldTag.withValue(this.fieldValue)).withClasses(Styles.MX_4, Styles.MB_6);
  }
}
