package views.applicant;

import static com.google.common.base.Preconditions.checkNotNull;
import static j2html.TagCreator.div;
import static j2html.TagCreator.each;
import static j2html.TagCreator.form;
import static views.questiontypes.ApplicantQuestionRendererParams.ErrorDisplayMode.shouldShowErrorsWithModal;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableList;
import com.google.inject.assistedinject.Assisted;
import controllers.applicant.ApplicantRequestedAction;
import controllers.applicant.ApplicantRoutes;
import j2html.tags.ContainerTag;
import j2html.tags.specialized.ButtonTag;
import j2html.tags.specialized.DivTag;
import j2html.tags.specialized.FormTag;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import javax.inject.Inject;
import play.mvc.Http.HttpVerbs;
import play.twirl.api.Content;
import services.AlertSettings;
import services.MessageKey;
import services.applicant.question.ApplicantQuestion;
import services.question.types.QuestionDefinition;
import services.settings.SettingsManifest;
import views.AlertComponent;
import views.ApplicationBaseView;
import views.ApplicationBaseViewParams;
import views.HtmlBundle;
import views.components.ButtonStyles;
import views.components.Modal;
import views.questiontypes.ApplicantQuestionRendererFactory;
import views.questiontypes.ApplicantQuestionRendererParams;
import views.style.ApplicantStyles;

/** Renders a page for answering questions in a program screen (block). */
public final class ApplicantProgramBlockEditView extends ApplicationBaseView {
  private final String BLOCK_FORM_ID = "cf-block-form";

  private final ApplicantLayout layout;
  private final ApplicantFileUploadRenderer applicantFileUploadRenderer;
  private final ApplicantQuestionRendererFactory applicantQuestionRendererFactory;
  private final ApplicantRoutes applicantRoutes;
  private final EditOrDiscardAnswersModalCreator editOrDiscardAnswersModalCreator;
  private final SettingsManifest settingsManifest;

  @Inject
  ApplicantProgramBlockEditView(
      ApplicantLayout layout,
      ApplicantFileUploadRenderer applicantFileUploadRenderer,
      @Assisted ApplicantQuestionRendererFactory applicantQuestionRendererFactory,
      ApplicantRoutes applicantRoutes,
      EditOrDiscardAnswersModalCreator editOrDiscardAnswersModalCreator,
      SettingsManifest settingsManifest) {
    this.layout = checkNotNull(layout);
    this.applicantFileUploadRenderer = checkNotNull(applicantFileUploadRenderer);
    this.applicantQuestionRendererFactory = checkNotNull(applicantQuestionRendererFactory);
    this.applicantRoutes = checkNotNull(applicantRoutes);
    this.editOrDiscardAnswersModalCreator = checkNotNull(editOrDiscardAnswersModalCreator);
    this.settingsManifest = checkNotNull(settingsManifest);
  }

  public Content render(ApplicationBaseViewParams params) {
    DivTag blockDiv =
        div()
            .with(div(renderBlockWithSubmitForm(params)).withClasses("my-8"))
            .withClasses("my-8", "m-auto", "break-words");

    String errorMessage = "";
    if (params.block().hasErrors()
        && ApplicantQuestionRendererParams.ErrorDisplayMode.shouldShowErrors(
            params.errorDisplayMode())) {
      // Add error message to title for screen reader users.
      errorMessage = " — " + params.messages().at(MessageKey.ERROR_ANNOUNCEMENT_SR.getKeyName());
    }

    ImmutableList.Builder<Modal> modals = ImmutableList.builder();
    if (shouldShowErrorsWithModal(params.errorDisplayMode())) {
      modals.add(editOrDiscardAnswersModalCreator.createModal(params));
    }

    HtmlBundle bundle =
        layout
            .getBundle(params.request())
            .setTitle(
                layout.renderPageTitleWithBlockProgress(
                        params.programTitle(),
                        params.blockIndex(),
                        params.totalBlockCount(),
                        params.messages())
                    + errorMessage);

    Optional<DivTag> maybeBackToAdminViewButton =
        layout.maybeRenderBackToAdminViewButton(params.request(), params.programId());
    if (maybeBackToAdminViewButton.isPresent()) {
      bundle.addMainContent(maybeBackToAdminViewButton.get());
    }
    bundle.addMainContent(
        layout.renderProgramApplicationTitleAndProgressIndicator(
            params.programTitle(),
            params.blockIndex(),
            params.totalBlockCount(),
            false,
            params.messages()));

    if (params.eligibilityAlertSettings().show()) {
      bundle.addMainContent(renderEligibilityAlert(params.eligibilityAlertSettings()));
    }

    bundle
        .addMainContent(blockDiv)
        .addModals(modals.build())
        .addMainStyles(ApplicantStyles.MAIN_PROGRAM_APPLICATION);

    params.bannerToastMessage().ifPresent(bundle::addToastMessages);

    return layout.renderWithNav(
        params.request(),
        params.applicantPersonalInfo(),
        params.messages(),
        bundle,
        Optional.of(params.applicantId()));
  }

