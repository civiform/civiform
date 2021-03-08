package views.components;

import static j2html.TagCreator.div;
import static j2html.TagCreator.input;
import static j2html.TagCreator.label;
import static j2html.TagCreator.textarea;

import j2html.attributes.Attr;
import j2html.tags.ContainerTag;
import j2html.tags.Tag;
import views.Styles;

public class FieldWithLabel {
  public static String[] CORE_FIELD_CLASSES = {
    Styles.BLOCK,
    Styles.W_FULL,
    Styles.BG_GRAY_50,
    Styles.BORDER,
    Styles.BORDER_GRAY_500,
    Styles.P_2
  };
  public static String[] CORE_LABEL_CLASSES = {
    Styles._MX_1,
    Styles.BLOCK,
    Styles.UPPERCASE,
    Styles.TRACKING_WIDE,
    Styles.TEXT_GRAY_600,
    Styles.TEXT_XS,
    Styles.FONT_BOLD,
    Styles.MB_2
  };

  protected Tag fieldTag;
  protected String fieldValue = "";
  protected ContainerTag labelTag;
  protected String labelText = "";

  public FieldWithLabel(Tag fieldTag, String inputId) {
    this.fieldTag =
        fieldTag.withId(inputId).withName(inputId).withClasses(FieldWithLabel.CORE_FIELD_CLASSES);
    this.labelTag = label().attr(Attr.FOR, inputId).withClasses(FieldWithLabel.CORE_LABEL_CLASSES);
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
    labelTag.attr(Attr.FOR, inputId);
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
    return div(labelTag.withText(this.labelText), fieldTag.withValue(this.fieldValue))
        .withClasses(Styles.MX_4, Styles.MB_6);
  }
}
