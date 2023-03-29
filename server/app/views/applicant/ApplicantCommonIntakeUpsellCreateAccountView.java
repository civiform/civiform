package views.applicant;

import static com.google.common.base.Preconditions.checkNotNull;
import static j2html.TagCreator.*;

import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import com.google.inject.Inject;
import j2html.tags.DomContent;
import j2html.tags.specialized.SectionTag;
import java.util.Optional;
import models.Account;
import play.i18n.Messages;
import play.mvc.Http;
import play.twirl.api.Content;
import services.MessageKey;
import services.applicant.ApplicantService;
import views.BaseHtmlView;
import views.HtmlBundle;
import views.components.Icons;
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
      Long applicantId,
      Long programId,
      boolean isTrustedIntermediary,
      ImmutableList<ApplicantService.ApplicantProgramData> eligiblePrograms,
      Messages messages,
      Optional<ToastMessage> bannerMessage) {
    String title =
        isTrustedIntermediary
            ? messages.at(MessageKey.TITLE_COMMON_INTAKE_CONFIRMATION_TI.getKeyName())
            : messages.at(MessageKey.TITLE_COMMON_INTAKE_CONFIRMATION.getKeyName());
    HtmlBundle bundle = layout.getBundle().setTitle(title);
    // Don't show "create an account" upsell box to TIs, or anyone with an email address already.
    boolean shouldUpsell =
        Strings.isNullOrEmpty(account.getEmailAddress()) && account.getMemberOfGroup().isEmpty();

    var actionButtonsBuilder = ImmutableList.<DomContent>builder();
    if (shouldUpsell && eligiblePrograms.isEmpty()) {
      actionButtonsBuilder.add(
          redirectButton(
                  "go-back-and-edit",
                  messages.at(MessageKey.BUTTON_GO_BACK_AND_EDIT.getKeyName()),
                  controllers.applicant.routes.ApplicantProgramReviewController.review(
                          applicantId, programId)
                      .url())
              .withClasses(ApplicantStyles.BUTTON_UPSELL_SECONDARY_ACTION));
    }

    if (shouldUpsell) {
      actionButtonsBuilder.add(
          redirectButton(
                  "apply-to-programs",
                  messages.at(MessageKey.BUTTON_APPLY_TO_PROGRAMS.getKeyName()),
                  controllers.applicant.routes.ApplicantProgramsController.index(applicantId).url())
              .withClasses(ApplicantStyles.BUTTON_UPSELL_SECONDARY_ACTION),
          redirectButton(
                  "sign-in",
                  messages.at(MessageKey.BUTTON_LOGIN.getKeyName()),
                  controllers.routes.LoginController.applicantLogin(Optional.of(redirectTo)).url())
              .withClasses(ApplicantStyles.BUTTON_UPSELL_SECONDARY_ACTION),
          redirectButton(
                  "sign-up",
                  messages.at(MessageKey.BUTTON_CREATE_AN_ACCOUNT.getKeyName()),
                  controllers.routes.LoginController.register().url())
              .withClasses(ApplicantStyles.BUTTON_UPSELL_PRIMARY_ACTION));
    } else {
      actionButtonsBuilder.add(
          redirectButton(
                  "apply-to-programs",
                  messages.at(MessageKey.BUTTON_APPLY_TO_PROGRAMS.getKeyName()),
                  controllers.applicant.routes.ApplicantProgramsController.index(applicantId).url())
              .withClasses(ApplicantStyles.BUTTON_UPSELL_PRIMARY_ACTION));
    }

    var content =
        div()
            .withClasses(ApplicantStyles.PROGRAM_INFORMATION_BOX)
            .with(
                h1(title).withClasses("text-3xl", "text-black", "font-bold", "mb-4"),
                eligibleProgramsSection(eligiblePrograms, messages, isTrustedIntermediary)
                    .withClasses("mb-4"),
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
                            .with(actionButtonsBuilder.build())));

    bannerMessage.ifPresent(bundle::addToastMessages);
    bundle.addMainStyles(ApplicantStyles.MAIN_PROGRAM_APPLICATION).addMainContent(content);
    return layout.renderWithNav(request, applicantName, messages, bundle);
  }

  private SectionTag eligibleProgramsSection(
      ImmutableList<ApplicantService.ApplicantProgramData> eligiblePrograms,
      Messages messages,
      boolean isTrustedIntermediary) {
    var eligibleProgramsSection = section();

    if (eligiblePrograms.isEmpty()) {
      var moreLink =
          new LinkElement()
              .setStyles("underline")
              .setText(
                  messages.at(
                      MessageKey.CONTENT_COMMON_INTAKE_NO_MATCHING_PROGRAMS_LINK_TEXT.getKeyName()))
              .setHref("https://access.arkansas.gov/Learn/Home")
              .opensInNewTab()
              .setIcon(Icons.OPEN_IN_NEW, LinkElement.IconPosition.END)
              .asAnchorText()
              .attr(
                  "aria-label",
                  messages.at(
                      MessageKey.CONTENT_COMMON_INTAKE_NO_MATCHING_PROGRAMS_LINK_TEXT
                          .getKeyName()
                          .toLowerCase()));

      return eligibleProgramsSection.with(
          p(isTrustedIntermediary
                  ? rawHtml(
                      messages.at(
                          MessageKey.CONTENT_COMMON_INTAKE_NO_MATCHING_PROGRAMS_TI.getKeyName(),
                          moreLink))
                  : rawHtml(
                      messages.at(
                          MessageKey.CONTENT_COMMON_INTAKE_NO_MATCHING_PROGRAMS.getKeyName(),
                          moreLink)))
              .withClasses("mb-4"),
          p(messages.at(
                  MessageKey.CONTENT_COMMON_INTAKE_NO_MATCHING_PROGRAMS_NEXT_STEP.getKeyName()))
              .withClasses("mb-4"));
    }

    return eligibleProgramsSection.with(
        section(
            each(
                eligiblePrograms,
                ep ->
                    div(
                        h2(ep.program().localizedName().getOrDefault(messages.lang().toLocale()))
                            .withClasses(
                                "text-lg",
                                "text-black",
                                "font-bold",
                                ReferenceClasses.APPLICANT_CIF_ELIGIBLE_PROGRAM_NAME),
                        p(ep.program()
                                .localizedDescription()
                                .getOrDefault(messages.lang().toLocale()))
                            .withClasses("mb-4")))),
        section(
            p(isTrustedIntermediary
                    ? messages.at(MessageKey.CONTENT_COMMON_INTAKE_CONFIRMATION_TI.getKeyName())
                    : messages.at(MessageKey.CONTENT_COMMON_INTAKE_CONFIRMATION.getKeyName()))
                .withClasses("mb-4")));
  }
}
