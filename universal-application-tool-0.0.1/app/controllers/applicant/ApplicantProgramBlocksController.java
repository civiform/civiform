package controllers.applicant;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletionStage;
import javax.inject.Inject;
import play.data.DynamicForm;
import play.data.FormFactory;
import play.libs.concurrent.HttpExecutionContext;
import play.mvc.Controller;
import play.mvc.Http.Request;
import play.mvc.Result;
import services.applicant.ApplicantService;
import services.applicant.Block;
import services.applicant.ReadOnlyApplicantProgramService;
import views.applicant.ApplicantProgramBlockEditView;

public final class ApplicantProgramBlocksController extends Controller {
  private static final ImmutableSet<String> STRIPPED_FORM_FIELDS = ImmutableSet.of("csrfToken");

  private final ApplicantService applicantService;
  private final HttpExecutionContext httpExecutionContext;
  private final ApplicantProgramBlockEditView editView;
  private final FormFactory formFactory;

  @Inject
  public ApplicantProgramBlocksController(
      ApplicantService applicantService,
      HttpExecutionContext httpExecutionContext,
      ApplicantProgramBlockEditView editView,
      FormFactory formFactory) {
    this.applicantService = checkNotNull(applicantService);
    this.httpExecutionContext = checkNotNull(httpExecutionContext);
    this.editView = checkNotNull(editView);
    this.formFactory = checkNotNull(formFactory);
  }

  public CompletionStage<Result> edit(
      Request request, long applicantId, long programId, long blockId) {
    return applicantService
        .getReadOnlyApplicantProgramService(applicantId, programId)
        .thenApplyAsync(
            (roApplicantProgramService) -> {
              Optional<Block> block = roApplicantProgramService.getBlock(blockId);

              if (block.isPresent()) {
                return ok(editView.render(request, applicantId, programId, block.get()));
              } else {
                return notFound();
              }
            },
            httpExecutionContext.current());
  }

  public CompletionStage<Result> update(
      Request request, long applicantId, long programId, long blockId) {
    DynamicForm form = formFactory.form().bindFromRequest(request);
    ImmutableMap<String, String> formData = cleanForm(form.rawData());

    return applicantService
        .stageAndUpdateIfValid(applicantId, programId, blockId, formData)
        .thenApplyAsync(
            (errorAndROApplicantProgramService) -> {
              ReadOnlyApplicantProgramService roApplicantProgramService =
                  errorAndROApplicantProgramService.getResult();
              return updateResult(
                  request, applicantId, programId, blockId, roApplicantProgramService);
            },
            httpExecutionContext.current());
  }

  private Result updateResult(
      Request request,
      long applicantId,
      long programId,
      long blockId,
      ReadOnlyApplicantProgramService roApplicantProgramService) {
    Block thisBlockUpdated = roApplicantProgramService.getBlock(blockId).orElseThrow();

    // Validation errors: re-render this block with errors and previously entered data.
    if (thisBlockUpdated.hasErrors()) {
      return ok(editView.render(request, applicantId, programId, thisBlockUpdated));
    }

    // TODO: redirect to review page when it is available.
    Result reviewPageRedirect = redirect(routes.ApplicantProgramsController.index(applicantId));
    Optional<Long> nextBlockIdMaybe =
        roApplicantProgramService.getFirstIncompleteBlock().map(Block::getId);
    return nextBlockIdMaybe.isEmpty()
        ? reviewPageRedirect
        : redirect(
            routes.ApplicantProgramBlocksController.edit(
                applicantId, programId, nextBlockIdMaybe.get()));
  }

  private ImmutableMap<String, String> cleanForm(Map<String, String> formData) {
    return formData.entrySet().stream()
        .filter(entry -> !STRIPPED_FORM_FIELDS.contains(entry.getKey()))
        .collect(ImmutableMap.toImmutableMap(Map.Entry::getKey, Map.Entry::getValue));
  }
}
