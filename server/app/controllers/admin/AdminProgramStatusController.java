package controllers.admin;

import static com.google.common.base.Preconditions.checkNotNull;

import auth.Authorizers;
import controllers.CiviFormController;
import javax.inject.Inject;
import org.pac4j.play.java.Secure;
import play.mvc.Result;
import services.CiviFormError;
import services.ErrorAnd;
import services.program.ProgramBlockDefinitionNotFoundException;
import services.program.ProgramDefinition;
import services.program.ProgramNeedsABlockException;
import services.program.ProgramNotFoundException;
import services.program.ProgramService;

/** Controller for handling admin management of program application statuses. */
public class AdminProgramStatusController extends CiviFormController {

  private final ProgramService programService;
  private final RequestChecker requestChecker;

  @Inject
  public AdminProgramStatusController(ProgramService programService,
    RequestChecker requestChecker) {
    this.programService = checkNotNull(programService);
    this.requestChecker = checkNotNull(requestChecker);
  }

  @Secure(authorizers = Authorizers.Labels.CIVIFORM_ADMIN)
  public Result update(long programId) {
    requestChecker.throwIfProgramNotDraft(programId);

    try {
      // TODO figure out api for receving data and passing it on here.
      ErrorAnd<ProgramDefinition, CiviFormError> result =
          programService.updateStatuses(programId );
      if (result.isError()) {
        String errorMessage = joinErrors(result.getErrors());
        // TODO render the edit screen instead.
        return notFound(errorMessage);
      }
    } catch (ProgramNotFoundException e) {
      return notFound(e.toString());
    }

    return redirect(routes.AdminProgramBlocksController.edit(programId,0));
  }
}
