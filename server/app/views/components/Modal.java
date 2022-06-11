package views.components;

import static j2html.TagCreator.div;
import static views.BaseHtmlView.button;

import j2html.tags.Tag;
import j2html.tags.specialized.ButtonTag;
import java.util.Optional;
import views.style.BaseStyles;
import views.style.ReferenceClasses;
import views.style.Styles;

/** Utility class for rendering a modal box. */
public class Modal {

  private String modalId;
  private Tag<?> content;
  private String modalTitle;
  private String triggerButtonText;
  private Optional<ButtonTag> triggerButtonContent;
  private String buttonStyles;
  private Width width;

  private Modal(ModalBuilder builder) {
    this.modalId = builder.modalId;
    this.content = builder.content;
    this.modalTitle = builder.modalTitle;
    this.triggerButtonText = builder.triggerButtonText;
    this.triggerButtonContent = builder.triggerButtonContent;
    this.buttonStyles = builder.buttonStyles;
    this.width = builder.width;
  }

  public Tag<?> getContainerTag() {
    return div()
        .withId(modalId)
        .withClasses(ReferenceClasses.MODAL, BaseStyles.MODAL, width.getStyle())
        .with(getModalHeader())
        .with(getContent());
  }

  public ButtonTag getButton() {
    String triggerButtonId = modalId + "-button";
    if (triggerButtonContent.isPresent()) {
      return triggerButtonContent.get().withClasses(buttonStyles).withId(triggerButtonId);
    } else {
      return button(triggerButtonId, triggerButtonText).withClasses(buttonStyles);
    }
  }

  private Tag<?> getContent() {
    return div(content).withClasses(BaseStyles.MODAL_CONTENT);
  }

  private Tag<?> getModalHeader() {
    return div()
        .withClasses(BaseStyles.MODAL_HEADER)
        .with(div(modalTitle).withClasses(Styles.TEXT_LG))
        .with(div().withClasses(Styles.FLEX_GROW))
        .with(div("x").withId(modalId + "-close").withClasses(BaseStyles.MODAL_CLOSE_BUTTON));
  }

  public static ModalBuilder builder(String modalId, Tag<?> content) {
    return new ModalBuilder(modalId, content);
  }

  public static class ModalBuilder {

    private String modalId;
    private Tag<?> content;
    private String buttonStyles = BaseStyles.MODAL_BUTTON;

    // Optional fields. See #setOptionalFields().
    private String modalTitle;
    private String triggerButtonText;

    private Optional<ButtonTag> triggerButtonContent = Optional.empty();
    private Width width = Width.DEFAULT;

    public ModalBuilder(String modalId, Tag<?> content) {
      this.modalId = modalId;
      this.content = content;
    }

    public ModalBuilder setModalTitle(String modalTitle) {
      this.modalTitle = modalTitle;
      return this;
    }

    public ModalBuilder setTriggerButtonText(String triggerButtonText) {
      this.triggerButtonText = triggerButtonText;
      return this;
    }

    public ModalBuilder setTriggerButtonContent(ButtonTag triggerButtonContent) {
      this.triggerButtonContent = Optional.ofNullable(triggerButtonContent);
      return this;
    }

    public ModalBuilder setTriggerButtonStyles(String buttonStyles) {
      this.buttonStyles = buttonStyles;
      return this;
    }

    public ModalBuilder setWidth(Width width) {
      this.width = width;
      return this;
    }

    public Modal build() {
      setOptionalFields();
      return new Modal(this);
    }

    private void setOptionalFields() {
      modalTitle = Optional.ofNullable(modalTitle).orElse(modalId);
      triggerButtonText = Optional.ofNullable(triggerButtonText).orElse(modalTitle);
    }
  }

  public enum Width {
    DEFAULT(Styles.W_AUTO),
    HALF(Styles.W_1_2),
    THIRD(Styles.W_1_3),
    FOURTH(Styles.W_1_4);

    private final String width;

    Width(String width) {
      this.width = width;
    }

    public String getStyle() {
      return this.width;
    }
  }
}
