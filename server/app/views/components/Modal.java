package views.components;

import static j2html.TagCreator.div;
import static views.BaseHtmlView.button;

import com.google.auto.value.AutoValue;
import j2html.tags.ContainerTag;
import j2html.tags.specialized.ButtonTag;
import j2html.tags.specialized.DivTag;
import java.util.Optional;
import java.util.UUID;
import views.style.BaseStyles;
import views.style.ReferenceClasses;
import views.style.StyleUtils;

/** Utility class for rendering a modal box. */
@AutoValue
public abstract class Modal {

  public abstract String modalId();

  public abstract ContainerTag<?> content();

  public abstract String modalTitle();

  public abstract Optional<ButtonTag> triggerButtonContent();

  public abstract Width width();

  public abstract boolean displayOnLoad();

  private static Modal.Builder builder() {
    // Set some defaults before the user sets their own values.
    return new AutoValue_Modal.Builder().setWidth(Width.DEFAULT).setDisplayOnLoad(false);
  }

  public static Builder builder(String modalId, ContainerTag<?> content) {
    return builder().setModalId(modalId).setContent(content);
  }

  @AutoValue.Builder
  public abstract static class Builder {

    public abstract Builder setModalId(String modalId);

    public abstract String modalTitle();

    abstract String modalId();

    public abstract Builder setContent(ContainerTag<?> content);

    public abstract Builder setModalTitle(String modalTitle);

    public abstract Builder setTriggerButtonContent(ButtonTag triggerButtonContent);

    public abstract Builder setWidth(Width width);

    public abstract Builder setDisplayOnLoad(boolean value);

    abstract Modal autoBuild();

    // In the client-facing build() method, we set the modal title to the modal ID if it is not
    // already set.
    public Modal build() {
      setModalTitle(modalTitle() != null ? modalTitle() : modalId());
      return autoBuild();
    }
  }

  public enum Width {
    DEFAULT("w-auto"),
    HALF("w-1/2"),
    THIRD("w-1/3"),
    FOURTH("w-1/4"),
    THREE_FOURTHS("w-9/12");

    private final String width;

    Width(String width) {
      this.width = width;
    }

    public String getStyle() {
      return this.width;
    }
  }

  public DivTag getContainerTag() {
    String modalStyles =
        StyleUtils.joinStyles(ReferenceClasses.MODAL, BaseStyles.MODAL, width().getStyle());
    if (displayOnLoad()) {
      modalStyles = StyleUtils.joinStyles(modalStyles, ReferenceClasses.MODAL_DISPLAY_ON_LOAD);
    }
    return div()
        .withId(modalId())
        .withClasses(modalStyles)
        .with(getModalHeader())
        .with(getContent());
  }

  public ButtonTag getButton() {
    String triggerButtonId = getTriggerButtonId();
    if (triggerButtonContent().isPresent()) {
      return triggerButtonContent().get().withId(triggerButtonId);
    } else {
      return button(triggerButtonId, modalTitle());
    }
  }

  public String getTriggerButtonId() {
    return modalId() + "-button";
  }

  private DivTag getContent() {
    return div(content()).withClasses(BaseStyles.MODAL_CONTENT);
  }

  private DivTag getModalHeader() {
    return div()
        .withClasses(BaseStyles.MODAL_HEADER)
        .with(div(modalTitle()).withClasses("text-lg"))
        .with(div().withClasses("flex-grow"))
        .with(
            Icons.svg(Icons.CLOSE)
                .withClasses(ReferenceClasses.MODAL_CLOSE, BaseStyles.MODAL_CLOSE_BUTTON));
  }

  public static String randomModalId() {
    // We prepend a "uuid-" since element IDs must start with an alphabetic character, whereas UUIDs
    // can start with a numeric character.
    return "uuid-" + UUID.randomUUID();
  }
}
