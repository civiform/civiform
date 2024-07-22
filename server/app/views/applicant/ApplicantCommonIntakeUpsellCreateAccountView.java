package views.applicant;

import static com.google.common.base.Preconditions.checkNotNull;
import static j2html.TagCreator.div;
import static j2html.TagCreator.each;
import static j2html.TagCreator.h2;
import static j2html.TagCreator.p;
import static j2html.TagCreator.rawHtml;
import static j2html.TagCreator.section;
import static views.applicant.AuthenticateUpsellCreator.createLoginButton;
import static views.applicant.AuthenticateUpsellCreator.createLoginPromptModal;
import static views.applicant.AuthenticateUpsellCreator.createNewAccountButton;

import auth.CiviFormProfile;
import com.google.common.collect.ImmutableList;
import com.google.inject.Inject;
import controllers.applicant.ApplicantRoutes;
import j2html.tags.DomContent;
import j2html.tags.specialized.SectionTag;
import java.util.Locale;
import java.util.Optional;
import models.AccountModel;
import play.i18n.Messages;
import play.mvc.Http;
import play.twirl.api.Content;
import services.MessageKey;
import services.applicant.ApplicantPersonalInfo;
import services.applicant.ApplicantService;
import services.settings.SettingsManifest;
import views.components.ButtonStyles;
import views.components.Icons;
import views.components.LinkElement;
import views.components.Modal;
import views.components.ToastMessage;
import views.style.ReferenceClasses;

/** Renders a confirmation page after application submission, for the common intake form. */
public final class ApplicantCommonIntakeUpsellCreateAccountView extends ApplicantUpsellView {

  private final ApplicantLayout layout;
  private final SettingsManifest settingsManifest;

  @Inject
  public ApplicantCommonIntakeUpsellCreateAccountView(
      ApplicantLayout layout, SettingsManifest settingsManifest) {
    this.layout = checkNotNull(layout);
    this.settingsManifest = checkNotNull(settingsManifest);
  }

  /** Renders a sign-up page with a baked-in redirect. */
  public Content render(
      Http.Request request,
      String redirectTo,
      AccountModel account,
      ApplicantPersonalInfo personalInfo,
      Long applicantId,
      Long programId,
      CiviFormProfile profile,
      ImmutableList<ApplicantService.ApplicantProgramData> eligiblePrograms,
      Messages messages,
      Optional<ToastMessage> bannerMessage,
      ApplicantRoutes applicantRoutes) {
    boolean shouldUpsell = shouldUpsell(account);

    Modal loginPromptModal =
        createLoginPromptModal(
                messages,
                redirectTo,
                /* description= */ messages.at(MessageKey.GENERAL_LOGIN_MODAL_PROMPT.getKeyName()),
                /* bypassMessage= */ MessageKey.BUTTON_CONTINUE_WITHOUT_AN_ACCOUNT)
            .build();

    var actionButtonsBuilder = ImmutableList.<DomContent>builder();
    if (shouldUpsell && eligiblePrograms.isEmpty()) {
      actionButtonsBuilder.add(
          redirectButton(
                  "go-back-and-edit",
                  messages.at(MessageKey.BUTTON_GO_BACK_AND_EDIT.getKeyName()),
                  applicantRoutes.review(profile, applicantId, programId).url())
              .withClasses(ButtonStyles.OUTLINED_TRANSPARENT));
    }

    if (shouldUpsell) {
      actionButtonsBuilder.add(
          button(messages.at(MessageKey.BUTTON_APPLY_TO_PROGRAMS.getKeyName()))
              .withId(loginPromptModal.getTriggerButtonId())
              .withClasses(ButtonStyles.OUTLINED_TRANSPARENT),
          createLoginButton("sign-in", messages, redirectTo),
          createNewAccountButton("sign-up", messages));
    } else {
      actionButtonsBuilder.add(
          createApplyToProgramsButton(
              "apply-to-programs",
              messages.at(MessageKey.BUTTON_APPLY_TO_PROGRAMS.getKeyName()),
              applicantId,
              profile,
              applicantRoutes));
    }

    String title =
        profile.isTrustedIntermediary()
            ? messages.at(MessageKey.TITLE_COMMON_INTAKE_CONFIRMATION_TI.getKeyName())
            : messages.at(MessageKey.TITLE_COMMON_INTAKE_CONFIRMATION.getKeyName());
    var content =
        createMainContent(
            title,
            eligibleProgramsSection(
                    request, eligiblePrograms, messages, profile.isTrustedIntermediary())
                .withClasses("mb-4"),
            shouldUpsell,
            messages,
            // The applicant portal name should always be set (there is a
            // default setting as well).
            settingsManifest.getApplicantPortalName(request).get(),
            actionButtonsBuilder.build());
    return layout.renderWithNav(
        request,
        personalInfo,
        messages,
        createHtmlBundle(request, layout, title, bannerMessage, loginPromptModal, content),
        applicantId);
  }

  private SectionTag eligibleProgramsSection(
      Http.Request request,
      ImmutableList<ApplicantService.ApplicantProgramData> eligiblePrograms,
      Messages messages,
      boolean isTrustedIntermediary) {
    var eligibleProgramsSection = section();

    if (eligiblePrograms.isEmpty()) {
      String linkText = settingsManifest.getCommonIntakeMoreResourcesLinkText(request).get();
      var moreLink =
          new LinkElement()
              .setStyles("underline")
              .setText(linkText)
              .setHref(settingsManifest.getCommonIntakeMoreResourcesLinkHref(request).get())
              .opensInNewTab()
              .setIcon(Icons.OPEN_IN_NEW, LinkElement.IconPosition.END)
              .asAnchorText()
              .attr(
                  "aria-label",
                  linkText
                      + " "
                      + messages
                          .at(MessageKey.LINK_OPENS_NEW_TAB_SR.getKeyName())
                          .toLowerCase(Locale.ROOT));

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
