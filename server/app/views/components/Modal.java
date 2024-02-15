package views.components;

import static j2html.TagCreator.div;
import static views.BaseHtmlView.button;
import static views.BaseHtmlView.iconOnlyButton;

import com.google.auto.value.AutoValue;
import j2html.tags.ContainerTag;
import j2html.tags.specialized.ButtonTag;
import j2html.tags.specialized.DivTag;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import play.i18n.Messages;
import services.MessageKey;
import views.style.BaseStyles;
import views.style.ReferenceClasses;
import views.style.StyleUtils;

/**
 * Utility class for rendering a modal box.
 *
 * @deprecated Use {@link views.ViewUtils#makeUSWDSModal} instead. We're migrating existing modals
 *     to use the USWDS modal component, so new modals should use the USWDS modal from the
 *     beginning. See https://github.com/civiform/civiform/issues/6264.
 */
@AutoValue
@Deprecated
public abstract class Modal {

  public abstract String modalId();

  /**
   * Informs where this modal is seen.
   *
   * <p>If the location is {@code Location.APPLICANT_FACING}, then {@code messages()} is required to
   * be present so that the modal is correctly localized.
   */
  public abstract Location location();

  /** Required to be set if this modal is applicant-facing; optional otherwise. */
  public abstract Optional<Messages> messages();

  public abstract ContainerTag<?> content();

  public abstract String modalTitle();

  public abstract Optional<ButtonTag> triggerButtonContent();

  public abstract Width width();

  public abstract boolean displayOnLoad();

  public abstract RepeatOpenBehavior repeatOpenBehavior();

  public static Modal.RequiredModalId builder() {
    // Set some defaults before the user sets their own values.
    return new AutoValue_Modal.Builder()
        .setWidth(Width.DEFAULT)
        .setDisplayOnLoad(false)
        .setRepeatOpenBehavior(RepeatOpenBehavior.alwaysShow());
  }

  public interface RequiredModalId {
    RequiredLocation setModalId(String modalId);
  }

  public interface RequiredLocation {
    RequiredContent setLocation(Location location);
  }

  public interface RequiredContent {
    RequiredTitle setContent(ContainerTag<?> content);
  }

  public interface RequiredTitle {
    Builder setModalTitle(String modalTitle);
  }

