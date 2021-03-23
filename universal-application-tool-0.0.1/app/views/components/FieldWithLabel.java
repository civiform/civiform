package views.components;

import static j2html.TagCreator.div;
import static j2html.TagCreator.label;
import static j2html.TagCreator.textarea;

import com.google.common.base.Strings;
import j2html.TagCreator;
import j2html.attributes.Attr;
import j2html.tags.ContainerTag;
import j2html.tags.Tag;
import views.style.BaseStyles;
import views.style.StyleUtils;
import views.style.Styles;

public class FieldWithLabel {
  private static final String[] CORE_FIELD_CLASSES = {
    BaseStyles.FIELD_BACKGROUND_COLOR,
    BaseStyles.FIELD_BORDER_COLOR,
    Styles.BLOCK,
    Styles.BORDER,
    Styles.P_2,
    Styles.W_FULL
  };

  private static final String[] CORE_CHECKBOX_FIELD_CLASSES = {
    Styles.H_4, Styles.W_4, Styles.MR_3, Styles.MB_2
  };

  private static final String[] CORE_CHECKBOX_LABEL_CLASSES = {
    Styles.TEXT_GRAY_600, Styles.ALIGN_TEXT_TOP, Styles.FONT_BOLD, Styles.TEXT_XS, Styles.UPPERCASE
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

  private static final String[] FLOATED_FIELD_CLASSES = {
    Styles.BG_TRANSPARENT,
    Styles.BLOCK,
    Styles.PX_1,
    Styles.PY_2,
    Styles.W_FULL,
    Styles.TEXT_BASE,
    StyleUtils.focus(Styles.OUTLINE_NONE)
  };

  private static final String[] FLOATED_LABEL_CLASSES = {
    Styles.ABSOLUTE,
    Styles.POINTER_EVENTS_NONE,
    Styles.TEXT_GRAY_600,
    Styles.TOP_0,
    Styles.LEFT_0,
    Styles.TEXT_BASE,
    Styles.PX_1,
    Styles.PY_2,
    "-z-1 origin-0",
    Styles.DURATION_300
  };

  protected Tag fieldTag;
  protected String fieldName = "";
  protected String fieldType = "text";
  protected String fieldValue = "";
  protected String formId = "";
  protected String id = "";
  protected String labelText = "";
  protected String placeholderText = "";
  protected boolean checked = false;
  protected boolean floatLabel = false;

  public FieldWithLabel(Tag fieldTag) {
    this.fieldTag = fieldTag.withClasses(FieldWithLabel.CORE_FIELD_CLASSES);
  }

  public static FieldWithLabel checkbox() {
    Tag fieldTag = TagCreator.input();
    return new FieldWithLabel(fieldTag).setFieldType("checkbox");
  }

  public static FieldWithLabel input() {
    Tag fieldTag = TagCreator.input();
    return new FieldWithLabel(fieldTag).setFieldType("text");
  }

  public static FieldWithLabel number() {
    Tag fieldTag = TagCreator.input();
    return new FieldWithLabel(fieldTag).setFieldType("number");
  }

  public static FieldWithLabel textArea() {
    Tag fieldTag = textarea();
    return new FieldWithLabel(fieldTag).setFieldType("text");
  }

  public FieldWithLabel setChecked(boolean checked) {
    this.checked = checked;
    return this;
  }

  public FieldWithLabel setFieldName(String fieldName) {
    this.fieldName = fieldName;
    return this;
  }

  public FieldWithLabel setFieldType(String fieldType) {
    this.fieldTag.withType(fieldType);
    this.fieldType = fieldType;
    return this;
  }

  public FieldWithLabel setFloatLabel(boolean floatLabel) {
    this.floatLabel = floatLabel;
    return this;
  }

  public FieldWithLabel setFormId(String formId) {
    this.formId = formId;
    return this;
  }

  public FieldWithLabel setId(String inputId) {
    this.id = inputId;
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

    fieldTag
        .withCondId(!Strings.isNullOrEmpty(this.id), this.id)
        .withName(this.fieldName)
        .withCondPlaceholder(!Strings.isNullOrEmpty(this.placeholderText), this.placeholderText)
        .condAttr(!Strings.isNullOrEmpty(this.formId), "form", formId);

    if (this.fieldType.equals("checkbox")) {
      return getCheckboxContainer();
    }

    ContainerTag labelTag =
        label()
            .condAttr(!Strings.isNullOrEmpty(this.id), Attr.FOR, this.id)
            .withClasses(FieldWithLabel.CORE_LABEL_CLASSES)
            .withText(this.labelText);

    if (this.floatLabel) {
      fieldTag.withPlaceholder(" ").withClasses(FieldWithLabel.FLOATED_FIELD_CLASSES);
      labelTag.withClasses(FieldWithLabel.FLOATED_LABEL_CLASSES);

      return div()
          .with(
              div(fieldTag, labelTag)
                  .withClasses(
                      "floated my-2 outline relative border-b-2 border-gray-600"
                          + " focus-within:border-blue-500"));
    }
    return div(labelTag, fieldTag).withClasses(Styles.MX_4, Styles.MB_6);
  }

  /** Swaps the order of the label and field, possibly adds, and adds different styles. */
  private ContainerTag getCheckboxContainer() {
    fieldTag.withClasses(CORE_CHECKBOX_FIELD_CLASSES);
    if (this.checked) {
      fieldTag.attr("checked");
    }

    ContainerTag labelTag =
        label()
            .withClasses(CORE_CHECKBOX_LABEL_CLASSES)
            .condAttr(!Strings.isNullOrEmpty(this.id), Attr.FOR, this.id)
            .withText(this.labelText);

    return div(fieldTag, labelTag).withClasses(Styles.MX_4, Styles.MB_1);
  }
}
