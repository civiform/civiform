package views.admin.programs;

import static j2html.TagCreator.div;
import static j2html.TagCreator.each;
import static j2html.TagCreator.h1;
import static j2html.TagCreator.p;

import com.google.common.collect.ImmutableList;
import com.google.inject.Inject;
import j2html.tags.Tag;
import models.Application;
import play.twirl.api.Content;
import services.Path;
import services.applicant.ApplicantData;
import services.applicant.ApplicantService;
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
  private final ApplicantService applicantService;

  @Inject
  public ProgramApplicationView(AdminLayout layout, ApplicantService applicantService) {
    this.layout = layout;
    this.applicantService = applicantService;
  }

  public Content render(long programId, Application application) throws ProgramNotFoundException {
    ImmutableList<Block> allBlocks =
        applicantService
            .getReadOnlyApplicantProgramService(application.getApplicant().id, programId)
            .toCompletableFuture()
            .join()
            .getAllBlocks();
    Tag contentDiv =
        div()
            .withClasses(Styles.PX_20)
            .with(
                h1(application.getApplicantData().getApplicantName()).withClasses(Styles.MY_4),
                each(
                    allBlocks,
                    block -> this.renderApplicationBlock(application.getApplicantData(), block)),
                renderDownloadButton(programId, application.id));

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

  private Tag renderApplicationBlock(ApplicantData applicantData, Block block) {
    Tag topContent =
        div(
                div(
                    div(block.getName())
                        .withClasses(
                            Styles.TEXT_BLACK, Styles.FONT_BOLD, Styles.TEXT_XL, Styles.MB_2)),
                p().withClasses(Styles.FLEX_GROW),
                p(block.getDescription()).withClasses(Styles.TEXT_GRAY_700, Styles.ITALIC))
            .withClasses(Styles.FLEX);

    Tag mainContent =
        div()
            .withClasses(Styles.W_FULL)
            .with(
                each(
                    block.getQuestions(),
                    question -> this.renderQuestion(applicantData, question)));

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

  private Tag renderQuestion(ApplicantData applicantData, ApplicantQuestion question) {
    QuestionDefinition questionDefinition = question.getQuestionDefinition();
    return div(div()
            .withClasses(Styles.FLEX)
            .with(
                div(
                        div(String.format("Question %d", questionDefinition.getId()))
                            .withClasses(
                                Styles.TEXT_GRAY_400, Styles.TEXT_XL, Styles.MB_2, "line-clamp-3"),
                        div(questionDefinition.getName())
                            .withClasses(Styles.TEXT_GRAY_400, Styles.TEXT_BASE, "line-clamp-3"))
                    .withClasses(Styles.MB_8),
                p().withClasses(Styles.W_8),
                div(
                    each(
                        question.getContextualizedScalars().entrySet(),
                        entry -> this.renderScalar(entry.getKey(), applicantData))),
                p().withClasses(Styles.FLEX_GROW)))
        .withClasses(Styles.MB_8);
  }

  private Tag renderScalar(Path key, ApplicantData applicantData) {
    return div(applicantData.readAsString(key).orElse("<unanswered>"))
        .withClasses(Styles.TEXT_GRAY_700, Styles.TEXT_BASE, "line-clamp-3");
  }
}
