package controllers.admin;

import static com.google.common.base.Preconditions.checkNotNull;
import static play.mvc.Results.notFound;
import static play.mvc.Results.ok;

import auth.Authorizers;
import javax.inject.Inject;
import org.pac4j.play.java.Secure;
import play.mvc.Http.Request;
import play.mvc.Result;
import services.program.BlockDefinition;
import services.program.ProgramBlockDefinitionNotFoundException;
import services.program.ProgramDefinition;
import services.program.ProgramNotFoundException;
import services.program.ProgramService;
import views.admin.programs.ProgramBlockPredicatesEditView;

public class AdminProgramBlockPredicatesController {
  private final ProgramService programService;
  private final ProgramBlockPredicatesEditView predicatesEditView;

  @Inject
  public AdminProgramBlockPredicatesController(
      ProgramService programService, ProgramBlockPredicatesEditView predicatesEditView) {
    this.programService = checkNotNull(programService);
    this.predicatesEditView = checkNotNull(predicatesEditView);
  }

  @Secure(authorizers = Authorizers.Labels.UAT_ADMIN)
  public Result edit(Request request, long programId, long blockDefinitionId) {
    try {
      ProgramDefinition programDefinition = programService.getProgramDefinition(programId);
      BlockDefinition blockDefinition = programDefinition.getBlockDefinition(blockDefinitionId);
      return ok(
          predicatesEditView.render(
              request,
              programDefinition,
              blockDefinition,
              programDefinition.getAvailablePredicateQuestionDefinitions(blockDefinitionId)));
    } catch (ProgramNotFoundException e) {
      return notFound(String.format("Program ID %d not found.", programId));
    } catch (ProgramBlockDefinitionNotFoundException e) {
      return notFound(
          String.format("Block ID %d not found for Program %d", blockDefinitionId, programId));
    }
  }

  @Secure(authorizers = Authorizers.Labels.UAT_ADMIN)
  public Result update(Request request, long programId, long blockDefinitionId) {
    // 1. Create the PredicateDefinition using request (and a VisibilityPredicateForm)
    // 2. Validate that it is well-formed.
    // 3. programService.setBlockPredicate(programId, blockDefinitionId, predicate);
    //    catch ProgramNotFoundException | ProgramBlockDefinitionNotFoundException.
    // 4. Redirect back to the predicate edit page? or back to the block edit page?

    return ok("POST");
  }
}
