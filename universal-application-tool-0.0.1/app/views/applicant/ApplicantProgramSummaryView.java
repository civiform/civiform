package views.applicant;

import static com.google.common.base.Preconditions.checkNotNull;
import static j2html.TagCreator.br;
import static j2html.TagCreator.button;
import static j2html.TagCreator.div;
import static j2html.TagCreator.h1;
import static j2html.TagCreator.header;
import static j2html.TagCreator.span;

import com.google.common.collect.ImmutableList;
import com.google.inject.Inject;
import controllers.applicant.routes;
import java.util.Date;
import j2html.tags.ContainerTag;
import java.time.LocalDate;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Arrays;
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
   * Renders a summary of all of the applicant's data for a specific application.
   * Data includes:
   *    Program Id, Applicant Id - Needed for link context (submit & edit)
   *    Question Data for each question:
   *      - question text
   *      - answer text
   *      - block id (for edit link)
   */
  public Content render(Long applicantId, Long programId, String programTitle, ImmutableList<SummaryData> data) {
    ContainerTag headerTag = renderHeader(programTitle, 100);

    ContainerTag content = div().withClasses("mx-16");
    for (SummaryData questionData : data) {
      content.with(
        renderQuestionSummary(questionData, applicantId, programId));
    }

    // TODO: [NOW] Add submit action (POST).
    String submitLink = routes.ApplicantProgramReviewController.submit(applicantId, programId).url();
    ContainerTag actions = new LinkElement()
      .setId("submit-program-link-" + programId)
      .setHref(submitLink)
      .setText("Submit")
      .setStyles(Styles.FLOAT_RIGHT, Styles.MY_4)
      .asButton();
    content.with(actions);

    return layout.render(headerTag, h1("Application Review").withClasses("px-16 py-4"), content);
  }

  private ContainerTag renderHeader(String programTitle, int percentComplete) {
    ContainerTag headerTag = header().withClasses("flex flex-col");
    ContainerTag titleHeader =
      h1(programTitle)
        .withClasses("text-base p-0 text-right p-3 p-r-0 m-0 text-secondary overflow-ellipsis overflow-hidden whitespace-nowrap max-w-full flex-auto");
    
    ContainerTag progressInner = div()
      .withClasses("progress bg-yellow-400 transition-all duration-400 h-full block absolute left-0 top-0 w-1 rounded-r-full")
      .withStyle("width:" + percentComplete + "%");
    ContainerTag progressIndicator = div(progressInner).withId("progress-indicator")
      .withClasses("border font-semibold bg-gray-200 relative h-2.5");

    headerTag.with(titleHeader, progressIndicator);
    return headerTag;
  }

  private ContainerTag renderQuestionSummary(SummaryData data, Long applicantId, Long programId) {
    ContainerTag questionPrompt = div(data.questionText).withClasses("flex-auto font-semibold text-base");
    ContainerTag questionContent = div(questionPrompt).withClasses("flex flex-row pr-2");

    // Show timestamp if answered elsewhere.
    if (data.isPreviousResponse) {
      LocalDate date = Instant.ofEpochMilli(data.timestamp).atZone(ZoneId.systemDefault()).toLocalDate();  
      ContainerTag timestampContent = div("Previously answered on " + date).withClasses("flex-auto text-right font-light text-sm");
      questionContent.with(timestampContent);
    }
    
    // Add answer text, converting newlines to <br/> tags.
    ContainerTag answerContent = div().withClasses("flex-auto text-left font-light text-sm");
    String[] texts = data.answerText.split("\n");
    texts = Arrays.stream(texts).filter(text -> text.length() > 0).toArray(String[]::new);
    for (int i = 0; i < texts.length; i++) {
      if (i > 0) {
        answerContent.with(br());
      }
      answerContent.withText(texts[i]);
    }

    // Link to block containing specific question.
    String editLink = routes.ApplicantProgramBlocksController.review(applicantId, programId, data.blockId).url();
    ContainerTag editAction = new LinkElement()
      .setHref(editLink)
      .setText("Edit")
      .asAnchorText();      
    ContainerTag editContent = div(editAction).withClasses("flex-auto text-right font-light italic relative");
    ContainerTag answerDiv = div(answerContent, editContent).withClasses("flex flex-row pr-2");

    return div(questionContent, answerDiv).withClasses("w-7/8 my-2 py-2 border-b border-gray-300");
  }
}
