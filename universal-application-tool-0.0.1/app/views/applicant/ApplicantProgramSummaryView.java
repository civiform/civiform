package views.applicant;

import static com.google.common.base.Preconditions.checkNotNull;
import static j2html.TagCreator.br;
import static j2html.TagCreator.div;
import static j2html.TagCreator.form;
import static j2html.TagCreator.h1;
import static j2html.TagCreator.header;

import com.google.common.collect.ImmutableList;
import com.google.inject.Inject;
import controllers.applicant.routes;
import j2html.tags.ContainerTag;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.Optional;
import play.i18n.Messages;
import play.mvc.Http.HttpVerbs;
import play.mvc.Http.Request;
import play.twirl.api.Content;
import services.applicant.AnswerData;
import views.BaseHtmlView;
import views.components.LinkElement;
import views.components.ToastMessage;
import views.style.ReferenceClasses;
import views.style.Styles;

public final class ApplicantProgramSummaryView extends BaseHtmlView {

  private final ApplicantLayout layout;

  @Inject
  public ApplicantProgramSummaryView(ApplicantLayout layout) {
    this.layout = checkNotNull(layout);
  }

  /**
   * Renders a summary of all of the applicant's data for a specific application. Data includes:
   * Program Id, Applicant Id - Needed for link context (submit & edit) Question Data for each
   * question: - question text - answer text - block id (for edit link)
   */
  public Content render(
      Request request,
      Long applicantId,
      Long programId,
      String programTitle,
      ImmutableList<AnswerData> data,
      Messages messages,
      Optional<String> banner) {
    ContainerTag headerTag = renderHeader(100);

    ContainerTag content = div().withClasses(Styles.MX_16);

    if (banner.isPresent()) {
      content.with(ToastMessage.error(banner.get()).getContainerTag());
    }

    ContainerTag applicationSummary = div().withId("application-summary");
    for (AnswerData questionData : data) {
      applicationSummary.with(renderQuestionSummary(questionData, applicantId));
    }
    content.with(applicationSummary);

    // Add submit action (POST).
    String submitLink =
        routes.ApplicantProgramReviewController.submit(applicantId, programId).url();
    ContainerTag actions =
        form()
            .withAction(submitLink)
            .withMethod(HttpVerbs.POST)
            .with(makeCsrfTokenInputTag(request))
            .with(submitButton("Submit"));
    content.with(actions);

    return layout.render(
        messages,
        headerTag,
        h1("Application review for " + programTitle).withClasses(Styles.PX_16, Styles.PY_4),
        content);
  }

  private ContainerTag renderHeader(int percentComplete) {
    ContainerTag headerTag = header().withClasses(Styles.FLEX, Styles.FLEX_COL, Styles._MT_12);
    ContainerTag progressInner =
        div()
            .withClasses(
                Styles.BG_YELLOW_400,
                Styles.TRANSITION_ALL,
                Styles.DURATION_300,
                Styles.H_FULL,
                Styles.BLOCK,
                Styles.ABSOLUTE,
                Styles.LEFT_0,
                Styles.TOP_0,
                Styles.W_1,
                Styles.ROUNDED_R_FULL)
            .withStyle("width:" + percentComplete + "%");
    ContainerTag progressIndicator =
        div(progressInner)
            .withId("progress-indicator")
            .withClasses(
                Styles.BORDER,
                Styles.FONT_SEMIBOLD,
                Styles.BG_GRAY_200,
                Styles.RELATIVE,
                Styles.H_2);

    headerTag.with(progressIndicator);
    return headerTag;
  }

  private ContainerTag renderQuestionSummary(AnswerData data, Long applicantId) {
    ContainerTag questionPrompt =
        div(data.questionText()).withClasses(Styles.FLEX_AUTO, Styles.FONT_SEMIBOLD);
    ContainerTag questionContent =
        div(questionPrompt).withClasses(Styles.FLEX, Styles.FLEX_ROW, Styles.PR_2);

    // Show timestamp if answered elsewhere.
    if (data.isPreviousResponse()) {
      LocalDate date =
          Instant.ofEpochMilli(data.timestamp()).atZone(ZoneId.systemDefault()).toLocalDate();
      ContainerTag timestampContent =
          div("Previously answered on " + date)
              .withClasses(Styles.FLEX_AUTO, Styles.TEXT_RIGHT, Styles.FONT_LIGHT, Styles.TEXT_XS);
      questionContent.with(timestampContent);
    }

    // Add answer text, converting newlines to <br/> tags.
    ContainerTag answerContent =
        div().withClasses(Styles.FLEX_AUTO, Styles.TEXT_LEFT, Styles.FONT_LIGHT, Styles.TEXT_SM);
    String[] texts = data.answerText().split("\n");
    texts = Arrays.stream(texts).filter(text -> text.length() > 0).toArray(String[]::new);
    for (int i = 0; i < texts.length; i++) {
      if (i > 0) {
        answerContent.with(br());
      }
      answerContent.withText(texts[i]);
    }

    // Link to block containing specific question.
    String editLink =
        routes.ApplicantProgramBlocksController.review(
                applicantId, data.programId(), data.blockId())
            .url();
    ContainerTag editAction =
        new LinkElement()
            .setHref(editLink)
            .setText("Edit")
            .setStyles(Styles.ABSOLUTE, Styles.BOTTOM_0, Styles.RIGHT_0)
            .asAnchorText();
    ContainerTag editContent =
        div(editAction)
            .withClasses(
                Styles.FLEX_AUTO,
                Styles.TEXT_RIGHT,
                Styles.FONT_LIGHT,
                Styles.ITALIC,
                Styles.RELATIVE);
    ContainerTag answerDiv =
        div(answerContent, editContent).withClasses(Styles.FLEX, Styles.FLEX_ROW, Styles.PR_2);

    return div(questionContent, answerDiv)
        .withClasses(
            Styles.MY_2,
            Styles.PY_2,
            Styles.BORDER_B,
            Styles.BORDER_GRAY_300,
            ReferenceClasses.APPLICANT_SUMMARY_ROW);
  }
}
