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
import j2html.tags.Tag;
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

    Tag contentDiv =
        div()
            .withId("application-view")
            .withClasses(Styles.PX_20)
            .with(
                h2("Program: " + programName).withClasses(Styles.MY_4),
                h1(applicantNameWithApplicationId).withClasses(Styles.MY_4),
                h2(renderDownloadButton(programId, applicationId)).withClasses(Styles.TEXT_RIGHT, Styles.TEXT_RED_400),
                each(
                    blocks,
                    block -> renderApplicationBlock(programId, block, blockToAnswers.get(block))),
                renderDownloadButton(programId, applicationId));

    HtmlBundle htmlBundle = layout.getBundle().setTitle(title).addMainContent(contentDiv);
    return layout.render(htmlBundle);
  }

  private Tag renderDownloadButton(long programId, long applicationId) {
    String link =
        controllers.admin.routes.AdminApplicationController.download(programId, applicationId)
            .url();
    return new LinkElement()
        .setId("download-button")
        .setHref(link)
        .setText("Export to PDF")
        .asButton()
        // TODO: when the download link works, un-hide.

        .withClasses(Styles.BORDER, Styles.BORDER_GRAY_700, Styles.BG_WHITE, Styles.ROUNDED, Styles.P_1);
  }

  private Tag renderApplicationBlock(long programId, Block block, Collection<AnswerData> answers) {
    Tag topContent =
        div()
            .withClasses(Styles.FLEX)
            .with(
                div(
                    div(block.getName())
                        .withClasses(
                            Styles.TEXT_BLACK, Styles.FONT_BOLD, Styles.TEXT_XL, Styles.MB_2)))
            .with(p().withClasses(Styles.FLEX_GROW))
            .with(p(block.getDescription()).withClasses(Styles.TEXT_GRAY_700, Styles.ITALIC));

    Tag mainContent =
        div()
            .withClasses(Styles.W_FULL)
            .with(each(answers, answer -> renderAnswer(programId, answer)));

    Tag innerDiv =
        div(topContent, mainContent)
            .withClasses(
                Styles.BORDER, Styles.BORDER_GRAY_300, Styles.BG_WHITE, Styles.ROUNDED, Styles.P_4);

    return div(innerDiv)
        .withClasses(
            ReferenceClasses.ADMIN_APPLICATION_BLOCK_CARD,
            Styles.W_FULL,
            Styles.SHADOW_LG,
            Styles.MB_4);
  }

  private Tag renderAnswer(long programId, AnswerData answerData) {
    LocalDate date =
        Instant.ofEpochMilli(answerData.timestamp()).atZone(ZoneId.systemDefault()).toLocalDate();
    Tag answerContent;
    if (answerData.fileKey().isPresent()) {
      String encodedFileKey = URLEncoder.encode(answerData.fileKey().get(), StandardCharsets.UTF_8);
      String fileLink =
          controllers.routes.FileController.adminShow(programId, encodedFileKey).url();
      answerContent = a(answerData.answerText()).withHref(fileLink);
    } else {
      answerContent = div(answerData.answerText());
    }
    return div()
        .withClasses(Styles.FLEX)
        .with(
            div()
                .withClasses(Styles.MB_8)
                .with(
                    div(answerData.questionDefinition().getName())
                        .withClasses(Styles.TEXT_GRAY_400, Styles.TEXT_BASE, Styles.LINE_CLAMP_3)))
        .with(p().withClasses(Styles.W_8))
        .with(
            answerContent.withClasses(Styles.TEXT_GRAY_700, Styles.TEXT_BASE, Styles.LINE_CLAMP_3))
        .with(p().withClasses(Styles.FLEX_GROW))
        .with(
            div("Answered on " + date)
                .withClasses(
                    Styles.FLEX_AUTO, Styles.TEXT_RIGHT, Styles.FONT_LIGHT, Styles.TEXT_XS));
  }
}
