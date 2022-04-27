package controllers.admin;

import static com.google.common.base.Preconditions.checkNotNull;

import auth.Authorizers;
import controllers.CiviFormController;
import forms.BlockForm;
import java.util.Optional;
import javax.inject.Inject;
import org.pac4j.play.java.Secure;
import play.data.DynamicForm;
import play.data.Form;
import play.data.FormFactory;
import play.mvc.Http.Request;
import play.mvc.Result;
import services.CiviFormError;
import services.ErrorAnd;
import services.program.BlockDefinition;
import services.program.IllegalPredicateOrderingException;
import services.program.ProgramBlockAdditionResult;
import services.program.ProgramBlockDefinitionNotFoundException;
import services.program.ProgramDefinition;
import services.program.ProgramDefinition.Direction;
import services.program.ProgramNeedsABlockException;
import services.program.ProgramNotFoundException;
import services.program.ProgramService;
import services.question.QuestionService;
import services.question.ReadOnlyQuestionService;
import views.admin.programs.ProgramBlockEditView;

/** Controller for admins editing screens (blocks) of a program. */
public class AdminProgramBlocksController extends CiviFormController {

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

  /**
   * Return a HTML page displaying all configurations of the specified program.
   *
   * <p>By default, the last program screen (block) is shown. Admins can navigate to other screens
   * (blocks) if applicable through links on the page.
   */
  @Secure(authorizers = Authorizers.Labels.CIVIFORM_ADMIN)
  public Result index(long programId) {
    try {
      ProgramDefinition program = programService.getProgramDefinition(programId);
      long blockId = program.getLastBlockDefinition().id();
      return redirect(routes.AdminProgramBlocksController.edit(programId, blockId));
    } catch (ProgramNotFoundException | ProgramNeedsABlockException e) {
      return notFound(e.toString());
    }
  }

  /** POST endpoint for creating a new screen (block) for the program. */
  @Secure(authorizers = Authorizers.Labels.CIVIFORM_ADMIN)
  public Result create(Request request, long programId) {
    Optional<Long> enumeratorId =
        Optional.ofNullable(
                formFactory.form().bindFromRequest(request).get(editView.ENUMERATOR_ID_FORM_FIELD))
            .map(Long::valueOf);
    try {
      ErrorAnd<ProgramBlockAdditionResult, CiviFormError> result;
      if (enumeratorId.isPresent()) {
        result = programService.addRepeatedBlockToProgram(programId, enumeratorId.get());
      } else {
        result = programService.addBlockToProgram(programId);
      }
      ProgramDefinition program = result.getResult().program();
      BlockDefinition block =
          result.getResult().maybeAddedBlock().isEmpty()
              ? program.getLastBlockDefinition()
              : result.getResult().maybeAddedBlock().get();
      if (result.isError()) {
        String errorMessage = joinErrors(result.getErrors());
        return renderEditViewWithMessage(request, program, block, errorMessage);
      }
      return redirect(routes.AdminProgramBlocksController.edit(programId, block.id()).url());
    } catch (ProgramNotFoundException | ProgramNeedsABlockException e) {
      return notFound(e.toString());
    } catch (ProgramBlockDefinitionNotFoundException e) {
      throw new RuntimeException(
          "Something happened to the enumerator block while creating a repeated block", e);
    }
  }

  /**
   * Return a HTML page displaying all configurations of the specified program screen (block) and
   * forms to update them.
   */
  @Secure(authorizers = Authorizers.Labels.CIVIFORM_ADMIN)
  public Result edit(Request request, long programId, long blockId) {
    try {
      ProgramDefinition program = programService.getProgramDefinition(programId);
      BlockDefinition block = program.getBlockDefinition(blockId);
      return renderEditViewWithMessage(request, program, block, "");
    } catch (ProgramNotFoundException | ProgramBlockDefinitionNotFoundException e) {
      return notFound(e.toString());
    }
  }

  /** POST endpoint for updating a screen (block) for the program. */
  @Secure(authorizers = Authorizers.Labels.CIVIFORM_ADMIN)
  public Result update(Request request, long programId, long blockId) {
    Form<BlockForm> blockFormWrapper = formFactory.form(BlockForm.class);
    BlockForm blockForm = blockFormWrapper.bindFromRequest(request).get();

    try {
      ErrorAnd<ProgramDefinition, CiviFormError> result =
          programService.updateBlock(programId, blockId, blockForm);
      if (result.isError()) {
        String errorMessage = joinErrors(result.getErrors());
        return renderEditViewWithMessage(
            request, result.getResult(), blockId, blockForm, errorMessage);
      }
    } catch (ProgramNotFoundException | ProgramBlockDefinitionNotFoundException e) {
      return notFound(e.toString());
    }

    return redirect(routes.AdminProgramBlocksController.edit(programId, blockId));
  }

  /** POST endpoint for moving a screen (block) for the program. */
  @Secure(authorizers = Authorizers.Labels.CIVIFORM_ADMIN)
  public Result move(Request request, long programId, long blockId) {
    DynamicForm requestData = formFactory.form().bindFromRequest(request);
    Direction direction = Direction.valueOf(requestData.get("direction"));
    try {
      programService.moveBlock(programId, blockId, direction);
    } catch (IllegalPredicateOrderingException e) {
      return redirect(routes.AdminProgramBlocksController.edit(programId, blockId))
          .flashing("error", e.getLocalizedMessage());
    } catch (ProgramNotFoundException e) {
      return notFound(e.toString());
    }
    return redirect(routes.AdminProgramBlocksController.edit(programId, blockId));
  }

  /** POST endpoint for deleting a screen (block) for the program. */
  @Secure(authorizers = Authorizers.Labels.CIVIFORM_ADMIN)
  public Result destroy(long programId, long blockId) {
    try {
      programService.deleteBlock(programId, blockId);
    } catch (IllegalPredicateOrderingException e) {
      return redirect(routes.AdminProgramBlocksController.edit(programId, blockId))
          .flashing("error", e.getLocalizedMessage());
    } catch (ProgramNotFoundException | ProgramNeedsABlockException e) {
      return notFound(e.toString());
    }
    return redirect(routes.AdminProgramBlocksController.index(programId));
  }

  private Result renderEditViewWithMessage(
      Request request, ProgramDefinition program, BlockDefinition block, String message) {
    ReadOnlyQuestionService roQuestionService =
        questionService.getReadOnlyQuestionService().toCompletableFuture().join();

    return ok(
        editView.render(
            request, program, block, message, roQuestionService.getUpToDateQuestions()));
  }

  private Result renderEditViewWithMessage(
      Request request,
      ProgramDefinition program,
      long blockId,
      BlockForm blockForm,
      String message) {
    try {
      BlockDefinition blockDefinition = program.getBlockDefinition(blockId);
      ReadOnlyQuestionService roQuestionService =
          questionService.getReadOnlyQuestionService().toCompletableFuture().join();

      return ok(
          editView.render(
              request,
              program,
              blockId,
              blockForm,
              blockDefinition,
              blockDefinition.programQuestionDefinitions(),
              message,
              roQuestionService.getUpToDateQuestions()));
    } catch (ProgramBlockDefinitionNotFoundException e) {
      return notFound(e.toString());
    }
  }
}
