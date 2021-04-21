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
import j2html.tags.ContainerTag;
import java.util.Arrays;
import play.twirl.api.Content;
import views.BaseHtmlView;

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
  public Content render(ImmutableList<String> questions, ImmutableList<String> answers) {
    ContainerTag headerTag = renderHeader("program title", 100);

    ContainerTag content = div().withClasses("mx-16");
    String[] q = questions.toArray(new String[] {});
    String[] a = answers.toArray(new String[] {});
    for (int i = 0; i < q.length; i++) {
      content.with(renderQuestionSummary(q[i], a[i], "Edit"));
    }

    // TODO: [NOW] Add submit action.    
    ContainerTag actions = div(button("Submit")).withClasses("float-right my-4");
    content.with(actions);

    // TODO: [NOW] Add Program title.
    return layout.render(headerTag, h1("Application Review:").withClasses("px-16 py-4"), content);
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

  private ContainerTag renderQuestionSummary(
      String questionText, String answerText, String editLink) {
    ContainerTag questionContent = div(questionText).withClasses("font-semibold text-base");

    // Add answer text, converting newlines to <br/> tags.
    ContainerTag answerContent = div().withClasses("flex-auto text-left font-light text-sm");
    String[] texts = answerText.split("\n");
    texts = Arrays.stream(texts).filter(text -> text.length() > 0).toArray(String[]::new);
    for (int i = 0; i < texts.length; i++) {
      if (i > 0) {
        answerContent.with(br());
      }
      answerContent.withText(texts[i]);
    }

    // TODO: [NOW] This should be a link to block containing specific answer.
    ContainerTag editContent = div(
      span(editLink).withClasses("absolute bottom-0 right-0 text-sm")
    ).withClasses("flex-auto text-right font-light italic relative");
    ContainerTag answerDiv = div(answerContent, editContent).withClasses("flex flex-row pr-2");
    return div(questionContent, answerDiv).withClasses("w-7/8 my-2 py-2 border-b border-gray-300");
  }
}
