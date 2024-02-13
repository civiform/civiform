package views.applicant;

import static j2html.TagCreator.div;
import static j2html.TagCreator.p;
import static views.questiontypes.ApplicantQuestionRendererParams.ErrorDisplayMode.DISPLAY_ERRORS_WITH_MODAL_REVIEW;

import com.google.inject.Inject;
import j2html.tags.specialized.ButtonTag;
import j2html.tags.specialized.DivTag;
import services.MessageKey;
import views.ApplicationBaseView;
import views.BaseHtmlView;
import views.components.ButtonStyles;
import views.components.Modal;
import views.style.ReferenceClasses;

/**
 * A helper class that creates a modal displayed to an applicant when the applicant clicked "Review"
 * on a block but had invalid answers.
 */
public class EditOrDiscardAnswersModalCreator extends BaseHtmlView {

  @Inject
  public EditOrDiscardAnswersModalCreator() {}

  /**
   * Creates a modal asking the applicant to either (1) edit their answers so that they pass
   * validation, or (2) discard their answers and proceed with the action they'd chosen.
   *
   * @throws IllegalArgumentException if {@code params#errorDisplayMode()} isn't a modal-displaying
   *     mode.
   */
  public Modal createModal(ApplicationBaseView.Params params) {
    if (params.errorDisplayMode() != DISPLAY_ERRORS_WITH_MODAL_REVIEW) {
      throw new IllegalArgumentException(
          "The params.errorDisplayMode() should be DISPLAY_ERRORS_WITH_MODAL_REVIEW.");
    }

    DivTag modalContent =
        div()
            .with(
                p(params.messages().at(MessageKey.MODAL_ERROR_SAVING_REVIEW_CONTENT.getKeyName())))
            .with(
                div()
                    .withClasses(
                        "flex",
                        "flex-col",
                        "sm:flex-row",
                        "flex-wrap",
                        "justify-end",
                        "my-8",
                        "gap-4")
                    .with(renderReviewWithoutSavingButton(params), renderStayAndFixButton(params)));
    return Modal.builder()
        .setModalId(Modal.randomModalId())
        .setLocation(Modal.Location.APPLICANT_FACING)
        .setContent(modalContent)
        .setModalTitle(
            params.messages().at(MessageKey.MODAL_ERROR_SAVING_REVIEW_TITLE.getKeyName()))
        .setMessages(params.messages())
        .setWidth(Modal.Width.DEFAULT)
        .setDisplayOnLoad(true)
        .build();
  }

  private ButtonTag renderStayAndFixButton(ApplicationBaseView.Params params) {
    return button(
            params.messages().at(MessageKey.MODAL_ERROR_SAVING_STAY_AND_FIX_BUTTON.getKeyName()))
        // Adding the MODAL_CLOSE class means that clicking the button will close the modal.
        .withClasses(ReferenceClasses.MODAL_CLOSE, ButtonStyles.SOLID_BLUE);
  }

  private ButtonTag renderReviewWithoutSavingButton(ApplicationBaseView.Params params) {
    String reviewUrl =
        params
            .applicantRoutes()
            .review(params.profile(), params.applicantId(), params.programId())
            .url();
    return redirectButton(
            "review-without-saving",
            params.messages().at(MessageKey.MODAL_ERROR_SAVING_REVIEW_NO_SAVE_BUTTON.getKeyName()),
            reviewUrl)
        .withClasses(ButtonStyles.OUTLINED_TRANSPARENT, "mr-2");
  }
}
