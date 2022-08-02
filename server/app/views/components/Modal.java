package views.components;

import static j2html.TagCreator.div;
import static views.BaseHtmlView.button;

import j2html.tags.ContainerTag;
import j2html.tags.specialized.ButtonTag;
import j2html.tags.specialized.DivTag;
import java.util.Optional;
import java.util.UUID;
import views.style.BaseStyles;
import views.style.ReferenceClasses;
import views.style.StyleUtils;
import views.style.Styles;

/** Utility class for rendering a modal box. */
public final class Modal {

  public static final String MODAL_OPENER_FOR_ATTRIBUTE = "data-modal-opener-for";

  private final String modalId;
  private final ContainerTag<?> content;
  private final String modalTitle;
  private final String triggerButtonText;
  private final Optional<ButtonTag> triggerButtonContent;
  private final String buttonStyles;
  private final Width width;
  private final boolean displayOnLoad;

  private Modal(ModalBuilder builder) {
    this.modalId = builder.modalId;
    this.content = builder.content;
    this.modalTitle = builder.modalTitle;
    this.triggerButtonText = builder.triggerButtonText;
    this.triggerButtonContent = builder.triggerButtonContent;
    this.buttonStyles = builder.buttonStyles;
    this.width = builder.width;
    this.displayOnLoad = builder.displayOnLoad;
  }

  public DivTag getContainerTag() {
    String modalStyles =
        StyleUtils.joinStyles(ReferenceClasses.MODAL, BaseStyles.MODAL, width.getStyle());
    if (displayOnLoad) {
      modalStyles = StyleUtils.joinStyles(modalStyles, ReferenceClasses.MODAL_DISPLAY_ON_LOAD);
    }
    return div().withId(modalId).withClasses(modalStyles).with(getModalHeader()).with(getContent());
  }

  public ButtonTag getButton() {
    String triggerButtonId = getTriggerButtonId();
    if (triggerButtonContent.isPresent()) {
      return triggerButtonContent.get().withId(triggerButtonId);
    } else {
      return button(triggerButtonId, triggerButtonText).withClasses(buttonStyles);
    }
  }

  public String getTriggerButtonId() {
    return modalId + "-button";
  }

  public String getModalId() {
    return modalId;
  }

  public boolean displayOnLoad() {
    return displayOnLoad;
  }

  private DivTag getContent() {
    return div(content).withClasses(BaseStyles.MODAL_CONTENT);
  }

  private DivTag getModalHeader() {
    return div()
        .withClasses(BaseStyles.MODAL_HEADER)
        .with(div(modalTitle).withClasses(Styles.TEXT_LG))
        .with(div().withClasses(Styles.FLEX_GROW))
        .with(div("x").withId(modalId + "-close").withClasses(BaseStyles.MODAL_CLOSE_BUTTON));
  }

  public static String randomModalId() {
    // We prepend a "a-" since element IDs must start with an alphabetic character, whereas UUIDs
    // can start with a numeric character.
    return "a-" + UUID.randomUUID().toString();
  }

  public static ModalBuilder builder(String modalId, ContainerTag<?> content) {
    return new ModalBuilder(modalId, content);
  }

  public static final class ModalBuilder {

    private String modalId;
    private ContainerTag<?> content;
    private String buttonStyles = BaseStyles.MODAL_BUTTON;

    // Optional fields. See #setOptionalFields().
    private String modalTitle;
    private String triggerButtonText;

    private Optional<ButtonTag> triggerButtonContent = Optional.empty();
    private Width width = Width.DEFAULT;
    private boolean displayOnLoad = false;

    public ModalBuilder(String modalId, ContainerTag<?> content) {
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

    public ModalBuilder setDisplayOnLoad(boolean value) {
      this.displayOnLoad = value;
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
