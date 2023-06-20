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

import annotations.BindingAnnotations;
import com.google.common.collect.ImmutableList;
import com.google.inject.Inject;
import com.typesafe.config.Config;
import j2html.tags.DomContent;
import j2html.tags.specialized.SectionTag;
import java.util.Optional;
import models.Account;
import play.i18n.Messages;
import play.mvc.Http;
import play.twirl.api.Content;
import services.MessageKey;
import services.applicant.ApplicantPersonalInfo;
import services.applicant.ApplicantService;
import views.components.ButtonStyles;
import views.components.Icons;
import views.components.LinkElement;
import views.components.Modal;
import views.components.ToastMessage;
import views.style.ReferenceClasses;

/** Renders a confirmation page after application submission, for the common intake form. */
public final class ApplicantCommonIntakeUpsellCreateAccountView extends ApplicantUpsellView {

  private final ApplicantLayout layout;
  private final Config configuration;
  private final String authProviderName;

  @Inject
  public ApplicantCommonIntakeUpsellCreateAccountView(
      ApplicantLayout layout,
      @BindingAnnotations.ApplicantAuthProviderName String authProviderName,
      Config configuration) {
    this.layout = checkNotNull(layout);
    this.configuration = checkNotNull(configuration);
    this.authProviderName = checkNotNull(authProviderName);
  }

  /** Renders a sign-up page with a baked-in redirect. */
  public Content render(
      Http.Request request,
      String redirectTo,
      Account account,
      ApplicantPersonalInfo personalInfo,
      Long applicantId,
      Long programId,
      boolean isTrustedIntermediary,
      ImmutableList<ApplicantService.ApplicantProgramData> eligiblePrograms,
      Messages messages,
      Optional<ToastMessage> bannerMessage) {
    boolean shouldUpsell = shouldUpsell(account);

    Modal loginPromptModal =
        createLoginPromptModal(
                messages,
                redirectTo,
                /* description =*/ messages.at(MessageKey.GENERAL_LOGIN_MODAL_PROMPT.getKeyName()),
                /* bypassMessage= */ MessageKey.BUTTON_CONTINUE_WITHOUT_AN_ACCOUNT)
            .build();

    var actionButtonsBuilder = ImmutableList.<DomContent>builder();
    if (shouldUpsell && eligiblePrograms.isEmpty()) {
      actionButtonsBuilder.add(
          redirectButton(
                  "go-back-and-edit",
                  messages.at(MessageKey.BUTTON_GO_BACK_AND_EDIT.getKeyName()),
                  controllers.applicant.routes.ApplicantProgramReviewController.review(
                          applicantId, programId)
                      .url())
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
              applicantId));
    }

    String title =
        isTrustedIntermediary
            ? messages.at(MessageKey.TITLE_COMMON_INTAKE_CONFIRMATION_TI.getKeyName())
            : messages.at(MessageKey.TITLE_COMMON_INTAKE_CONFIRMATION.getKeyName());
    var content =
        createMainContent(
            title,
            eligibleProgramsSection(eligiblePrograms, messages, isTrustedIntermediary)
                .withClasses("mb-4"),
            shouldUpsell,
            messages,
            authProviderName,
            actionButtonsBuilder.build());
    return layout.renderWithNav(
        request,
        personalInfo,
        messages,
        createHtmlBundle(layout, title, bannerMessage, loginPromptModal, content));
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
              .setText(configuration.getString("common_intake_more_resources_link_text"))
              .setHref(configuration.getString("common_intake_more_resources_link_href"))
              .opensInNewTab()
              .setIcon(Icons.OPEN_IN_NEW, LinkElement.IconPosition.END)
              .asAnchorText()
              .attr(
                  "aria-label", configuration.getString("common_intake_more_resources_link_text"));

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
