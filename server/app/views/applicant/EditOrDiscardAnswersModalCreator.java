package views.applicant;

import static j2html.TagCreator.div;
import static j2html.TagCreator.p;
import static views.questiontypes.ApplicantQuestionRendererParams.ErrorDisplayMode.DISPLAY_ERRORS_WITH_MODAL_PREVIOUS;
import static views.questiontypes.ApplicantQuestionRendererParams.ErrorDisplayMode.DISPLAY_ERRORS_WITH_MODAL_REVIEW;
import static views.questiontypes.ApplicantQuestionRendererParams.ErrorDisplayMode.shouldShowErrorsWithModal;

import com.google.inject.Inject;
import j2html.tags.specialized.ButtonTag;
import j2html.tags.specialized.DivTag;
import services.MessageKey;
import views.ApplicationBaseViewParams;
import views.BaseHtmlView;
import views.components.ButtonStyles;
import views.components.Modal;
import views.style.ReferenceClasses;

/**
 * A helper class that creates a modal displayed to an applicant when the applicant clicked "Review"
 * on a block but had invalid answers.
 */
public class EditOrDiscardAnswersModalCreator extends BaseHtmlView {
  private static final String WITHOUT_SAVE_BUTTON_CLASSES = ButtonStyles.OUTLINED_TRANSPARENT;

  @Inject
  public EditOrDiscardAnswersModalCreator() {}

  /**
   * Creates a modal asking the applicant to either (1) edit their answers so that they pass
   * validation, or (2) discard their answers and proceed with the action they'd chosen.
   *
   * @throws IllegalArgumentException if {@code params#errorDisplayMode()} isn't a modal-displaying
   *     mode.
   */
  public Modal createModal(ApplicationBaseViewParams params) {
    if (!shouldShowErrorsWithModal(params.errorDisplayMode())) {
      throw new IllegalArgumentException(
          String.format(
              "The params.errorDisplayMode() is %s which isn't a modal-displaying type",
              params.errorDisplayMode()));
    }

    MessageKey title;
    MessageKey content;
    ButtonTag withoutSaveButton;
    if (params.errorDisplayMode() == DISPLAY_ERRORS_WITH_MODAL_PREVIOUS) {
      title = MessageKey.MODAL_ERROR_SAVING_PREVIOUS_TITLE;
      content = MessageKey.MODAL_ERROR_SAVING_PREVIOUS_CONTENT;
      withoutSaveButton = renderPreviousWithoutSavingButton(params);
    } else if (params.errorDisplayMode() == DISPLAY_ERRORS_WITH_MODAL_REVIEW) {
      title = MessageKey.MODAL_ERROR_SAVING_REVIEW_TITLE;
      content = MessageKey.MODAL_ERROR_SAVING_REVIEW_CONTENT;
      withoutSaveButton = renderReviewWithoutSavingButton(params);
    } else {
      throw new IllegalStateException("The params.errorDisplayMode() should be checked above");
    }

    DivTag modalContent =
        div()
            .with(p(params.messages().at(content.getKeyName())))
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
                    .with(withoutSaveButton, renderStayAndFixButton(params)));

    return Modal.builder()
        .setModalId(Modal.randomModalId())
        .setLocation(Modal.Location.APPLICANT_FACING)
        .setContent(modalContent)
        .setModalTitle(params.messages().at(title.getKeyName()))
        .setMessages(params.messages())
        .setWidth(Modal.Width.DEFAULT)
        .setDisplayOnLoad(true)
        .build();
  }

  private ButtonTag renderStayAndFixButton(ApplicationBaseViewParams params) {
    return button(
            params.messages().at(MessageKey.MODAL_ERROR_SAVING_STAY_AND_FIX_BUTTON.getKeyName()))
        // Adding the MODAL_CLOSE class means that clicking the button will close the modal.
        .withClasses(ReferenceClasses.MODAL_CLOSE, ButtonStyles.SOLID_BLUE, "ml-2");
  }

  private ButtonTag renderReviewWithoutSavingButton(ApplicationBaseViewParams params) {
    String reviewUrl =
        params
            .applicantRoutes()
            .review(params.profile(), params.applicantId(), params.programId())
            .url();
    return redirectButton(
            "review-without-saving",
            params.messages().at(MessageKey.MODAL_ERROR_SAVING_REVIEW_NO_SAVE_BUTTON.getKeyName()),
            reviewUrl)
        .withClasses(WITHOUT_SAVE_BUTTON_CLASSES);
  }

  private ButtonTag renderPreviousWithoutSavingButton(ApplicationBaseViewParams params) {
    return redirectButton(
            "previous-without-saving",
            params
                .messages()
                .at(MessageKey.MODAL_ERROR_SAVING_PREVIOUS_NO_SAVE_BUTTON.getKeyName()),
            params
                .applicantRoutes()
                .blockPreviousOrReview(
                    params.profile(),
                    params.applicantId(),
                    params.programId(),
                    params.blockIndex(),
                    params.inReview())
                .url())
        .withClasses(WITHOUT_SAVE_BUTTON_CLASSES);
  }
}
