package views.admin.programs;

import static j2html.TagCreator.body;
import static j2html.TagCreator.div;
import static j2html.TagCreator.each;
import static j2html.TagCreator.h1;
import static j2html.TagCreator.head;
import static j2html.TagCreator.p;

import com.google.inject.Inject;
import j2html.tags.Tag;
import models.Application;
import play.twirl.api.Content;
import services.Path;
import services.applicant.ApplicantService;
import services.program.BlockDefinition;
import services.program.ProgramNotFoundException;
import services.program.ProgramQuestionDefinition;
import services.program.ProgramService;
import services.question.types.QuestionDefinition;
import views.BaseHtmlView;
import views.admin.AdminLayout;
import views.components.LinkElement;
import views.style.Styles;

public final class ProgramApplicationView extends BaseHtmlView {
  private final AdminLayout layout;
  private final ProgramService service;
  private final ApplicantService applicantService;

  @Inject
  public ProgramApplicationView(
      AdminLayout layout, ProgramService service, ApplicantService applicantService) {
    this.layout = layout;
    this.service = service;
    this.applicantService = applicantService;
  }

  public Content render(long programId, Application application) throws ProgramNotFoundException {
    Tag contentDiv =
        div()
            .withClasses(Styles.PX_20)
            .with(
                h1(applicantService.applicantName(application)).withClasses(Styles.MY_4),
                each(
                    service.getProgramDefinition(programId).blockDefinitions(),
                    block -> this.renderApplicationBlock(application, block)),
                renderDownloadButton(programId, application.id));

    return layout.render(head(layout.tailwindStyles()), body(contentDiv));
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

  private Tag renderApplicationBlock(Application application, BlockDefinition block) {
    Tag topContent =
        div(
                div(
                    div(block.name())
                        .withClasses(
                            Styles.TEXT_BLACK, Styles.FONT_BOLD, Styles.TEXT_XL, Styles.MB_2)),
                p().withClasses(Styles.FLEX_GROW),
                p(block.description()).withClasses(Styles.TEXT_GRAY_700, Styles.ITALIC))
            .withClasses(Styles.FLEX);

    Tag mainContent =
        div()
            .withClasses(Styles.W_FULL)
            .with(
                each(
                    block.programQuestionDefinitions(),
                    question -> this.renderQuestion(application, question)));

    Tag innerDiv =
        div(topContent, mainContent)
            .withClasses(
                Styles.BORDER, Styles.BORDER_GRAY_300, Styles.BG_WHITE, Styles.ROUNDED, Styles.P_4);

    return div(innerDiv).withClasses(Styles.W_FULL, Styles.SHADOW_LG, Styles.MB_4);
  }

  private Tag renderQuestion(Application application, ProgramQuestionDefinition question) {
    QuestionDefinition definition = question.getQuestionDefinition();
    return div(div()
            .withClasses(Styles.FLEX)
            .with(
                div(
                        div(String.format("Question %d", definition.getId()))
                            .withClasses(
                                Styles.TEXT_GRAY_400, Styles.TEXT_XL, Styles.MB_2, "line-clamp-3"),
                        div(definition.getName())
                            .withClasses(Styles.TEXT_GRAY_400, Styles.TEXT_BASE, "line-clamp-3"))
                    .withClasses(Styles.MB_8),
                p().withClasses(Styles.W_8),
                div(
                    each(
                        definition.getScalars().entrySet(),
                        entry -> this.renderScalar(entry.getKey(), application))),
                p().withClasses(Styles.FLEX_GROW)))
        .withClasses(Styles.MB_8);
  }

  private Tag renderScalar(Path key, Application application) {
    return div(application.getApplicantData().readAsString(key).orElse("<unanswered>"))
        .withClasses(Styles.TEXT_GRAY_700, Styles.TEXT_BASE, "line-clamp-3");
  }
}
