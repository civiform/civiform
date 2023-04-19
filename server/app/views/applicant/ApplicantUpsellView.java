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
import views.components.buttons.Button;
import views.components.buttons.ButtonAction;
import views.components.buttons.ButtonStyle;
import views.components.buttons.ButtonStyles;
import views.style.ApplicantStyles;
import views.style.StyleUtils;

/** Base class for confirmation pages after application submission. */
public abstract class ApplicantUpsellView extends BaseHtmlView {

  protected static Modal createLoginPromptModal(
      Messages messages, String postLoginRedirectTo, MessageKey triggerButtonMsg) {
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
                            .withClasses(ButtonStyles.SOLID_WHITE),
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
        .setTriggerButtonContent(
            Button.builder()
                .setText(messages.at(triggerButtonMsg.getKeyName()))
                .setStyle(ButtonStyle.SOLID_WHITE)
                // Trigger for modal is wired elsewhere.
                .setButtonAction(ButtonAction.ofNone())
                .build())
        .setModalTitle(modalTitle)
        .setWidth(Width.HALF)
        .build();
  }

  protected static ButtonTag createLoginButton(
      String buttonId, Messages messages, String postLoginRedirectTo) {
    return redirectButton(
            buttonId,
            messages.at(MessageKey.BUTTON_LOGIN.getKeyName()),
            routes.LoginController.applicantLogin(Optional.of(postLoginRedirectTo)).url())
        .withClasses(ButtonStyles.SOLID_WHITE);
  }

  protected static ButtonTag createNewAccountButton(String buttonId, Messages messages) {
    return redirectButton(
            buttonId,
            messages.at(MessageKey.BUTTON_CREATE_ACCOUNT.getKeyName()),
            routes.LoginController.register().url())
        .withClasses(ButtonStyles.SOLID_BLUE);
  }

  protected static ButtonTag createApplyToProgramsButton(
      String buttonId, String buttonText, Long applicantId) {
    return redirectButton(
            buttonId,
            buttonText,
            controllers.applicant.routes.ApplicantProgramsController.index(applicantId).url())
        .withClasses(ButtonStyles.SOLID_BLUE);
  }

  protected static DivTag createMainContent(
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
            createAccountManagementSection(
                shouldUpsell, messages, civicEntityFullName, actionButtons));
  }

  protected static HtmlBundle createHtmlBundle(
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

  /** Don't show "create an account" upsell box to TIs, or anyone with an account already. */
  protected static boolean shouldUpsell(Account account) {
    return Strings.isNullOrEmpty(account.getAuthorityId()) && account.getMemberOfGroup().isEmpty();
  }

  private static SectionTag createAccountManagementSection(
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
}
