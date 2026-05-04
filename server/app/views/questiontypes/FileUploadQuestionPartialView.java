package views.questiontypes;

import javax.inject.Inject;
import play.mvc.Http;
import play.mvc.Result;
import play.mvc.Results;
import services.applicant.ReadOnlyApplicantProgramService;
import services.applicant.question.FileUploadQuestion;
import services.question.exceptions.QuestionNotFoundException;
import views.BaseView;
import views.shared.BaseViewDeps;

public class FileUploadQuestionPartialView extends BaseView<FileUploadQuestionPartialViewModel> {
  @Inject
  public FileUploadQuestionPartialView(BaseViewDeps baseViewDeps) {
    super(baseViewDeps);
  }

  @Override
  protected String pageTemplate() {
    return "questiontypes/FileUploadQuestionPartial";
  }

  /** Renders the file-upload question partial for HTMX (success or global parse-error handling). */
  public Result renderHtmxSuccess(
      Http.Request request,
      long programId,
      String blockId,
      long questionId,
      ReadOnlyApplicantProgramService roApplicantProgramService) {
    final FileUploadQuestion stagedQuestion;
    try {
      stagedQuestion =
          roApplicantProgramService
              .getActiveBlock(blockId)
              .orElseThrow()
              .findFileUploadQuestion(questionId);
    } catch (QuestionNotFoundException e) {
      return Results.badRequest().as(Http.MimeTypes.HTML);
    }

    return Results.ok(
            render(
                request,
                FileUploadQuestionPartialViewModel.builder()
                    .fileUploadQuestion(stagedQuestion)
                    .htmxProgramId(programId)
                    .htmxBlockId(blockId)
                    .build()))
        .as(Http.MimeTypes.HTML);
  }
}
