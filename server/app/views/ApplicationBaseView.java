package views;

import static j2html.TagCreator.a;
import static j2html.TagCreator.p;
import static j2html.TagCreator.rawHtml;
import static j2html.TagCreator.span;

import controllers.applicant.ApplicantRequestedAction;
import j2html.tags.DomContent;
import j2html.tags.specialized.ATag;
import j2html.tags.specialized.PTag;
import j2html.tags.specialized.SpanTag;
import play.i18n.Messages;
import services.MessageKey;
import services.settings.SettingsManifest;
import views.components.ButtonStyles;
import views.style.BaseStyles;

public class ApplicationBaseView extends BaseHtmlView {
  final String REVIEW_APPLICATION_BUTTON_ID = "review-application-button";

  /**
   * Renders a "Review" button that will also save the applicant's data before redirecting to the
   * review page (if the SAVE_ON_ALL_ACTIONS feature flag is on).
   */
  protected DomContent renderReviewButton(
      SettingsManifest settingsManifest, ApplicationBaseViewParams params) {
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
    return renderReviewButton(settingsManifest, params, formAction);
  }

  /** Renders a "Review" button with a custom action. */
  protected DomContent renderReviewButton(
      SettingsManifest settingsManifest, ApplicationBaseViewParams params, String formAction) {
    if (settingsManifest.getSaveOnAllActions(params.request())) {
      return submitButton(params.messages().at(MessageKey.BUTTON_REVIEW.getKeyName()))
          .withClasses(ButtonStyles.OUTLINED_TRANSPARENT)
          .withFormaction(formAction);
    }

    return renderOldReviewButton(params);
  }

  /**
   * Returns a "Review" button that will redirect the applicant to the review page *without* saving
   * the applicant's data.
   */
  protected ATag renderOldReviewButton(ApplicationBaseViewParams params) {
    String reviewUrl =
        params
            .applicantRoutes()
            .review(params.profile(), params.applicantId(), params.programId())
            .url();
    return a().withHref(reviewUrl)
        .withText(params.messages().at(MessageKey.BUTTON_REVIEW.getKeyName()))
        .withId(REVIEW_APPLICATION_BUTTON_ID)
        .withClasses(ButtonStyles.OUTLINED_TRANSPARENT);
  }

  /**
   * Renders a "Previous" button that will also save the applicant's data before redirecting to the
   * previous block (if the SAVE_ON_ALL_ACTIONS feature flag is on).
   */
  protected DomContent renderPreviousButton(
      SettingsManifest settingsManifest, ApplicationBaseViewParams params) {
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
    return renderPreviousButton(settingsManifest, params, formAction);
  }

  /**
   * Renders a "Previous" button with a custom action (if the SAVE_ON_ALL_ACTIONS feature flag is
   * on).
   */
  protected DomContent renderPreviousButton(
      SettingsManifest settingsManifest, ApplicationBaseViewParams params, String formAction) {
    if (settingsManifest.getSaveOnAllActions(params.request())) {
      return submitButton(params.messages().at(MessageKey.BUTTON_PREVIOUS_SCREEN.getKeyName()))
          .withClasses(ButtonStyles.OUTLINED_TRANSPARENT)
          .withFormaction(formAction);
    }
    return renderOldPreviousButton(params);
  }

  /**
   * Returns a "Previous" button that will redirect the applicant to the previous block *without*
   * saving the applicant's data.
   */
  protected ATag renderOldPreviousButton(ApplicationBaseViewParams params) {
    String redirectUrl =
        params
            .applicantRoutes()
            .blockPreviousOrReview(
                params.profile(),
                params.applicantId(),
                params.programId(),
                /* currentBlockIndex= */ params.blockIndex(),
                params.inReview())
            .url();
    return a().withHref(redirectUrl)
        .withText(params.messages().at(MessageKey.BUTTON_PREVIOUS_SCREEN.getKeyName()))
        .withClasses(ButtonStyles.OUTLINED_TRANSPARENT)
        .withId("cf-block-previous");
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
