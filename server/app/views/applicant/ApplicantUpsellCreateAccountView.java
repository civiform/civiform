package views.applicant;

import static com.google.common.base.Preconditions.checkNotNull;
import static j2html.TagCreator.div;
import static j2html.TagCreator.h1;
import static j2html.TagCreator.h2;
import static j2html.TagCreator.section;

import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import com.google.inject.Inject;
import controllers.routes;
import j2html.tags.DomContent;
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
      Long applicantId,
      Long applicationId,
      Messages messages,
      Optional<ToastMessage> bannerMessage) {
    String title = messages.at(MessageKey.TITLE_APPLICATION_CONFIRMATION.getKeyName());
    HtmlBundle bundle = layout.getBundle().setTitle(title);
    // Don't show "create an account" upsell box to TIs, or anyone with an account already.
    boolean shouldUpsell =
        Strings.isNullOrEmpty(account.getAuthorityId()) && account.getMemberOfGroup().isEmpty();

    Modal loginPromptModal = createLoginPromptModal(messages, redirectTo);

    ImmutableList<DomContent> actionButtons =
        shouldUpsell
            ? ImmutableList.of(
                loginPromptModal.getButton(), // apply to another program
                loginButton("sign-in", messages, redirectTo),
                createAccountButton("sign-up", messages))
            : ImmutableList.of(
                redirectButton(
                        "another-program",
                        messages.at(MessageKey.LINK_APPLY_TO_ANOTHER_PROGRAM.getKeyName()),
                        controllers.applicant.routes.ApplicantProgramsController.index(applicantId)
                            .url())
                    .withClasses(ApplicantStyles.BUTTON_UPSELL_PRIMARY_ACTION));

    var content =
        div()
            .withClasses(ApplicantStyles.PROGRAM_INFORMATION_BOX)
            .with(
                h1(title).withClasses("text-3xl", "text-black", "font-bold", "mb-4"),
                section(
                    div(messages.at(
                            MessageKey.CONTENT_CONFIRMED.getKeyName(), programTitle, applicationId))
                        .withClasses(ReferenceClasses.BT_APPLICATION_ID, "mb-4"),
                    div(customConfirmationMessage.getOrDefault(locale)).withClasses("mb-4")),
                section()
                    .condWith(
                        shouldUpsell,
                        h2(messages.at(MessageKey.TITLE_CREATE_AN_ACCOUNT.getKeyName()))
                            .withClasses("mb-4", "font-bold"),
                        div(messages.at(MessageKey.CONTENT_PLEASE_CREATE_ACCOUNT.getKeyName()))
                            .withClasses("mb-4"))
                    .with(
                        div()
                            .withClasses(
                                "flex",
                                "flex-col",
                                "gap-4",
                                "justify-end",
                                StyleUtils.responsiveMedium("flex-row"))
                            .with(actionButtons)));

    bannerMessage.ifPresent(bundle::addToastMessages);
    bundle.addMainStyles(ApplicantStyles.MAIN_PROGRAM_APPLICATION).addMainContent(content);
    bundle.addModals(loginPromptModal);
    return layout.renderWithNav(request, applicantName, messages, bundle);
  }

  private static Modal createLoginPromptModal(Messages messages, String postLoginRedirectTo) {
    String modalTitle = messages.at(MessageKey.TITLE_CREATE_AN_ACCOUNT.getKeyName());
    String modalDescription = messages.at(MessageKey.LOGIN_MODAL_PROMPT.getKeyName());

    DivTag modalContent =
        div()
            .with(
                div().withText(modalDescription).withClass("mb-8"),
                div()
                    .with(
                        redirectButton(
                                "continue-without-an-account",
                                messages.at(
                                    MessageKey.BUTTON_CONTINUE_WITHOUT_AN_ACCOUNT.getKeyName()),
                                postLoginRedirectTo)
                            .withClasses(ApplicantStyles.BUTTON_UPSELL_SECONDARY_ACTION),
                        loginButton("modal-sign-in", messages, postLoginRedirectTo),
                        createAccountButton("modal-sign-up", messages))
                    .withClasses(
                        "flex",
                        "flex-col",
                        "gap-4",
                        "justify-end",
                        StyleUtils.responsiveMedium("flex-row")));

    return Modal.builder()
        .setModalId(Modal.randomModalId())
        .setContent(modalContent)
        .setTriggerButtonContent(
            button(messages.at(MessageKey.LINK_APPLY_TO_ANOTHER_PROGRAM.getKeyName()))
                .withClasses(ApplicantStyles.BUTTON_UPSELL_SECONDARY_ACTION))
        .setModalTitle(modalTitle)
        .setWidth(Width.HALF)
        .build();
  }

  private static ButtonTag loginButton(
      String loginButtonId, Messages messages, String postLoginRedirectTo) {
    return redirectButton(
            loginButtonId,
            messages.at(MessageKey.BUTTON_LOGIN.getKeyName()),
            routes.LoginController.applicantLogin(Optional.of(postLoginRedirectTo)).url())
        .withClasses(ApplicantStyles.BUTTON_UPSELL_SECONDARY_ACTION);
  }

  private static ButtonTag createAccountButton(String createAccountButtonId, Messages messages) {
    return redirectButton(
            createAccountButtonId,
            messages.at(MessageKey.BUTTON_CREATE_ACCOUNT.getKeyName()),
            routes.LoginController.register().url())
        .withClasses(ApplicantStyles.BUTTON_UPSELL_PRIMARY_ACTION);
  }
}
