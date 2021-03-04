package controllers.admin;

import static com.google.common.base.Preconditions.checkNotNull;

import auth.Authorizers;
import forms.BlockForm;
import java.util.Optional;
import javax.inject.Inject;
import org.pac4j.play.java.Secure;
import play.data.Form;
import play.data.FormFactory;
import play.mvc.Controller;
import play.mvc.Http.Request;
import play.mvc.Result;
import services.program.BlockDefinition;
import services.program.ProgramBlockNotFoundException;
import services.program.ProgramDefinition;
import services.program.ProgramNeedsABlockException;
import services.program.ProgramNotFoundException;
import services.program.ProgramService;
import services.question.QuestionService;
import services.question.ReadOnlyQuestionService;
import views.admin.programs.ProgramBlockEditView;

public class AdminProgramBlocksController extends Controller {

  private final ProgramService programService;
  private final ProgramBlockEditView editView;
  private final QuestionService questionService;
  private final FormFactory formFactory;

  @Inject
  public AdminProgramBlocksController(
      ProgramService programService,
      QuestionService questionService,
      ProgramBlockEditView editView,
      FormFactory formFactory) {
    this.programService = checkNotNull(programService);
    this.questionService = checkNotNull(questionService);
    this.editView = checkNotNull(editView);
    this.formFactory = checkNotNull(formFactory);
  }

  @Secure(authorizers = Authorizers.Labels.UAT_ADMIN)
  public Result index(long programId) {
    Optional<ProgramDefinition> programMaybe = programService.getProgramDefinition(programId);

    if (programMaybe.isEmpty()) {
      return notFound(String.format("Program ID %d not found.", programId));
    }

    ProgramDefinition program = programMaybe.get();

    return redirect(
        routes.AdminProgramBlocksController.edit(
            programId, program.blockDefinitions().get(program.blockDefinitions().size() - 1).id()));
  }

  @Secure(authorizers = Authorizers.Labels.UAT_ADMIN)
  public Result create(long programId) {
    ProgramDefinition program;

    try {
      program = programService.addBlockToProgram(programId);
    } catch (ProgramNotFoundException e) {
      // This really shouldn't happen because the first if check should catch it
      return notFound(e.toString());
    }

    long blockId = program.blockDefinitions().get(program.blockDefinitions().size() - 1).id();

    return redirect(routes.AdminProgramBlocksController.edit(programId, blockId).url());
  }

  @Secure(authorizers = Authorizers.Labels.UAT_ADMIN)
  public Result edit(Request request, long programId, long blockId) {
    Optional<ProgramDefinition> programMaybe = programService.getProgramDefinition(programId);

    if (programMaybe.isEmpty()) {
      return notFound(String.format("Program ID %d not found.", programId));
    }

    ProgramDefinition program = programMaybe.get();
    Optional<BlockDefinition> blockMaybe = program.getBlockDefinition(blockId);

    if (blockMaybe.isEmpty()) {
      return notFound(String.format("Block ID %d not found for Program %d", blockId, programId));
    }

    BlockDefinition block = blockMaybe.get();
    ReadOnlyQuestionService roQuestionService =
        questionService.getReadOnlyQuestionService().toCompletableFuture().join();

    return ok(editView.render(request, program, block, roQuestionService.getAllQuestions()));
  }

  @Secure(authorizers = Authorizers.Labels.UAT_ADMIN)
  public Result update(Request request, long programId, long blockId) {
    Form<BlockForm> blockFormWrapper = formFactory.form(BlockForm.class);
    BlockForm blockForm = blockFormWrapper.bindFromRequest(request).get();

    try {
      programService.updateBlock(programId, blockId, blockForm);
    } catch (ProgramNotFoundException | ProgramBlockNotFoundException e) {
      return notFound(String.format("Block ID %d not found for Program %d", blockId, programId));
    }

    return redirect(routes.AdminProgramBlocksController.edit(programId, blockId));
  }

  @Secure(authorizers = Authorizers.Labels.UAT_ADMIN)
  public Result destroy(long programId, long blockId) {
    try {
      programService.deleteBlock(programId, blockId);
    } catch (ProgramNotFoundException e) {
      return notFound(String.format("Program ID %d not found.", programId));
    } catch (ProgramNeedsABlockException e) {
      return notFound(String.format("Cannot delete the last block of a program."));
    }
    return redirect(routes.AdminProgramBlocksController.index(programId));
  }
}
