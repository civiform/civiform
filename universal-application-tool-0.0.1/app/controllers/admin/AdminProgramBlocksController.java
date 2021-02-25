package controllers.admin;

import play.data.FormFactory;
import play.mvc.Controller;
import play.mvc.Http.Request;
import play.mvc.Result;
import services.program.ProgramDefinition;
import services.program.ProgramService;
import views.admin.ProgramBlockEditView;

import javax.inject.Inject;

import java.util.Optional;

import static com.google.common.base.Preconditions.checkNotNull;

public class AdminProgramBlocksController extends Controller {

    private final ProgramService service;
    // private final ProgramBlockEditView blockEditView;
    // private final FormFactory formFactory;

    @Inject
    public AdminProgramBlocksController(
            ProgramService service,
            ProgramBlockEditView blockEditView,
            FormFactory formFactory) {
        this.service = checkNotNull(service);
        // this.blockEditView = checkNotNull(blockEditView);
        // this.formFactory = checkNotNull(formFactory);
    }

    public Result index(long programId) {
        Optional<ProgramDefinition> programMaybe = service.getProgramDefinition(programId);
        if (programMaybe.isEmpty()) {
            return notFound(String.format("Program ID %d not found.", programId));
        }

        ProgramDefinition program = programMaybe.get();
        if (program.blockDefinitions().size() > 0) {
            // routes.AdminProgramBlocksController.edit(pid, program.blockdefinitions.get(0).id())
            return ok();
        }

        return redirect(routes.AdminProgramBlocksController.newOne(programId));
    }

    public Result newOne(Request request, long programId) {
        return ok();
    }
}
