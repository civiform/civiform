package views.applicant;

import static j2html.TagCreator.div;

import controllers.routes;
import j2html.tags.specialized.ButtonTag;
import j2html.tags.specialized.DivTag;
import java.util.Optional;
import java.util.UUID;
import play.i18n.Messages;
import services.MessageKey;
import views.BaseHtmlView;
import views.components.ButtonStyles;
import views.components.Modal;
import views.components.Modal.Width;
import views.style.StyleUtils;

/** Helper methods for rendering authentication related upsells. */
public final class AuthenticateUpsellCreator extends BaseHtmlView {

  /** Creates a Modal that prompts the guest user to log in or create an account. */
  public static Modal.Builder createLoginPromptModal(
      Messages messages, String postLoginRedirectTo, String description, MessageKey bypassMessage) {
    String modalTitle = messages.at(MessageKey.TITLE_CREATE_AN_ACCOUNT.getKeyName());

    // Include randomization in the IDs for this modal since some pages may have
    // multiple modals.
    String uuid = UUID.randomUUID().toString();
    DivTag modalContent =
        div()
            .with(
                div().withText(description).withClass("mb-8"),
                div()
                    .with(
                        redirectButton(
                                "bypass-login-prompt-button-" + uuid,
                                messages.at(bypassMessage.getKeyName()),
                                postLoginRedirectTo)
                            .withClasses(ButtonStyles.OUTLINED_TRANSPARENT),
                        createLoginButton("modal-sign-in-" + uuid, messages, postLoginRedirectTo),
                        createNewAccountButton("modal-sign-up-" + uuid, messages))
                    .withClasses(
                        "flex",
                        "flex-col",
                        "gap-4",
                        "justify-end",
                        StyleUtils.responsiveMedium("flex-row")));

    return Modal.builder()
        .setModalId(Modal.randomModalId())
        .setLocation(Modal.Location.APPLICANT_FACING)
        .setContent(modalContent)
        .setModalTitle(modalTitle)
        .setMessages(messages)
        .setWidth(Width.HALF);
  }

  /** Creates a button that redirects to the account creation page. */
  static ButtonTag createNewAccountButton(String buttonId, Messages messages) {
    return redirectButton(
            buttonId,
            messages.at(MessageKey.BUTTON_CREATE_ACCOUNT.getKeyName()),
            routes.LoginController.register().url())
        .withClasses(ButtonStyles.SOLID_BLUE);
  }

  /** Creates a button that redirects to the login page. */
  static ButtonTag createLoginButton(
      String buttonId, Messages messages, String postLoginRedirectTo) {
    return redirectButton(
            buttonId,
            messages.at(MessageKey.BUTTON_LOGIN.getKeyName()),
            routes.LoginController.applicantLogin(Optional.of(postLoginRedirectTo)).url())
        .withClasses(ButtonStyles.OUTLINED_TRANSPARENT);
  }
}
