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
  protected ContainerTag labelTag;
  protected String labelText = "";

  protected ContainerTag renderedElement;
  protected boolean isRendered = false;

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
    if (this.isRendered) {
      return this;
    }
    fieldTag.withId(inputId).withName(inputId);
    labelTag.attr(Attr.FOR, inputId);
    return this;
  }

  public FieldWithLabel setLabelText(String labelText) {
    if (this.isRendered) {
      return this;
    }
    this.labelText = labelText;
    return this;
  }

  public FieldWithLabel setPlaceholderText(String placeholder) {
    if (this.isRendered) {
      return this;
    }
    fieldTag.withPlaceholder(placeholder);
    return this;
  }

  public FieldWithLabel setValue(String value) {
    if (this.isRendered) {
      return this;
    }
    this.fieldValue = value;
    return this;
  }

  public ContainerTag getContainer() {
    if (!this.isRendered) {
      this.renderedElement =
          div(labelTag.withText(this.labelText), fieldTag.withValue(this.fieldValue))
              .withClasses(Styles.MX_4, Styles.MB_6);
      this.isRendered = true;
    }
    return renderedElement;
  }
}
