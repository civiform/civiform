package views.components;

import static j2html.TagCreator.div;
import static views.BaseHtmlView.button;

import j2html.TagCreator;
import j2html.tags.Tag;
import java.util.Optional;
import views.style.BaseStyles;
import views.style.ReferenceClasses;
import views.style.Styles;

public class Modal {

  private String modalId;
  private Tag content;
  private String modalTitle;
  private String triggerButtonText;
  private Tag triggerButtonContent;
  private String buttonStyles;

  private Modal(ModalBuilder builder) {
    this.modalId = builder.modalId;
    this.content = builder.content;
    this.modalTitle = builder.modalTitle;
    this.triggerButtonText = builder.triggerButtonText;
    this.triggerButtonContent = builder.triggerButtonContent;
    this.buttonStyles = builder.buttonStyles;
  }

  public Tag getContainerTag() {
    return div()
        .withId(modalId)
        .withClasses(ReferenceClasses.MODAL, BaseStyles.MODAL)
        .with(getModalHeader())
        .with(getContent());
  }

  public Tag getButton() {
    String triggerButtonId = modalId + "-button";
    if (triggerButtonContent != null) {
      return TagCreator.button()
          .withType("button")
          .withClasses(buttonStyles)
          .withId(triggerButtonId)
          .with(triggerButtonContent);
    } else {
      return button(triggerButtonId, triggerButtonText).withClasses(buttonStyles);
    }
  }

  private Tag getContent() {
    return div(content).withClasses(BaseStyles.MODAL_CONTENT);
  }

  private Tag getModalHeader() {
    return div()
        .withClasses(BaseStyles.MODAL_HEADER)
        .with(div(modalTitle).withClasses(Styles.TEXT_LG))
        .with(div("x").withId(modalId + "-close").withClasses(BaseStyles.MODAL_CLOSE_BUTTON));
  }

  public static ModalBuilder builder(String modalId, Tag content) {
    return new ModalBuilder(modalId, content);
  }

  public static class ModalBuilder {

    private String modalId;
    private Tag content;
    private String buttonStyles = BaseStyles.MODAL_BUTTON;

    // Optional fields. See #setOptionalFields().
    private String modalTitle;
    private String triggerButtonText;

    private Tag triggerButtonContent = null;

    public ModalBuilder(String modalId, Tag content) {
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

    public ModalBuilder setTriggerButtonContent(Tag triggerButtonContent) {
      this.triggerButtonContent = triggerButtonContent;
      return this;
    }

    public ModalBuilder setTriggerButtonStyles(String buttonStyles) {
      this.buttonStyles = buttonStyles;
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
}