  private DivTag renderEligibilityAlert(AlertSettings eligibilityAlertSettings) {
    return AlertComponent.renderFullAlert(
        eligibilityAlertSettings.alertType(),
        eligibilityAlertSettings.text(),
        eligibilityAlertSettings.title(),
        false,
        AlertComponent.HeadingLevel.H2);
  }

  private ContainerTag<?> renderBlockWithSubmitForm(ApplicationBaseViewParams params) {
    if (params.block().isFileUpload()) {
      return applicantFileUploadRenderer.renderFileUploadBlock(
          params, applicantQuestionRendererFactory);
    }

    FormTag form = form();
    final boolean formHasErrors = params.block().hasErrors();

    if (formHasErrors
        && ApplicantQuestionRendererParams.ErrorDisplayMode.shouldShowErrors(
            params.errorDisplayMode())) {
      form.with(
          div()
              .withText(params.messages().at(MessageKey.ERROR_ANNOUNCEMENT_SR.getKeyName()))
              .attr("role", "alert")
              .attr("aria-live", "polite")
              // aria-atomic=true is necessary to make Voiceover on iOS read the error messages
              // after more than one invalid submission.
              // See https://www.w3.org/WAI/WCAG21/Techniques/aria/ARIA19
              .attr("area-atomic", "true")
              .withClasses("sr-only"));
    }

    String formAction =
        applicantRoutes
            .updateBlock(
                params.profile(),
                params.applicantId(),
                params.programId(),
                params.block().getId(),
                params.inReview(),
                ApplicantRequestedAction.NEXT_BLOCK)
            .url();

    AtomicInteger ordinalErrorCount = new AtomicInteger(0);

    return form.withId(BLOCK_FORM_ID)
        .withAction(formAction)
        .withMethod(HttpVerbs.POST)
        .with(makeCsrfTokenInputTag(params.request()))
        .with(requiredFieldsExplanationContent(params.messages()))
        .with(
            each(
                params.block().getQuestions(),
                question -> {
                  if (question.hasErrors()) {
                    ordinalErrorCount.incrementAndGet();
                  }

                  ApplicantQuestionRendererParams rendererParams =
                      ApplicantQuestionRendererParams.builder()
                          .setMessages(params.messages())
                          .setIsNameSuffixEnabled(
                              settingsManifest.getNameSuffixDropdownEnabled(params.request()))
                          .setErrorDisplayMode(params.errorDisplayMode())
                          .setAutofocus(
                              calculateAutoFocusTarget(
                                  params.errorDisplayMode(),
                                  question.getQuestionDefinition(),
                                  formHasErrors,
                                  ordinalErrorCount.get(),
                                  params.applicantSelectedQuestionName()))
                          .build();

                  return renderQuestion(question, rendererParams);
                }))
        .with(renderBottomNavButtons(params));
  }

  // One field at most should be autofocused on the page. If there are errors,
  // it should be the first field with an error of the first question with
  // errors. Otherwise, if there is a user-selected question it should be the
  // first field of that question.
  @VisibleForTesting
  ApplicantQuestionRendererParams.AutoFocusTarget calculateAutoFocusTarget(
      ApplicantQuestionRendererParams.ErrorDisplayMode errorDisplayMode,
      QuestionDefinition questionDefinition,
      boolean formHasErrors,
      int ordinalErrorCount,
      Optional<String> applicantSelectedQuestionName) {
    if (formHasErrors
        && ApplicantQuestionRendererParams.ErrorDisplayMode.shouldShowErrors(errorDisplayMode)) {
      return ordinalErrorCount == 1
          ? ApplicantQuestionRendererParams.AutoFocusTarget.FIRST_ERROR
          : ApplicantQuestionRendererParams.AutoFocusTarget.NONE;
    }

    return applicantSelectedQuestionName.map(questionDefinition.getName()::equals).orElse(false)
        ? ApplicantQuestionRendererParams.AutoFocusTarget.FIRST_FIELD
        : ApplicantQuestionRendererParams.AutoFocusTarget.NONE;
  }

  private DivTag renderQuestion(
      ApplicantQuestion question, ApplicantQuestionRendererParams params) {
    checkNotNull(
        applicantQuestionRendererFactory,
        "Must call init function for initializing ApplicantQuestionRendererFactory");
    return applicantQuestionRendererFactory
        .getRenderer(question, Optional.of(params.messages()))
        .render(params);
  }

  private DivTag renderBottomNavButtons(ApplicationBaseViewParams params) {
    return div()
        .withClasses(ApplicantStyles.APPLICATION_NAV_BAR)
        .with(renderReviewButton(params))
        .with(renderPreviousButton(params))
        .with(renderNextButton(params));
  }

  private ButtonTag renderNextButton(ApplicationBaseViewParams params) {
    return submitButton(params.messages().at(MessageKey.BUTTON_NEXT_SCREEN.getKeyName()))
        .withClasses(ButtonStyles.SOLID_BLUE);
  }
}
