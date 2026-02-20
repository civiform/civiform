package views.components;

import static j2html.TagCreator.button;
import static j2html.TagCreator.div;
import static j2html.TagCreator.li;
import static j2html.TagCreator.p;
import static j2html.TagCreator.span;
import static j2html.TagCreator.ul;

import j2html.tags.specialized.DivTag;
import java.util.List;
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
 * <p>The modals use USWDS (U.S. Web Design System) styling and match the structure in
 * ModalFragment.html's sessionInactivityTimeoutModal and sessionLengthTimeoutModal fragments.
 */
public final class SessionTimeoutModals {

  public static List<DivTag> render(Messages messages, String csrfToken) {
    // Create hidden elements for localized messages
    DivTag localizedMessages =
        div()
            .withId("session-timeout-messages")
            .withClass("hidden")
            .with(
                span()
                    .withId("session-extended-success-text")
                    .attr("role", "alert")
                    .withText(messages.at(MessageKey.SESSION_EXTENDED_SUCCESS.getKeyName())),
                span()
                    .withId("session-extended-error-text")
                    .attr("role", "alert")
                    .withText(messages.at(MessageKey.SESSION_EXTENDED_ERROR.getKeyName())));

    // Inactivity warning modal
    DivTag inactivityModal = createInactivityWarningModal(messages, csrfToken);

    // Session length warning modal (always use logged-in messages since this is only used for
    // admin/TI)
    DivTag sessionLengthModal = createSessionLengthWarningModal(messages);

    return List.of(inactivityModal, sessionLengthModal, localizedMessages);
  }

  /**
   * The inactivity modal needs a custom primary button with HTMX attributes for extending the
   * session. The body includes both the description and a custom footer, passed to makeUswdsModal
   * with hasFooter=false.
   */
  private static DivTag createInactivityWarningModal(Messages messages, String csrfToken) {
    DivTag body =
        div()
            .with(
                p(messages.at(MessageKey.SESSION_INACTIVITY_WARNING_MESSAGE.getKeyName())),
                div()
                    .withClass("usa-modal__footer")
                    .with(
                        ul().withClass("usa-button-group")
                            .with(
                                li().withClass("usa-button-group__item")
                                    .with(
                                        button(
                                                messages.at(
                                                    MessageKey.SESSION_INACTIVITY_EXTEND_BUTTON
                                                        .getKeyName()))
                                            .withType("button")
                                            .withId("extend-session-button")
                                            .withClass("usa-button")
                                            .attr("data-close-modal")
                                            .attr("data-modal-primary", "")
                                            .attr("data-modal-type", "session-inactivity-warning")
                                            .attr("hx-target", "this")
                                            .attr("hx-swap", "none")
                                            .attr("hx-post", "/extend-session")
                                            .attr(
                                                "hx-vals",
                                                "{\"csrfToken\": \"" + csrfToken + "\"}")),
                                li().withClass("usa-button-group__item")
                                    .with(
                                        button(messages.at(MessageKey.BUTTON_CANCEL.getKeyName()))
                                            .withType("button")
                                            .withClass(
                                                "usa-button usa-button--unstyled"
                                                    + " padding-105 text-center")
                                            .attr("data-close-modal")
                                            .attr("data-modal-secondary", "")
                                            .attr(
                                                "data-modal-type",
                                                "session-inactivity-warning")))));

    return ViewUtils.makeUswdsModal(
        body,
        "session-inactivity-warning",
        messages.at(MessageKey.SESSION_WARNING_TITLE.getKeyName()),
        "",
        /* hasFooter= */ false,
        "",
        messages.at(MessageKey.BUTTON_CLOSE.getKeyName()));
  }

  private static DivTag createSessionLengthWarningModal(Messages messages) {
    DivTag body =
        div()
            .with(p(messages.at(MessageKey.SESSION_LENGTH_WARNING_MESSAGE_LOGGED_IN.getKeyName())));

    return ViewUtils.makeUswdsModal(
        body,
        "session-length-warning",
        messages.at(MessageKey.SESSION_WARNING_TITLE.getKeyName()),
        messages.at(MessageKey.SESSION_LOGIN_BUTTON_LOGGED_IN.getKeyName()),
        /* hasFooter= */ true,
        messages.at(MessageKey.BUTTON_CANCEL.getKeyName()),
        messages.at(MessageKey.BUTTON_CLOSE.getKeyName()));
  }
}
