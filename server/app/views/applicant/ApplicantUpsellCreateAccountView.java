package views.applicant;

import static com.google.common.base.Preconditions.checkNotNull;
import static j2html.TagCreator.div;
import static j2html.TagCreator.h1;
import static j2html.TagCreator.h2;

import com.google.common.base.Strings;
import com.google.inject.Inject;
import controllers.routes;
import j2html.tags.specialized.ButtonTag;
import j2html.tags.specialized.DivTag;
import java.util.Locale;
import java.util.Optional;
import models.Account;
import play.i18n.Messages;
import play.mvc.Http;
import play.twirl.api.Content;
import services.LocalizedStrings;
import services.MessageKey;
import views.BaseHtmlView;
import views.HtmlBundle;
import views.components.Modal;
import views.components.Modal.Width;
import views.components.ToastMessage;
import views.style.ApplicantStyles;
import views.style.ReferenceClasses;
import views.style.StyleUtils;

/** Renders a confirmation page after application submission. */
public final class ApplicantUpsellCreateAccountView extends BaseHtmlView {

  private final ApplicantLayout layout;

  @Inject
  public ApplicantUpsellCreateAccountView(ApplicantLayout layout) {
    this.layout = checkNotNull(layout);
  }

  /** Renders a sign-up page with a baked-in redirect. */
  public Content render(
      Http.Request request,
      String redirectTo,
      Account account,
      Locale locale,
      String programTitle,
      LocalizedStrings customConfirmationMessage,
      Optional<String> applicantName,
      Long applicationId,
      Messages messages,
      Optional<ToastMessage> bannerMessage) {
    String title = messages.at(MessageKey.TITLE_APPLICATION_CONFIRMATION.getKeyName());

    HtmlBundle bundle = layout.getBundle().setTitle(title);

    DivTag content =
        div()
            .withClasses(ApplicantStyles.PROGRAM_INFORMATION_BOX)
            .with(h1(title).withClasses("text-3xl", "text-black", "font-bold", "mb-4"))
            .with(
                div(messages.at(
                        MessageKey.CONTENT_CONFIRMED.getKeyName(), programTitle, applicationId))
                    .withClasses(ReferenceClasses.BT_APPLICATION_ID, "mb-4"))
            .with(div(customConfirmationMessage.getOrDefault(locale)).withClasses("mb-4"));

    Modal loginPromptModal = createLoginPromptModal(messages, redirectTo);

    // Don't show "create an account" upsell box to TIs, or anyone with an email address already.
    boolean hasEmailAddress = !Strings.isNullOrEmpty(account.getEmailAddress());
    boolean isTi = account.getMemberOfGroup().isPresent();
    if (hasEmailAddress || isTi) {
      content.with(loggedInUpsellDiv(messages, redirectTo));
    } else {
      content.with(guestUpsellDiv(messages, redirectTo, loginPromptModal.getButton()));
    }

    bannerMessage.ifPresent(bundle::addToastMessages);

    bundle.addMainStyles(ApplicantStyles.MAIN_PROGRAM_APPLICATION).addMainContent(content);
    bundle.addModals(loginPromptModal);

    return layout.renderWithNav(request, applicantName, messages, bundle);
  }

  private DivTag loggedInUpsellDiv(Messages messages, String redirectTo) {
    return div()
        .withClasses("flex", "flex-row", "justify-end")
        .with(
            redirectButton(
                    "another-program",
                    messages.at(MessageKey.LINK_APPLY_TO_ANOTHER_PROGRAM.getKeyName()),
                    redirectTo)
                .withClasses(ApplicantStyles.BUTTON_PROGRAM_APPLY_TO_ANOTHER));
  }

  private DivTag guestUpsellDiv(
      Messages messages, String loginRedirectTo, ButtonTag triggerButton) {
    return div()
        .with(
            h2(messages.at(MessageKey.TITLE_CREATE_AN_ACCOUNT.getKeyName()))
                .withClasses("mb-4", "font-bold"))
        .with(
            div(messages.at(MessageKey.CONTENT_PLEASE_CREATE_ACCOUNT.getKeyName()))
                .withClasses("mb-4"))
        .with(
            div()
                .withClasses("flex", "flex-col", "gap-4", StyleUtils.responsiveMedium("flex-row"))
                // Empty div to push buttons to the right on desktop.
                .with(div().withClasses("flex-grow"))
                .with(triggerButton)
                .with(loginButton("upsell-login-button", messages, loginRedirectTo))
                .with(createAccountButton("upsell-createaccount-button", messages)));
  }

  private static Modal createLoginPromptModal(Messages messages, String postLoginRedirectTo) {
    String modalTitle = messages.at(MessageKey.TITLE_CREATE_AN_ACCOUNT.getKeyName());
    String modalDescription = messages.at(MessageKey.LOGIN_MODAL_PROMPT.getKeyName());

    DivTag modalContent =
        div()
            .with(div().withText(modalDescription).withClass("mb-8"))
            .with(
                div()
                    .with(
                        continueWithoutAnAccountButton(messages, postLoginRedirectTo),
                        loginButton("modal-login-button", messages, postLoginRedirectTo),
                        createAccountButton("modal-createaccount-button", messages))
                    .withClasses("flex", "flex-row", "gap-x-4", "justify-end"));

    return Modal.builder()
        .setModalId(Modal.randomModalId())
        .setContent(modalContent)
        .setTriggerButtonContent(
            button(messages.at(MessageKey.LINK_APPLY_TO_ANOTHER_PROGRAM.getKeyName()))
                .withClasses(ApplicantStyles.BUTTON_NOT_RIGHT_NOW))
        .setModalTitle(modalTitle)
        .setWidth(Width.HALF)
        .build();
  }

  private static ButtonTag createAccountButton(String createAccountButtonId, Messages messages) {
    return redirectButton(
            createAccountButtonId,
            messages.at(MessageKey.BUTTON_CREATE_ACCOUNT.getKeyName()),
            routes.LoginController.register().url())
        .withClasses(ApplicantStyles.BUTTON_CREATE_ACCOUNT);
  }

  private static ButtonTag loginButton(
      String loginButtonId, Messages messages, String postLoginRedirectTo) {
    return redirectButton(
            loginButtonId,
            messages.at(MessageKey.BUTTON_LOGIN.getKeyName()),
            routes.LoginController.applicantLogin(Optional.of(postLoginRedirectTo)).url())
        .withClasses(ApplicantStyles.BUTTON_NOT_RIGHT_NOW);
  }

  private static ButtonTag continueWithoutAnAccountButton(
      Messages messages, String programsPageUri) {
    String continueWithoutAccountMessage =
        messages.at(MessageKey.BUTTON_CONTINUE_WITHOUT_AN_ACCOUNT.getKeyName());
    return redirectButton(
            "continue-without-an-account", continueWithoutAccountMessage, programsPageUri)
        .withClasses(ApplicantStyles.BUTTON_NOT_RIGHT_NOW);
  }
}
