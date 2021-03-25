package controllers.admin;

import static com.google.common.base.Preconditions.checkNotNull;

import auth.Authorizers;
import forms.BlockForm;
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
    try {
      ProgramDefinition program = programService.getProgramDefinition(programId);
      long blockId = program.getLastBlockDefinition().id();
      return redirect(routes.AdminProgramBlocksController.edit(programId, blockId));
    } catch (ProgramNotFoundException | ProgramNeedsABlockException e) {
      return notFound(e.toString());
    }
  }

  @Secure(authorizers = Authorizers.Labels.UAT_ADMIN)
  public Result create(long programId) {
    try {
      ProgramDefinition program = programService.addBlockToProgram(programId);
      long blockId = program.getLastBlockDefinition().id();
      return redirect(routes.AdminProgramBlocksController.edit(programId, blockId).url());
    } catch (ProgramNotFoundException | ProgramNeedsABlockException e) {
      return notFound(e.toString());
    }
  }

  @Secure(authorizers = Authorizers.Labels.UAT_ADMIN)
  public Result edit(Request request, long programId, long blockId) {
    try {
      ProgramDefinition program = programService.getProgramDefinition(programId);
      BlockDefinition block = program.getBlockDefinition(blockId);

      ReadOnlyQuestionService roQuestionService =
          questionService.getReadOnlyQuestionService().toCompletableFuture().join();

      return ok(editView.render(request, program, block, roQuestionService.getAllQuestions()));
    } catch (ProgramNotFoundException | ProgramBlockNotFoundException e) {
      return notFound(e.toString());
    }
  }

  @Secure(authorizers = Authorizers.Labels.UAT_ADMIN)
  public Result update(Request request, long programId, long blockId) {
    Form<BlockForm> blockFormWrapper = formFactory.form(BlockForm.class);
    BlockForm blockForm = blockFormWrapper.bindFromRequest(request).get();

    try {
      programService.updateBlock(programId, blockId, blockForm);
    } catch (ProgramNotFoundException | ProgramBlockNotFoundException e) {
      return notFound(e.toString());
    }

    return redirect(routes.AdminProgramBlocksController.edit(programId, blockId));
  }

  @Secure(authorizers = Authorizers.Labels.UAT_ADMIN)
  public Result destroy(long programId, long blockId) {
    try {
      programService.deleteBlock(programId, blockId);
    } catch (ProgramNotFoundException | ProgramNeedsABlockException e) {
      return notFound(e.toString());
    }
    return redirect(routes.AdminProgramBlocksController.index(programId));
  }
}
