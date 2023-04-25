package views.applicant;

import static j2html.TagCreator.div;

import controllers.routes;
import j2html.tags.specialized.ButtonTag;
import j2html.tags.specialized.DivTag;
import java.util.Optional;
import play.i18n.Messages;
import services.MessageKey;
import views.BaseHtmlView;
import views.components.ButtonStyles;
import views.components.Modal;
import views.components.Modal.Width;
import views.style.StyleUtils;

public class AuthenticateUpsellViews extends BaseHtmlView {

  /** Creates a Modal that prompts the guest user to log in or create an account. */
  public static Modal createLoginPromptModal(
      Messages messages, String postLoginRedirectTo, MessageKey bypassMessage) {
    String modalTitle = messages.at(MessageKey.TITLE_CREATE_AN_ACCOUNT.getKeyName());
    String modalDescription = messages.at(MessageKey.LOGIN_MODAL_PROMPT.getKeyName());

    DivTag modalContent =
        div()
            .with(
                div().withText(modalDescription).withClass("mb-8"),
                div()
                    .with(
                        redirectButton(
                                "bypass-login-prompt-button",
                                messages.at(bypassMessage.getKeyName()),
                                postLoginRedirectTo)
                            .withClasses(ButtonStyles.OUTLINED_TRANSPARENT),
                        createLoginButton("modal-sign-in", messages, postLoginRedirectTo),
                        createNewAccountButton("modal-sign-up", messages))
                    .withClasses(
                        "flex",
                        "flex-col",
                        "gap-4",
                        "justify-end",
                        StyleUtils.responsiveMedium("flex-row")));

    return Modal.builder()
        .setModalId(Modal.randomModalId())
        .setContent(modalContent)
        .setModalTitle(modalTitle)
        .setWidth(Width.HALF)
        .build();
  }

  protected static ButtonTag createNewAccountButton(String buttonId, Messages messages) {
    return redirectButton(
            buttonId,
            messages.at(MessageKey.BUTTON_CREATE_ACCOUNT.getKeyName()),
            routes.LoginController.register().url())
        .withClasses(ButtonStyles.SOLID_BLUE);
  }

  protected static ButtonTag createLoginButton(
      String buttonId, Messages messages, String postLoginRedirectTo) {
    return redirectButton(
            buttonId,
            messages.at(MessageKey.BUTTON_LOGIN.getKeyName()),
            routes.LoginController.applicantLogin(Optional.of(postLoginRedirectTo)).url())
        .withClasses(ButtonStyles.OUTLINED_TRANSPARENT);
  }
}
