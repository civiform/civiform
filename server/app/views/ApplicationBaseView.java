package views;

import static j2html.TagCreator.p;
import static j2html.TagCreator.rawHtml;
import static j2html.TagCreator.span;

import controllers.applicant.ApplicantRequestedAction;
import j2html.tags.DomContent;
import j2html.tags.specialized.PTag;
import j2html.tags.specialized.SpanTag;
import play.i18n.Messages;
import services.MessageKey;
import views.components.ButtonStyles;
import views.style.BaseStyles;

public class ApplicationBaseView extends BaseHtmlView {
  final String REVIEW_APPLICATION_BUTTON_ID = "review-application-button";

  /**
   * Renders a "Review" button that will save the applicant's data before redirecting to the review
   * page.
   */
  protected DomContent renderReviewButton(ApplicationBaseViewParams params) {
    String formAction =
        params
            .applicantRoutes()
            .updateBlock(
                params.profile(),
                params.applicantId(),
                params.programId(),
                params.block().getId(),
                params.inReview(),
                ApplicantRequestedAction.REVIEW_PAGE)
            .url();
    return submitButton(params.messages().at(MessageKey.BUTTON_REVIEW.getKeyName()))
        .withClasses(ButtonStyles.OUTLINED_TRANSPARENT)
        .withFormaction(formAction);
  }

  /**
   * Renders a "Previous" button that will save the applicant's data before redirecting to the
   * previous block.
   */
  protected DomContent renderPreviousButton(ApplicationBaseViewParams params) {
    String formAction =
        params
            .applicantRoutes()
            .updateBlock(
                params.profile(),
                params.applicantId(),
                params.programId(),
                params.block().getId(),
                params.inReview(),
                ApplicantRequestedAction.PREVIOUS_BLOCK)
            .url();
    return submitButton(params.messages().at(MessageKey.BUTTON_PREVIOUS_SCREEN.getKeyName()))
        .withClasses(ButtonStyles.OUTLINED_TRANSPARENT)
        .withFormaction(formAction);
  }

  /**
   * Renders "Note: Fields marked with a * are required."
   *
   * @param messages the localized {@link Messages} for the current applicant
   * @return PTag containing requiredness text.
   */
  public static PTag requiredFieldsExplanationContent(Messages messages) {
    SpanTag redAsterisk = span("*").withClass(BaseStyles.FORM_ERROR_TEXT_COLOR);
    return p(rawHtml(messages.at(MessageKey.REQUIRED_FIELDS_NOTE.getKeyName(), redAsterisk)))
        .withClasses("text-sm", BaseStyles.FORM_LABEL_TEXT_COLOR, "mb-2");
  }
}
