package views.admin.programs;

import static j2html.TagCreator.div;
import static j2html.TagCreator.each;
import static j2html.TagCreator.h1;
import static j2html.TagCreator.p;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ListMultimap;
import com.google.inject.Inject;
import j2html.tags.Tag;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Collection;
import play.twirl.api.Content;
import services.applicant.AnswerData;
import services.applicant.Block;
import services.applicant.question.ApplicantQuestion;
import services.program.ProgramNotFoundException;
import services.question.types.QuestionDefinition;
import views.HtmlBundle;
import views.admin.AdminLayout;
import views.admin.AdminView;
import views.components.LinkElement;
import views.style.ReferenceClasses;
import views.style.Styles;

public final class ProgramApplicationView extends AdminView {
  private final AdminLayout layout;

  @Inject
  public ProgramApplicationView(AdminLayout layout) {
    this.layout = checkNotNull(layout);
  }

  public Content render(
      long programId,
      long applicationId,
      String applicantName,
      ImmutableList<Block> blocks,
      ImmutableList<AnswerData> answers) {
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
            .withClasses(Styles.PX_20)
            .with(
                h1(applicantName).withClasses(Styles.MY_4),
                each(blocks, block -> renderApplicationBlock(block, blockToAnswers.get(block))),
                renderDownloadButton(programId, applicationId));

    HtmlBundle htmlBundle =
        getHtmlBundle().setTitle("Program Application View").addMainContent(contentDiv);
    return layout.renderCentered(htmlBundle);
  }

  private Tag renderDownloadButton(long programId, long applicationId) {
    String link =
        controllers.admin.routes.AdminApplicationController.download(programId, applicationId)
            .url();
    return new LinkElement()
        .setId("download-button")
        .setHref(link)
        .setText("Download (PDF)")
        .asButton();
  }

  private Tag renderApplicationBlock(Block block, Collection<AnswerData> answers) {
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
        div().withClasses(Styles.W_FULL).with(each(answers, answer -> renderAnswer(answer)));

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

  private Tag renderAnswer(AnswerData answerData) {
    LocalDate date =
        Instant.ofEpochMilli(answerData.timestamp()).atZone(ZoneId.systemDefault()).toLocalDate();
    String questionIdentifier =
        String.format("Question ID: %d", answerData.questionDefinition().getId());
    return div()
        .withClasses(Styles.FLEX)
        .with(
            div()
                .withClasses(Styles.MB_8)
                .with(
                    div(questionIdentifier)
                        .withClasses(
                            Styles.TEXT_GRAY_400, Styles.TEXT_XL, Styles.MB_2, "line-clamp-3"))
                .with(
                    div(answerData.questionDefinition().getName())
                        .withClasses(Styles.TEXT_GRAY_400, Styles.TEXT_BASE, "line-clamp-3")))
        .with(p().withClasses(Styles.W_8))
        .with(
            div(answerData.answerText())
                .withClasses(Styles.TEXT_GRAY_700, Styles.TEXT_BASE, "line-clamp-3"))
        .with(p().withClasses(Styles.FLEX_GROW))
        .with(
            div("Answered on " + date)
                .withClasses(
                    Styles.FLEX_AUTO, Styles.TEXT_RIGHT, Styles.FONT_LIGHT, Styles.TEXT_XS));
  }
}
