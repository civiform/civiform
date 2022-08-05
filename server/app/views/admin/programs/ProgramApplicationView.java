package views.admin.programs;

import static com.google.common.base.Preconditions.checkNotNull;
import static j2html.TagCreator.a;
import static j2html.TagCreator.div;
import static j2html.TagCreator.each;
import static j2html.TagCreator.h2;
import static j2html.TagCreator.option;
import static j2html.TagCreator.p;
import static j2html.TagCreator.select;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ListMultimap;
import com.google.inject.Inject;
import j2html.tags.specialized.ATag;
import j2html.tags.specialized.DivTag;
import j2html.tags.specialized.OptionTag;
import j2html.tags.specialized.SelectTag;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Collection;
import play.twirl.api.Content;
import services.applicant.AnswerData;
import services.applicant.Block;
import services.program.StatusDefinitions;
import views.BaseHtmlLayout;
import views.BaseHtmlView;
import views.HtmlBundle;
import views.components.LinkElement;
import views.style.BaseStyles;
import views.style.ReferenceClasses;
import views.style.StyleUtils;
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
      ImmutableList<AnswerData> answers,
      StatusDefinitions statusDefinitions) {
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
            .withClasses(Styles.PX_20)
            .with(
                h2("Program: " + programName).withClasses(Styles.MY_4),
                div()
                    .withClasses(Styles.FLEX)
                    .with(
                        p(applicantNameWithApplicationId)
                            .withClasses(
                                Styles.MY_4, Styles.TEXT_BLACK, Styles.TEXT_2XL, Styles.MB_2),
                        // Spread out the items, so the following are right
                        // aligned.
                        p().withClasses(Styles.FLEX_GROW))
                    // Status options if configured on the program.
                    .condWith(
                        !statusDefinitions.getStatuses().isEmpty(),
                        div()
                            .withClasses(Styles.FLEX)
                            .with(
                                div("Status:")
                                    .withClasses(
                                        Styles.SELF_CENTER,
                                        ReferenceClasses.PROGRAM_ADMIN_STATUS_SELECTOR_LABEL),
                                renderStatusOptionsSelector(statusDefinitions)))
                    .with(renderDownloadButton(programId, applicationId)))
            .with(
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
            .withClasses(Styles.FLEX)
            .with(
                div(
                    div(block.getName())
                        .withClasses(
                            Styles.TEXT_BLACK, Styles.FONT_BOLD, Styles.TEXT_XL, Styles.MB_2)))
            .with(p().withClasses(Styles.FLEX_GROW))
            .with(p(block.getDescription()).withClasses(Styles.TEXT_GRAY_700, Styles.ITALIC));

    DivTag mainContent =
        div()
            .withClasses(Styles.W_FULL)
            .with(each(answers, answer -> renderAnswer(programId, answer)));

    DivTag innerDiv =
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

  private static SelectTag renderStatusOptionsSelector(StatusDefinitions statusDefinitions) {
    SelectTag dropdownTag =
        select()
            .withClasses(
                Styles.OUTLINE_NONE,
                Styles.PX_3,
                Styles.PY_1,
                Styles.MX_3,
                Styles.MY_4,
                Styles.BORDER,
                Styles.BORDER_GRAY_500,
                Styles.ROUNDED_FULL,
                Styles.BG_WHITE,
                Styles.TEXT_XS,
                StyleUtils.focus(BaseStyles.BORDER_SEATTLE_BLUE));

    // Add statuses in the order they're provided.
    statusDefinitions
        .getStatuses()
        .forEach(
            status -> {
              String value = status.statusText();
              OptionTag optionTag = option(value).withValue(value);
              dropdownTag.with(optionTag);
            });
    return dropdownTag;
  }
}
