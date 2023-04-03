package controllers.admin;

import static com.google.common.base.Preconditions.checkNotNull;
import static views.ViewUtils.ProgramDisplayType.ACTIVE;
import static views.ViewUtils.ProgramDisplayType.DRAFT;
import static views.components.ToastMessage.ToastType.ERROR;

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
import views.admin.programs.ProgramSettingsEditView;
import views.components.ToastMessage;

/** Controller for admins editing screens (blocks) of a program. */
public final class AdminProgramBlocksController extends CiviFormController {

  private final ProgramService programService;
  private final ProgramBlockEditView editView;
  private final ProgramBlockEditView readOnlyView;
  private final QuestionService questionService;
  private final FormFactory formFactory;
  private final RequestChecker requestChecker;

  @Inject
  public AdminProgramBlocksController(
      ProgramService programService,
      QuestionService questionService,
      ProgramBlockEditView.Factory programBlockViewFactory,
      FormFactory formFactory,
      RequestChecker requestChecker) {
    this.programService = checkNotNull(programService);
    this.questionService = checkNotNull(questionService);
    this.editView = checkNotNull(programBlockViewFactory.create(DRAFT));
    this.readOnlyView = checkNotNull(programBlockViewFactory.create(ACTIVE));
    this.formFactory = checkNotNull(formFactory);
    this.requestChecker = checkNotNull(requestChecker);
  }

  /**
   * Returns an HTML page displaying all configurations of the specified program and UI elements to
   * start editing aspects of the program.
   *
   * <p>For example, it contains a button to edit the program details and a selector to choose the
   * block(screen) that the admin wants to edit. By default, the last program screen (block) is
   * shown.
   */
  @Secure(authorizers = Authorizers.Labels.CIVIFORM_ADMIN)
  public Result index(long programId) {
    return index(programId, /* readOnly= */ false);
  }

  /**
   * Returns an HTML page displaying a read only version of the program's block configuration.
   *
   * <p>By default, the last program screen (block) is shown. Admins can navigate to other screens
   * (blocks) if applicable through links on the page.
   */
  @Secure(authorizers = Authorizers.Labels.CIVIFORM_ADMIN)
  public Result readOnlyIndex(long programId) {
    return index(programId, /* readOnly=*/ true);
  }

  /**
   * Returns an HTML page that displays all configurations of a specified program either as an
   * editable or read-only version.
   *
   * <p>By default, the last program screen (block) is shown. Admins can navigate to other screens
   * (blocks) if applicable through links on the page.
   */
  private Result index(long programId, boolean readOnly) {
    try {
      ProgramDefinition program = programService.getProgramDefinition(programId);
      long blockId = program.getLastBlockDefinition().id();

      String redirectUrl =
          readOnly
              ? routes.AdminProgramBlocksController.show(programId, blockId).url()
              : routes.AdminProgramBlocksController.edit(programId, blockId).url();

      return redirect(redirectUrl);
    } catch (ProgramNotFoundException | ProgramNeedsABlockException e) {
      return notFound(e.toString());
    }
  }

  /** POST endpoint for creating a new screen (block) for the program. */
  @Secure(authorizers = Authorizers.Labels.CIVIFORM_ADMIN)
  public Result create(Request request, long programId) {
    requestChecker.throwIfProgramNotDraft(programId);

    Optional<Long> enumeratorId =
        Optional.ofNullable(
                formFactory
                    .form()
                    .bindFromRequest(request)
                    .get(ProgramBlockEditView.ENUMERATOR_ID_FORM_FIELD))
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
        ToastMessage message = new ToastMessage(joinErrors(result.getErrors()), ERROR);
        return renderEditViewWithMessage(request, program, block, Optional.of(message));
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
   * Returns an HTML page displaying all configurations of the specified program screen (block) and
   * forms to update them.
   */
  @Secure(authorizers = Authorizers.Labels.CIVIFORM_ADMIN)
  public Result edit(Request request, long programId, long blockId) {
    requestChecker.throwIfProgramNotDraft(programId);

    try {
      ProgramDefinition program = programService.getProgramDefinition(programId);
      BlockDefinition block = program.getBlockDefinition(blockId);

      Optional<ToastMessage> maybeToastMessage =
          request.flash().get("success").map(ToastMessage::success);
      return renderEditViewWithMessage(request, program, block, maybeToastMessage);
    } catch (ProgramNotFoundException | ProgramBlockDefinitionNotFoundException e) {
      return notFound(e.toString());
    }
  }

  /**
   * Returns an HTML page displaying all configurations of the specified program screen (block) as a
   * read only view.
   */
  @Secure(authorizers = Authorizers.Labels.CIVIFORM_ADMIN)
  public Result show(Request request, long programId, long blockId) {
    requestChecker.throwIfProgramNotActive(programId);

    try {
      ProgramDefinition program = programService.getProgramDefinition(programId);
      BlockDefinition block = program.getBlockDefinition(blockId);
      return renderReadOnlyViewWithMessage(request, program, block);
    } catch (ProgramNotFoundException | ProgramBlockDefinitionNotFoundException e) {
      return notFound(e.toString());
    }
  }

  /** POST endpoint for updating a screen (block) for the program. */
  @Secure(authorizers = Authorizers.Labels.CIVIFORM_ADMIN)
  public Result update(Request request, long programId, long blockId) {
    requestChecker.throwIfProgramNotDraft(programId);

    Form<BlockForm> blockFormWrapper = formFactory.form(BlockForm.class);
    BlockForm blockForm = blockFormWrapper.bindFromRequest(request).get();

    try {
      ErrorAnd<ProgramDefinition, CiviFormError> result =
          programService.updateBlock(programId, blockId, blockForm);
      if (result.isError()) {
        ToastMessage message = new ToastMessage(joinErrors(result.getErrors()), ERROR);
        return renderEditViewWithMessage(
            request, result.getResult(), blockId, blockForm, Optional.of(message));
      }
    } catch (ProgramNotFoundException | ProgramBlockDefinitionNotFoundException e) {
      return notFound(e.toString());
    }

    return redirect(routes.AdminProgramBlocksController.edit(programId, blockId));
  }

  /** POST endpoint for moving a screen (block) for the program. */
  @Secure(authorizers = Authorizers.Labels.CIVIFORM_ADMIN)
  public Result move(Request request, long programId, long blockId) {
    requestChecker.throwIfProgramNotDraft(programId);

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
    requestChecker.throwIfProgramNotDraft(programId);

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
      Request request,
      ProgramDefinition program,
      BlockDefinition block,
      Optional<ToastMessage> message) {
    ReadOnlyQuestionService roQuestionService =
        questionService.getReadOnlyQuestionService().toCompletableFuture().join();

    return ok(editView.render(
            request, program, block, message, roQuestionService.getUpToDateQuestions()))
        .addingToSession(
            request,
            ProgramSettingsEditView.NAVIGATION_SOURCE_SESSION_KEY,
            ProgramSettingsEditView.NAVIGATION_SOURCE_PROGRAM_BLOCKS_SESSION_VALUE);
  }

  private Result renderReadOnlyViewWithMessage(
      Request request, ProgramDefinition program, BlockDefinition block) {
    ReadOnlyQuestionService roQuestionService =
        questionService.getReadOnlyQuestionService().toCompletableFuture().join();

    return ok(
        readOnlyView.render(
            request, program, block, Optional.empty(), roQuestionService.getUpToDateQuestions()));
  }

  private Result renderEditViewWithMessage(
      Request request,
      ProgramDefinition program,
      long blockId,
      BlockForm blockForm,
      Optional<ToastMessage> message) {
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
