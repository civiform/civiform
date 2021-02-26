package controllers.admin;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.Optional;
import javax.inject.Inject;
import play.data.FormFactory;
import play.mvc.Controller;
import play.mvc.Http.Request;
import play.mvc.Result;
import services.program.BlockDefinition;
import services.program.ProgramDefinition;
import services.program.ProgramNotFoundException;
import services.program.ProgramService;
import views.admin.programs.ProgramBlockEditView;

public class AdminProgramBlocksController extends Controller {

  private final ProgramService service;
  private final ProgramBlockEditView editView;

  @Inject
  public AdminProgramBlocksController(
      ProgramService service, ProgramBlockEditView editView, FormFactory formFactory) {
    this.service = checkNotNull(service);
    this.editView = checkNotNull(editView);
  }

  public Result index(long programId) {
    Optional<ProgramDefinition> programMaybe = service.getProgramDefinition(programId);

    if (programMaybe.isEmpty()) {
      return notFound(String.format("Program ID %d not found.", programId));
    }

    ProgramDefinition program = programMaybe.get();

    if (program.blockDefinitions().size() == 0) {
      return redirect(routes.AdminProgramBlocksController.create(programId));
    }

    return redirect(
        routes.AdminProgramBlocksController.edit(
            programId, program.blockDefinitions().get(program.blockDefinitions().size() - 1).id()));
  }

  public Result create(long programId) {
    ProgramDefinition program;

    try {
      program = service.addBlockToProgram(programId);
    } catch (ProgramNotFoundException e) {
      // This really shouldn't happen because the first if check should catch it
      return notFound(e.toString());
    }

    long blockId = program.blockDefinitions().get(program.blockDefinitions().size() - 1).id();

    return redirect(routes.AdminProgramBlocksController.edit(programId, blockId).url());
  }

  public Result edit(Request request, long programId, long blockId) {
    Optional<ProgramDefinition> programMaybe = service.getProgramDefinition(programId);

    if (programMaybe.isEmpty()) {
      return notFound(String.format("Program ID %d not found.", programId));
    }

    ProgramDefinition program = programMaybe.get();
    Optional<BlockDefinition> blockMaybe = program.getBlockDefinition(blockId);

    if (blockMaybe.isEmpty()) {
      return notFound(String.format("Block ID %d not found for Program %d", blockId, programId));
    }

    BlockDefinition block = blockMaybe.get();

    return ok(editView.render(request, program, block));
  }
}
