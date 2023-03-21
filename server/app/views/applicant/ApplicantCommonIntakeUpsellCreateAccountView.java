package views.applicant;

import static com.google.common.base.Preconditions.checkNotNull;
import static j2html.TagCreator.div;
import static j2html.TagCreator.each;
import static j2html.TagCreator.h1;
import static j2html.TagCreator.h2;
import static j2html.TagCreator.h3;
import static j2html.TagCreator.p;
import static j2html.TagCreator.section;

import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import com.google.inject.Inject;
import controllers.routes;
import j2html.tags.specialized.DivTag;
import java.util.Locale;
import java.util.Optional;
import models.Account;
import play.i18n.Messages;
import play.mvc.Http;
import play.twirl.api.Content;
import services.MessageKey;
import services.applicant.ApplicantService;
import views.BaseHtmlView;
import views.HtmlBundle;
import views.components.LinkElement;
import views.components.ToastMessage;
import views.style.ApplicantStyles;
import views.style.ReferenceClasses;
import views.style.StyleUtils;

/** Renders a confirmation page after application submission, for the common intake form. */
public final class ApplicantCommonIntakeUpsellCreateAccountView extends BaseHtmlView {

  private final ApplicantLayout layout;

  @Inject
  public ApplicantCommonIntakeUpsellCreateAccountView(ApplicantLayout layout) {
    this.layout = checkNotNull(layout);
  }

  /** Renders a sign-up page with a baked-in redirect. */
  public Content render(
      Http.Request request,
      String redirectTo,
      Account account,
      Optional<String> applicantName,
      ImmutableList<ApplicantService.ApplicantProgramData> eligiblePrograms,
      Messages messages,
      Optional<ToastMessage> bannerMessage) {
//  String title = messages.at(MessageKey.TITLE_APPLICATION_CONFIRMATION.getKeyName());
    String title = "Benefits you may qualify for";

    HtmlBundle bundle = layout.getBundle().setTitle(title);

    DivTag content =
        div()
            .withClasses(ApplicantStyles.PROGRAM_INFORMATION_BOX)
            .with(h1(title).withClasses("text-3xl", "text-black", "font-bold", "mb-4"))
            .with(
                eligibleProgramsSection(eligiblePrograms, messages.lang().toLocale())
                    .withClasses(ReferenceClasses.BT_APPLICATION_ID, "mb-4"));

    DivTag createAccountSection =
        div()
            .with(
                h2(messages.at(MessageKey.TITLE_CREATE_AN_ACCOUNT.getKeyName()))
                    .withClasses("mb-4", "font-bold"))
            .with(
                div(messages.at(MessageKey.CONTENT_PLEASE_CREATE_ACCOUNT.getKeyName()))
                    .withClasses("mb-4"))
            .with(
                div()
                    .withClasses(
                        "flex", "flex-col", "gap-4", StyleUtils.responsiveMedium("flex-row"))
                    // Empty div to push buttons to the right on desktop.
                    .with(div().withClasses("flex-grow"))
                    .with(
                        redirectButton(
                                "another-program",
                                messages.at(MessageKey.LINK_APPLY_TO_ANOTHER_PROGRAM.getKeyName()),
                                redirectTo)
                            .withClasses(ApplicantStyles.BUTTON_NOT_RIGHT_NOW))
                    .with(
                        redirectButton(
                                "all-done",
                                messages.at(MessageKey.LINK_ALL_DONE.getKeyName()),
                                org.pac4j.play.routes.LogoutController.logout().url())
                            .withClasses(ApplicantStyles.BUTTON_NOT_RIGHT_NOW))
                    .with(
                        redirectButton(
                                "sign-in",
                                messages.at(MessageKey.LINK_CREATE_ACCOUNT_OR_SIGN_IN.getKeyName()),
                                routes.LoginController.applicantLogin(Optional.of(redirectTo))
                                    .url())
                            .withClasses(ApplicantStyles.BUTTON_CREATE_ACCOUNT)));

    // Don't show "create an account" upsell box to TIs, or anyone with an email address already.
    if (Strings.isNullOrEmpty(account.getEmailAddress()) && account.getMemberOfGroup().isEmpty()) {
      content.with(createAccountSection);
    } else {
      content.with(
          new LinkElement()
              .setHref(redirectTo)
              .setText(messages.at(MessageKey.LINK_APPLY_TO_ANOTHER_PROGRAM.getKeyName()))
              .asAnchorText());
    }

    bannerMessage.ifPresent(bundle::addToastMessages);

    bundle.addMainStyles(ApplicantStyles.MAIN_PROGRAM_APPLICATION).addMainContent(content);

    return layout.renderWithNav(request, applicantName, messages, bundle);
  }

  private DivTag eligibleProgramsSection(
      ImmutableList<ApplicantService.ApplicantProgramData> eligiblePrograms,
      Locale preferredLocale) {
    DivTag eligibleProgramsDiv = div();

    if (eligiblePrograms.isEmpty()) {
      return eligibleProgramsDiv.with(
          p().withClasses("mb-4")
              .withText(
                  "The pre-screener could not find programs you may qualify for. However, you may"
                      + " apply for benefits at any time, by clicking ‘Apply to programs’. Or you"
                      + " can visit the State of Arkansas benefits site for additional benefit"
                      + " programs."),
          p().withClasses("mb-4")
              .withText("You can also return to the previous page to edit your answers."));
    }

    return eligibleProgramsDiv.with(
        section(
            each(
                eligiblePrograms,
                ep ->
                    div(
                        h3().withClasses("text-black", "font-bold")
                            .withText(ep.program().localizedName().getOrDefault(preferredLocale)),
                        p().withText(
                                ep.program()
                                    .localizedDescription()
                                    .getOrDefault(preferredLocale))))),
        section(
            p().withText(
                    "You may be able to get these benefits if you apply through the online"
                        + " application by clicking ‘Apply to programs’. You can also return to"
                        + " the previous page to edit your answers.")));
  }
}
