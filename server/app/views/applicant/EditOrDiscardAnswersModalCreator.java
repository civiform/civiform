package views.applicant;

import static j2html.TagCreator.button;
import static j2html.TagCreator.div;
import static j2html.TagCreator.p;

import com.google.inject.Inject;
import controllers.applicant.ApplicantRequestedAction;
import j2html.tags.specialized.ButtonTag;
import j2html.tags.specialized.DivTag;
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
   * @param applicantRequestedAction specifies where the applicant should be taken to next if they
   *     decide to discard their answers. should never be {@link
   *     ApplicantRequestedAction#NEXT_BLOCK}, because the "Save & next" button should show the
   *     validation errors without any modal.
   */
  public Modal createModal(
      ApplicationBaseView.Params params, ApplicantRequestedAction applicantRequestedAction) {
    if (applicantRequestedAction == ApplicantRequestedAction.NEXT_BLOCK) {
      throw new IllegalArgumentException(
          "The applicantRequestedAction should never be NEXT_BLOCK.");
    }
    // TODO: Use message strings
    DivTag modalContent =
        div()
            .with(
                p(
                    "There's some errors with the information you've filled in. Would you like to"
                        + " go back and fix the errors, or go to the review page without"
                        + " saving?"))
            .with(
                div()
                    .withClasses("flex", "my-8")
                    .with(renderReviewWithoutSavingButton(params), renderGoBackAndEditButton()));

    return Modal.builder()
        .setModalId(Modal.randomModalId())
        .setLocation(Modal.Location.APPLICANT_FACING)
        .setContent(modalContent)
        .setModalTitle("Edit or discard")
        .setMessages(params.messages())
        .setWidth(Modal.Width.DEFAULT)
        .setDisplayOnLoad(true)
        .build();
  }

  private ButtonTag renderGoBackAndEditButton() {
    return button("Go back and edit")
        .withClasses(ReferenceClasses.MODAL_CLOSE, ButtonStyles.SOLID_BLUE);
  }

  private ButtonTag renderReviewWithoutSavingButton(ApplicationBaseView.Params params) {
    String reviewUrl =
        params
            .applicantRoutes()
            .review(params.profile(), params.applicantId(), params.programId())
            .url();
    return redirectButton("review-without-saving", "Review without saving", reviewUrl)
        .withClasses(ButtonStyles.OUTLINED_TRANSPARENT, "mr-2");
  }
}
