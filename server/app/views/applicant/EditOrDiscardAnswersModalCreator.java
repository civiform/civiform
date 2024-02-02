package views.applicant;

import static j2html.TagCreator.button;
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
 * A helper class for a modal that's displayed to an applicant when the applicant clicked "Review"
 * or "Previous" but had invalid answers.
 */
public class EditOrDiscardAnswersModalCreator extends BaseHtmlView {

  @Inject
  public EditOrDiscardAnswersModalCreator() {}

  /**
   * Creates a modal asking the applicant to either edit their answers so that they pass validation,
   * or discard their answers and proceed with whatever action they were hoping to do.
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
            .with(p(params.messages().at(MessageKey.MODAL_ERROR_ON_REVIEW_CONTENT.getKeyName())))
            .with(
                div()
                    .withClasses("flex", "my-8")
                    .with(
                        renderReviewWithoutSavingButton(params),
                        renderGoBackAndEditButton(params)));

    return Modal.builder()
        .setModalId(Modal.randomModalId())
        .setLocation(Modal.Location.APPLICANT_FACING)
        .setContent(modalContent)
        .setModalTitle(params.messages().at(MessageKey.MODAL_ERROR_ON_REVIEW_TITLE.getKeyName()))
        .setMessages(params.messages())
        .setWidth(Modal.Width.DEFAULT)
        .setDisplayOnLoad(true)
        .build();
  }

  private ButtonTag renderGoBackAndEditButton(ApplicationBaseView.Params params) {
    return button(params.messages().at(MessageKey.MODAL_GO_BACK_AND_EDIT.getKeyName()))
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
            params.messages().at(MessageKey.MODAL_ERROR_ON_REVIEW_NO_SAVE_BUTTON.getKeyName()),
            reviewUrl)
        .withClasses(ButtonStyles.OUTLINED_TRANSPARENT, "mr-2");
  }
}
