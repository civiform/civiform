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
import play.mvc.Http.HttpVerbs;
import play.mvc.Http.Request;
import play.twirl.api.Content;
import services.applicant.SummaryData;
import views.BaseHtmlView;
import views.components.LinkElement;
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
      ImmutableList<SummaryData> data) {
    ContainerTag headerTag = renderHeader(100);

    ContainerTag content = div().withClasses("mx-16");
    for (SummaryData questionData : data) {
      content.with(renderQuestionSummary(questionData, applicantId, programId));
    }

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
        headerTag,
        h1("Application review for " + programTitle).withClasses(Styles.PX_16, Styles.PY_4),
        content);
  }

  private ContainerTag renderHeader(int percentComplete) {
    ContainerTag headerTag = header().withClasses("flex flex-col -mt-12");
    ContainerTag progressInner =
        div()
            .withClasses(
                "progress bg-yellow-400 transition-all duration-400 h-full block absolute left-0"
                    + " top-0 w-1 rounded-r-full")
            .withStyle("width:" + percentComplete + "%");
    ContainerTag progressIndicator =
        div(progressInner)
            .withId("progress-indicator")
            .withClasses("border font-semibold bg-gray-200 relative h-2.5");

    headerTag.with(progressIndicator);
    return headerTag;
  }

  private ContainerTag renderQuestionSummary(SummaryData data, Long applicantId, Long programId) {
    ContainerTag questionPrompt =
        div(data.questionText).withClasses(Styles.FLEX_AUTO, Styles.FONT_SEMIBOLD);
    ContainerTag questionContent =
        div(questionPrompt).withClasses(Styles.FLEX, Styles.FLEX_ROW, Styles.PR_2);

    // Show timestamp if answered elsewhere.
    if (data.isPreviousResponse) {
      LocalDate date =
          Instant.ofEpochMilli(data.timestamp).atZone(ZoneId.systemDefault()).toLocalDate();
      ContainerTag timestampContent =
          div("Previously answered on " + date)
              .withClasses(Styles.FLEX_AUTO, Styles.TEXT_RIGHT, Styles.FONT_LIGHT, Styles.TEXT_XS);
      questionContent.with(timestampContent);
    }

    // Add answer text, converting newlines to <br/> tags.
    ContainerTag answerContent =
        div().withClasses(Styles.FLEX_AUTO, Styles.TEXT_LEFT, Styles.FONT_LIGHT, Styles.TEXT_SM);
    String[] texts = data.answerText.split("\n");
    texts = Arrays.stream(texts).filter(text -> text.length() > 0).toArray(String[]::new);
    for (int i = 0; i < texts.length; i++) {
      if (i > 0) {
        answerContent.with(br());
      }
      answerContent.withText(texts[i]);
    }

    // Link to block containing specific question.
    String editLink =
        routes.ApplicantProgramBlocksController.review(applicantId, programId, data.blockId).url();
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
        .withClasses(Styles.MY_2, Styles.PY_2, Styles.BORDER_B, Styles.BORDER_GRAY_300);
  }
}
