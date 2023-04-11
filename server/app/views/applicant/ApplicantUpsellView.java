package views.applicant;

import static j2html.TagCreator.div;
import static j2html.TagCreator.h1;
import static j2html.TagCreator.h2;
import static j2html.TagCreator.section;

import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import controllers.routes;
import j2html.tags.DomContent;
import j2html.tags.specialized.ButtonTag;
import j2html.tags.specialized.DivTag;
import j2html.tags.specialized.SectionTag;
import java.util.Optional;
import models.Account;
import play.i18n.Messages;
import services.MessageKey;
import views.BaseHtmlView;
import views.HtmlBundle;
import views.components.Modal;
import views.components.Modal.Width;
import views.components.ToastMessage;
import views.style.ApplicantStyles;
import views.style.StyleUtils;

/** Renders a confirmation page after application submission. */
public abstract class ApplicantUpsellView extends BaseHtmlView {

  protected static Modal createLoginPromptModal(
      Messages messages, String postLoginRedirectTo, MessageKey applyToProgramsButtonMsg) {
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
                                messages.at(applyToProgramsButtonMsg.getKeyName()),
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
            button(messages.at(applyToProgramsButtonMsg.getKeyName()))
                .withClasses(ApplicantStyles.BUTTON_UPSELL_SECONDARY_ACTION))
        .setModalTitle(modalTitle)
        .setWidth(Width.HALF)
        .build();
  }

  protected static ButtonTag loginButton(
      String loginButtonId, Messages messages, String postLoginRedirectTo) {
    return redirectButton(
            loginButtonId,
            messages.at(MessageKey.BUTTON_LOGIN.getKeyName()),
            routes.LoginController.applicantLogin(Optional.of(postLoginRedirectTo)).url())
        .withClasses(ApplicantStyles.BUTTON_UPSELL_SECONDARY_ACTION);
  }

  protected static ButtonTag createAccountButton(String createAccountButtonId, Messages messages) {
    return redirectButton(
            createAccountButtonId,
            messages.at(MessageKey.BUTTON_CREATE_ACCOUNT.getKeyName()),
            routes.LoginController.register().url())
        .withClasses(ApplicantStyles.BUTTON_UPSELL_PRIMARY_ACTION);
  }

  protected static ButtonTag applyToProgramsButton(
      String buttonId, String buttonText, Long applicantId) {
    return redirectButton(
            buttonId,
            buttonText,
            controllers.applicant.routes.ApplicantProgramsController.index(applicantId).url())
        .withClasses(ApplicantStyles.BUTTON_UPSELL_PRIMARY_ACTION);
  }

  protected static SectionTag accountManagementSection(
      boolean shouldUpsell,
      Messages messages,
      String civicEntityFullName,
      ImmutableList<DomContent> actionButtons) {
    return section()
        .condWith(
            shouldUpsell,
            h2(messages.at(MessageKey.TITLE_CREATE_AN_ACCOUNT.getKeyName()))
                .withClasses("mb-4", "font-bold"),
            div(messages.at(
                    MessageKey.CONTENT_PLEASE_CREATE_ACCOUNT.getKeyName(), civicEntityFullName))
                .withClasses("mb-4"))
        .with(
            div()
                .withClasses(
                    "flex",
                    "flex-col",
                    "gap-4",
                    "justify-end",
                    StyleUtils.responsiveMedium("flex-row"))
                .with(actionButtons));
  }

  protected static DivTag mainContent(
      String title,
      SectionTag confirmationSection,
      Boolean shouldUpsell,
      Messages messages,
      String civicEntityFullName,
      ImmutableList<DomContent> actionButtons) {
    return div()
        .withClasses(ApplicantStyles.PROGRAM_INFORMATION_BOX)
        .with(
            h1(title).withClasses("text-3xl", "text-black", "font-bold", "mb-4"),
            confirmationSection,
            accountManagementSection(shouldUpsell, messages, civicEntityFullName, actionButtons));
  }

  protected static HtmlBundle bundle(
      ApplicantLayout layout,
      String title,
      Optional<ToastMessage> bannerMessage,
      Modal loginPromptModal,
      DivTag mainContent) {
    HtmlBundle bundle = layout.getBundle().setTitle(title);
    bannerMessage.ifPresent(bundle::addToastMessages);
    bundle
        .addMainStyles(ApplicantStyles.MAIN_PROGRAM_APPLICATION)
        .addMainContent(mainContent)
        .addModals(loginPromptModal);
    return bundle;
  }

  /** Don't show "create an account" upsell box to TIs, or anyone with an email address already. */
  protected static boolean shouldUpsell(Account account) {
    return Strings.isNullOrEmpty(account.getAuthorityId()) && account.getMemberOfGroup().isEmpty();
  }
}
