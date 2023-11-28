package views.applicant;

import static j2html.TagCreator.div;
import static j2html.TagCreator.h1;
import static j2html.TagCreator.h2;
import static j2html.TagCreator.section;

import auth.CiviFormProfile;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import controllers.applicant.ApplicantRoutes;
import j2html.tags.DomContent;
import j2html.tags.specialized.ButtonTag;
import j2html.tags.specialized.DivTag;
import j2html.tags.specialized.SectionTag;
import java.util.Optional;
import models.AccountModel;
import play.i18n.Messages;
import play.mvc.Http;
import services.MessageKey;
import views.BaseHtmlView;
import views.HtmlBundle;
import views.components.ButtonStyles;
import views.components.Modal;
import views.components.ToastMessage;
import views.style.ApplicantStyles;
import views.style.StyleUtils;

/** Base class for confirmation pages after application submission. */
public abstract class ApplicantUpsellView extends BaseHtmlView {

  protected static ButtonTag createApplyToProgramsButton(
      String buttonId,
      String buttonText,
      Long applicantId,
      CiviFormProfile profile,
      ApplicantRoutes applicantRoutes) {
    return redirectButton(buttonId, buttonText, applicantRoutes.index(profile, applicantId).url())
        .withClasses(ButtonStyles.SOLID_BLUE);
  }

  protected static DivTag createMainContent(
      String title,
      SectionTag confirmationSection,
      Boolean shouldUpsell,
      Messages messages,
      String authProviderName,
      ImmutableList<DomContent> actionButtons) {
    return div()
        .withClasses(ApplicantStyles.PROGRAM_INFORMATION_BOX)
        .with(
            h1(title).withClasses("text-3xl", "text-black", "font-bold", "mb-4"),
            confirmationSection,
            createAccountManagementSection(
                shouldUpsell, messages, authProviderName, actionButtons));
  }

  protected static HtmlBundle createHtmlBundle(
      Http.Request request,
      ApplicantLayout layout,
      String title,
      Optional<ToastMessage> bannerMessage,
      Modal loginPromptModal,
      DivTag mainContent) {
    HtmlBundle bundle = layout.getBundle(request).setTitle(title);
    bannerMessage.ifPresent(bundle::addToastMessages);
    bundle
        .addMainStyles(ApplicantStyles.MAIN_PROGRAM_APPLICATION)
        .addMainContent(mainContent)
        .addModals(loginPromptModal);
    return bundle;
  }

  /** Don't show "create an account" upsell box to TIs, or anyone with an account already. */
  protected static boolean shouldUpsell(AccountModel account) {
    return Strings.isNullOrEmpty(account.getAuthorityId()) && account.getMemberOfGroup().isEmpty();
  }

  private static SectionTag createAccountManagementSection(
      boolean shouldUpsell,
      Messages messages,
      String authProviderName,
      ImmutableList<DomContent> actionButtons) {
    return section()
        .condWith(
            shouldUpsell,
            h2(messages.at(MessageKey.TITLE_CREATE_AN_ACCOUNT.getKeyName()))
                .withClasses("mb-4", "font-bold"),
            div(messages.at(
                    MessageKey.CONTENT_PLEASE_CREATE_ACCOUNT.getKeyName(), authProviderName))
                .withClasses("mb-4"))
        .with(
            div()
                .withClasses(
                    "flex",
                    "flex-col",
                    "gap-4",
                    "justify-end",
                    StyleUtils.responsiveLarge("flex-row"))
                .with(actionButtons));
  }
}
