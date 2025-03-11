package views.components;

import static j2html.TagCreator.div;
import static j2html.TagCreator.form;
import static j2html.TagCreator.input;
import static j2html.TagCreator.p;
import static j2html.TagCreator.span;

import j2html.tags.specialized.DivTag;
import play.i18n.Messages;
import services.MessageKey;
import views.ViewUtils;

public final class SessionTimeoutModals {

  /*
   * Creates the session timeout modals for inactivity and session length warnings.
   */
  public static DivTag render(Messages messages, String csrfToken) {
    // Create hidden elements for localized messages
    DivTag localizedMessages =
        div()
            .withId("session-timeout-messages")
            .withClass("hidden")
            .with(
                span()
                    .withId("session-extended-success-text")
                    .withText(messages.at(MessageKey.SESSION_EXTENDED_SUCCESS.getKeyName())),
                span()
                    .withId("session-extended-error-text")
                    .withText(messages.at(MessageKey.SESSION_EXTENDED_ERROR.getKeyName())));

    // Inactivity warning modal
    DivTag inactivityModal = createInactivityWarningModal(messages, csrfToken);

    // Session length warning modal
    DivTag sessionLengthModal = createSessionLengthWarningModal(messages);

    // Container for both modals
    DivTag sessionTimeoutModals =
        div().withId("session-timeout-modals").with(inactivityModal, sessionLengthModal);

    return div().withId("session-timeout-container").with(sessionTimeoutModals, localizedMessages);
  }

  private static DivTag createInactivityWarningModal(Messages messages, String csrfToken) {
    // Create the body content for the inactivity warning modal
    DivTag modalBody =
        div()
            .withId("session-inactivity-description")
            .with(
                p(messages.at(MessageKey.SESSION_INACTIVITY_WARNING_MESSAGE.getKeyName())),
                form()
                    .withId("extend-session-form")
                    .attr("hx-post", "/extend-session")
                    .attr("hx-target", "this")
                    .attr("hx-swap", "none")
                    .with(input().isHidden().withName("csrfToken").withValue(csrfToken)));

    // Create the inactivity warning modal
    return ViewUtils.makeUSWDSModal(
            modalBody,
            "session-inactivity-warning",
            messages.at(MessageKey.SESSION_INACTIVITY_WARNING_TITLE.getKeyName()),
            messages.at(MessageKey.SESSION_EXTEND_BUTTON.getKeyName()),
            true,
            messages.at(MessageKey.SESSION_EXTEND_BUTTON.getKeyName()),
            messages.at(MessageKey.BUTTON_CANCEL.getKeyName()))
        .withClasses("hidden");
  }

  private static DivTag createSessionLengthWarningModal(Messages messages) {
    // Create the body content for the session length warning modal
    DivTag modalBody =
        div()
            .withId("session-length-description")
            .with(p(messages.at(MessageKey.SESSION_LENGTH_WARNING_MESSAGE.getKeyName())));

    // Create the session length warning modal
    return ViewUtils.makeUSWDSModal(
            modalBody,
            "session-length-warning",
            messages.at(MessageKey.SESSION_LENGTH_WARNING_TITLE.getKeyName()),
            messages.at(MessageKey.BUTTON_LOGOUT.getKeyName()),
            true,
            messages.at(MessageKey.BUTTON_LOGOUT.getKeyName()),
            messages.at(MessageKey.BUTTON_CANCEL.getKeyName()))
        .withClasses("hidden");
  }
}
