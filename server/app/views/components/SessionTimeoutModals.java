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

/**
 * Renders modal dialogs for session timeout notifications.
 *
 * <p>This class creates two types of modals:
 *
 * <ul>
 *   <li>Inactivity Warning Modal: Shown when user has been inactive for a certain period
 *   <li>Session Length Warning Modal: Shown when total session length is about to expire
 * </ul>
 *
 * <p>The modals use USWDS (U.S. Web Design System) styling and include:
 *
 * <ul>
 *   <li>Success/error messages for session extension attempts
 *   <li>CSRF protection for session extension requests
 *   <li>HTMX integration for AJAX form submission
 * </ul>
 */
public final class SessionTimeoutModals {

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
    return ViewUtils.makeUswdsModal(
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
    return ViewUtils.makeUswdsModal(
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
