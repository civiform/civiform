package views;

import static com.google.common.base.Preconditions.checkNotNull;
import static j2html.TagCreator.each;
import static j2html.TagCreator.form;
import static j2html.TagCreator.h1;
import static j2html.TagCreator.p;

import com.google.inject.Inject;
import j2html.tags.Tag;
import play.mvc.Http.HttpVerbs;
import play.mvc.Http.Request;
import play.twirl.api.Content;
import services.applicant.ApplicantQuestion;
import services.applicant.Block;
import views.questiontypes.ApplicantQuestionRendererFactory;

public final class ApplicantProgramBlockEditView extends BaseHtmlView {

  private final ApplicantLayout layout;
  private final ApplicantQuestionRendererFactory applicantQuestionRendererFactory;

  @Inject
  public ApplicantProgramBlockEditView(ApplicantLayout layout,
      ApplicantQuestionRendererFactory applicantQuestionRendererFactory) {
    this.layout = checkNotNull(layout);
    this.applicantQuestionRendererFactory = checkNotNull(applicantQuestionRendererFactory);
  }

  public Content render(Request request, long applicantId, long programId, Block block) {
    String formAction = controllers.routes.ApplicantProgramBlocksController
        .update(applicantId, programId, block.getId()).url();

    return layout.render(
        h1(block.getName()),
        p(block.getDescription()),
        form()
            .withAction(formAction)
            .withMethod(HttpVerbs.POST)
            .with(makeCsrfTokenInputTag(request))
            .with(each(block.getQuestions(), this::renderQuestion))
            .with(submitButton("Save and continue"))
    );
  }

  private Tag renderQuestion(ApplicantQuestion question) {
    return applicantQuestionRendererFactory.getRenderer(question).render();
  }
}
