package views.admin.programs;

import static com.google.common.base.Preconditions.checkNotNull;
import static j2html.TagCreator.a;
import static j2html.TagCreator.div;
import static j2html.TagCreator.each;
import static j2html.TagCreator.h1;
import static j2html.TagCreator.h2;
import static j2html.TagCreator.p;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ListMultimap;
import com.google.inject.Inject;
import j2html.tags.specialized.ATag;
import j2html.tags.specialized.DivTag;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Collection;
import play.twirl.api.Content;
import services.applicant.AnswerData;
import services.applicant.Block;
import views.BaseHtmlLayout;
import views.BaseHtmlView;
import views.HtmlBundle;
import views.components.LinkElement;
import views.style.ReferenceClasses;
import views.style.Styles;

/** Renders a page for a program admin to view a single submitted application. */
public final class ProgramApplicationView extends BaseHtmlView {
  private final BaseHtmlLayout layout;

  @Inject
  public ProgramApplicationView(BaseHtmlLayout layout) {
    this.layout = checkNotNull(layout);
  }

  public Content render(
      long programId,
      String programName,
      long applicationId,
      String applicantNameWithApplicationId,
      ImmutableList<Block> blocks,
      ImmutableList<AnswerData> answers) {
    String title = "Program Application View";
    ListMultimap<Block, AnswerData> blockToAnswers = ArrayListMultimap.create();
    for (AnswerData answer : answers) {
      Block answerBlock =
          blocks.stream()
              .filter(block -> block.getId().equals(answer.blockId()))
              .findFirst()
              .orElseThrow();
      blockToAnswers.put(answerBlock, answer);
    }

    DivTag contentDiv =
        div()
            .withId("application-view")
            .withClasses("px-20")
            .with(
                h2("Program: " + programName).withClasses("my-4"),
                h1(applicantNameWithApplicationId).withClasses("my-4"),
                renderDownloadButton(programId, applicationId),
                each(
                    blocks,
                    block -> renderApplicationBlock(programId, block, blockToAnswers.get(block))));

    HtmlBundle htmlBundle = layout.getBundle().setTitle(title).addMainContent(contentDiv);
    return layout.render(htmlBundle);
  }

  private ATag renderDownloadButton(long programId, long applicationId) {
    String link =
        controllers.admin.routes.AdminApplicationController.download(programId, applicationId)
            .url();
    return new LinkElement()
        .setId("download-button")
        .setHref(link)
        .setText("Export to PDF")
        .asRightAlignedButton();
  }

  private DivTag renderApplicationBlock(
      long programId, Block block, Collection<AnswerData> answers) {
    DivTag topContent =
        div()
            .withClasses("flex")
            .with(
                div(
                    div(block.getName())
                        .withClasses(
                            "text-black", "font-bold", "text-xl", "mb-2")))
            .with(p().withClasses("flex-grow"))
            .with(p(block.getDescription()).withClasses("text-gray-700", "italic"));

    DivTag mainContent =
        div()
            .withClasses("w-full")
            .with(each(answers, answer -> renderAnswer(programId, answer)));

    DivTag innerDiv =
        div(topContent, mainContent)
            .withClasses(
                "border", "border-gray-300", "bg-white", "rounded", "p-4");

    return div(innerDiv)
        .withClasses(
            ReferenceClasses.ADMIN_APPLICATION_BLOCK_CARD,
            "w-full",
            "shadow-lg",
            "mb-4");
  }

  private DivTag renderAnswer(long programId, AnswerData answerData) {
    LocalDate date =
        Instant.ofEpochMilli(answerData.timestamp()).atZone(ZoneId.systemDefault()).toLocalDate();
    DivTag answerContent;
    if (answerData.fileKey().isPresent()) {
      String encodedFileKey = URLEncoder.encode(answerData.fileKey().get(), StandardCharsets.UTF_8);
      String fileLink =
          controllers.routes.FileController.adminShow(programId, encodedFileKey).url();
      answerContent = div(a(answerData.answerText()).withHref(fileLink));
    } else {
      answerContent = div(answerData.answerText());
    }
    return div()
        .withClasses("flex")
        .with(
            div()
                .withClasses("mb-8")
                .with(
                    div(answerData.questionDefinition().getName())
                        .withClasses("text-gray-400", "text-base", "line-clamp-3")))
        .with(p().withClasses("w-8"))
        .with(
            answerContent.withClasses("text-gray-700", "text-base", "line-clamp-3"))
        .with(p().withClasses("flex-grow"))
        .with(
            div("Answered on " + date)
                .withClasses(
                    "flex-auto", "text-right", "font-light", "text-xs"));
  }
}
