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
import views.admin.ProgramBlockEditView;

public class AdminProgramBlocksController extends Controller {

  private final ProgramService service;
  private final ProgramBlockEditView editView;
  // private final FormFactory formFactory;

  @Inject
  public AdminProgramBlocksController(
      ProgramService service, ProgramBlockEditView editView, FormFactory formFactory) {
    this.service = checkNotNull(service);
    this.editView = checkNotNull(editView);
    // this.formFactory = checkNotNull(formFactory);
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

    long blockId = program.blockDefinitions().get(program.blockDefinitions().size() - 1).id();
    return redirect(routes.AdminProgramBlocksController.edit(programId, blockId));
  }

  public Result create(long programId) {
    Optional<ProgramDefinition> programMaybe = service.getProgramDefinition(programId);
    if (programMaybe.isEmpty()) {
      return notFound(String.format("Program ID %d not found.", programId));
    }

    ProgramDefinition program = programMaybe.get();
    try {
      program = service.addBlockToProgram(programId, "", "");
    } catch (ProgramNotFoundException e) {
      // This really shouldn't happen
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