  @AutoValue.Builder
  public abstract static class Builder
      implements RequiredModalId, RequiredLocation, RequiredTitle, RequiredContent {
    public abstract Builder setMessages(Messages messages);

    public abstract Builder setTriggerButtonContent(ButtonTag triggerButtonContent);

    public abstract Builder setWidth(Width width);

    public abstract Builder setDisplayOnLoad(boolean value);

    public abstract Builder setRepeatOpenBehavior(RepeatOpenBehavior repeatOpenBehavior);

    // Effectively private (here and below). Java does not allow private abstract methods.
    abstract String modalTitle();

    abstract String modalId();

    abstract Location location();

    abstract Optional<Messages> messages();

    // This is the build method that AutoValue will generate an implementation for.
    abstract Modal autoBuild();

    // In the client-facing build() method, we set the modal title to the modal ID if it is not
    // already set.
    public Modal build() {
      if (modalTitle() == null) {
        setModalTitle(modalId());
      }
      if (location() == Location.APPLICANT_FACING && messages().isEmpty()) {
        throw new IllegalArgumentException("Messages must be set if the modal is applicant-facing");
      }
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

  /** Describes where this modal will be seen. */
  public enum Location {
    /** This modal is seen by applicants. */
    APPLICANT_FACING,
    /** This modal is only seen by admins, never applicants. */
    ADMIN_FACING,
    /** This modal is only seen when debug mode is enabled. */
    DEBUG,
  }

  /**
   * Governs the behavior of the Modal if it were to be opened more than once. By default, shows the
   * Modal every time it is triggered. By setting showOnlyOnce to true, the Modal will only be shown
   * once.
   *
   * <p>In addition, setting the bypassUrl will allow the user to be redirected somewhere else
   * (presumably where they would have gone after dismissing the Modal) if the modal were opened
   * again when showOnlyOnce is true.
   */
  @AutoValue
  public abstract static class RepeatOpenBehavior {
    // Whether to limit displaying the Modal to once. Defaults to false.
    public abstract boolean showOnlyOnce();

    // Where to redirect the user if they try to open the Modal again when showOnlyOnce is true.
    // If empty, nothing will happen upon trying to open the Modal again.
    public abstract Optional<String> bypassUrl();

    // The group for this Modal. If two Modals have the same group, then opening one
    // of them will prevent any Modal in the group from being shown again.
    public abstract Group group();

    public enum Group {
      NONE,
      PROGRAMS_INDEX_LOGIN_PROMPT,
      PROGRAM_SLUG_LOGIN_PROMPT;
    }

    public static RepeatOpenBehavior alwaysShow() {
      return new AutoValue_Modal_RepeatOpenBehavior(false, Optional.empty(), Group.NONE);
    }

    public static RepeatOpenBehavior showOnlyOnce(Group group) {
      return new AutoValue_Modal_RepeatOpenBehavior(true, Optional.empty(), group);
    }

    public static RepeatOpenBehavior showOnlyOnce(Group group, String bypassUrl) {
      return new AutoValue_Modal_RepeatOpenBehavior(true, Optional.of(bypassUrl), group);
    }
  }

  public DivTag getContainerTag() {
    DivTag divTag =
        div()
            .withId(modalId())
            .attr("aria-labelledby", getModalTitleId())
            .attr("tabindex", 0)
            .with(getModalHeader())
            .with(getContent())
            // https://designsystem.digital.gov/components/modal/ recommends putting the close
            // button at the end of the modal so screen readers don't put focus on the close
            // button first.
            .with(getCloseButton());

    String modalStyles =
        StyleUtils.joinStyles(
            ReferenceClasses.MODAL, BaseStyles.MODAL, "w-11/12", "lg:" + width().getStyle());
    if (displayOnLoad()) {
      modalStyles = StyleUtils.joinStyles(modalStyles, ReferenceClasses.MODAL_DISPLAY_ON_LOAD);
    }
    divTag.withClasses(modalStyles);
    if (repeatOpenBehavior().showOnlyOnce()) {
      divTag.attr(
          "only-show-once-group", repeatOpenBehavior().group().name().toLowerCase(Locale.ROOT));
      if (repeatOpenBehavior().bypassUrl().isPresent()) {
        divTag.attr("bypass-url", repeatOpenBehavior().bypassUrl().get());
      }
    }

    return divTag;
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

  private String getModalTitleId() {
    return modalId() + "-title";
  }

  private DivTag getContent() {
    return div(content()).withClasses(BaseStyles.MODAL_CONTENT);
  }

  private DivTag getModalHeader() {
    return div()
        .withId(getModalTitleId())
        .withClasses(BaseStyles.MODAL_HEADER)
        .with(div(modalTitle()).withClasses(BaseStyles.MODAL_TITLE))
        // Leave enough space for the close button so the close button and title
        // don't overlap.
        .with(div().withClasses("w-12"));
  }

  private ButtonTag getCloseButton() {
    String closeButtonLabel;
    if (messages().isPresent()) {
      closeButtonLabel = messages().get().at(MessageKey.BUTTON_CLOSE.getKeyName());
    } else {
      closeButtonLabel = "Close";
    }
    return iconOnlyButton(closeButtonLabel)
        .withClasses(
            ReferenceClasses.MODAL_CLOSE,
            ButtonStyles.CLEAR_WITH_ICON,
            "top-0",
            "right-0",
            "absolute",
            "my-6",
            "mx-4")
        .with(Icons.svg(Icons.CLOSE).withClasses("w-6", "h-6", "cursor-pointer"));
  }

  public static String randomModalId() {
    // We prepend a "uuid-" since element IDs must start with an alphabetic character, whereas UUIDs
    // can start with a numeric character.
    return "uuid-" + UUID.randomUUID();
  }
}
