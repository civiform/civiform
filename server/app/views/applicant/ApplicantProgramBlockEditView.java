package views.applicant;

import static com.google.common.base.Preconditions.checkNotNull;
import static j2html.TagCreator.div;
import static j2html.TagCreator.each;
import static j2html.TagCreator.form;

import com.google.inject.assistedinject.Assisted;
import controllers.applicant.routes;
import j2html.tags.ContainerTag;
import j2html.tags.specialized.ButtonTag;
import j2html.tags.specialized.DivTag;
import j2html.tags.specialized.FormTag;
import javax.inject.Inject;
import play.i18n.Messages;
import play.mvc.Http.HttpVerbs;
import play.twirl.api.Content;
import services.MessageKey;
import services.applicant.question.ApplicantQuestion;
import views.ApplicationBaseView;
import views.FileUploadViewStrategy;
import views.HtmlBundle;
import views.components.ToastMessage;
import views.questiontypes.ApplicantQuestionRendererFactory;
import views.questiontypes.ApplicantQuestionRendererParams;
import views.style.ApplicantStyles;

/** Renders a page for answering questions in a program screen (block). */
public final class ApplicantProgramBlockEditView extends ApplicationBaseView {
  private final String BLOCK_FORM_ID = "cf-block-form";

  private final ApplicantLayout layout;
  private final FileUploadViewStrategy fileUploadStrategy;
  private final ApplicantQuestionRendererFactory applicantQuestionRendererFactory;

  @Inject
  ApplicantProgramBlockEditView(
      ApplicantLayout layout,
      FileUploadViewStrategy fileUploadStrategy,
      @Assisted ApplicantQuestionRendererFactory applicantQuestionRendererFactory) {
    this.layout = checkNotNull(layout);
    this.fileUploadStrategy = checkNotNull(fileUploadStrategy);
    this.applicantQuestionRendererFactory = applicantQuestionRendererFactory;
  }

  public Content render(Params params) {
    DivTag blockDiv =
        div()
            .with(div(renderBlockWithSubmitForm(params)).withClasses("my-8"))
            .withClasses("my-8", "m-auto");

    String errorMessage = "";
    if (params.block().hasErrors()
        && ApplicantQuestionRendererParams.ErrorDisplayMode.DISPLAY_ERRORS.equals(
            params.errorDisplayMode())) {
      // Add error message to title for screen reader users.
      errorMessage = " — " + params.messages().at(MessageKey.ERROR_ANNOUNCEMENT_SR.getKeyName());
    }
    HtmlBundle bundle =
        layout
            .getBundle()
            .setTitle(
                layout.renderPageTitleWithBlockProgress(
                        params.programTitle(), params.blockIndex(), params.totalBlockCount())
                    + errorMessage)
            .addMainContent(
                layout.renderProgramApplicationTitleAndProgressIndicator(
                    params.programTitle(), params.blockIndex(), params.totalBlockCount(), false),
                blockDiv)
            .addMainStyles(ApplicantStyles.MAIN_PROGRAM_APPLICATION);

    if (!params.preferredLanguageSupported()) {
      bundle.addMainContent(
          renderLocaleNotSupportedToast(
              params.applicantId(), params.programId(), params.messages()));
    }
    return layout.renderWithNav(
        params.request(), params.applicantName(), params.messages(), bundle);
  }

  /**
   * If the applicant's preferred language is not supported for this program, render a toast
   * warning. Allow them to dismiss the warning, and once it is dismissed it does not reappear for
   * the same program.
   */
  private DivTag renderLocaleNotSupportedToast(
      long applicantId, long programId, Messages messages) {
    // Note: we include applicantId and programId in the ID, so that the applicant sees the warning
    // for each program that is not properly localized. Otherwise, once dismissed, this toast would
    // never appear for other programs. Additionally, including the applicantId ensures that this
    // warning still appears across applicants, so that (for example) a Trusted Intermediary
    // handling multiple applicants will see the toast displayed.
    return ToastMessage.warning(messages.at(MessageKey.TOAST_LOCALE_NOT_SUPPORTED.getKeyName()))
        .setId(String.format("locale-not-supported-%d-%d", applicantId, programId))
        .setDismissible(true)
        .setIgnorable(true)
        .setDuration(0)
        .getContainerTag();
  }

  private ContainerTag<?> renderBlockWithSubmitForm(Params params) {
    if (params.block().isFileUpload()) {
      return fileUploadStrategy.renderFileUploadBlock(params, applicantQuestionRendererFactory);
    }

    FormTag form = form();

    if (params.block().hasErrors()
        && ApplicantQuestionRendererParams.ErrorDisplayMode.DISPLAY_ERRORS.equals(
            params.errorDisplayMode())) {
      form.with(
          div()
              .withText(params.messages().at(MessageKey.ERROR_ANNOUNCEMENT_SR.getKeyName()))
              .attr("role", "alert")
              .attr("aria-live", "polite")
              .withClasses("sr-only"));
    }

    String formAction =
        routes.ApplicantProgramBlocksController.update(
                params.applicantId(), params.programId(), params.block().getId(), params.inReview())
            .url();
    ApplicantQuestionRendererParams rendererParams =
        ApplicantQuestionRendererParams.builder()
            .setMessages(params.messages())
            .setErrorDisplayMode(params.errorDisplayMode())
            .build();

    return form.withId(BLOCK_FORM_ID)
        .withAction(formAction)
        .withMethod(HttpVerbs.POST)
        .with(makeCsrfTokenInputTag(params.request()))
        .with(this.requiredFieldsExplanationContent(params.messages()))
        .with(
            each(
                params.block().getQuestions(),
                question -> renderQuestion(question, rendererParams)))
        .with(renderBottomNavButtons(params));
  }

  private DivTag renderQuestion(
      ApplicantQuestion question, ApplicantQuestionRendererParams params) {
    checkNotNull(
        applicantQuestionRendererFactory,
        "Must call init function for initializing ApplicantQuestionRendererFactory");
    return applicantQuestionRendererFactory.getRenderer(question).render(params);
  }

  private DivTag renderBottomNavButtons(Params params) {
    return div()
        .withClasses(ApplicantStyles.APPLICATION_NAV_BAR)
        // An empty div to take up the space to the left of the buttons.
        .with(div().withClasses("flex-grow"))
        .with(renderReviewButton(params))
        .with(renderPreviousButton(params))
        .with(renderNextButton(params));
  }

  private ButtonTag renderNextButton(Params params) {
    return submitButton(params.messages().at(MessageKey.BUTTON_NEXT_SCREEN.getKeyName()))
        .withClasses(ApplicantStyles.BUTTON_BLOCK_NEXT)
        .withId("cf-block-submit");
  }
}
