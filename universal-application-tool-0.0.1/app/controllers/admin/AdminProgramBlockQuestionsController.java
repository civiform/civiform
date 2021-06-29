package controllers.admin;

import static com.google.common.base.Preconditions.checkNotNull;

import auth.Authorizers.Labels;
import com.google.common.collect.ImmutableList;
import forms.ProgramQuestionDefinitionOptionalityForm;
import javax.inject.Inject;
import org.pac4j.play.java.Secure;
import play.data.DynamicForm;
import play.data.FormFactory;
import play.mvc.Controller;
import play.mvc.Http.Request;
import play.mvc.Result;
import services.program.DuplicateProgramQuestionException;
import services.program.IllegalPredicateOrderingException;
import services.program.ProgramBlockDefinitionNotFoundException;
import services.program.ProgramNotFoundException;
import services.program.ProgramQuestionDefinitionNotFoundException;
import services.program.ProgramService;
import services.question.exceptions.QuestionNotFoundException;

/** Controller for admins editing questions on a screen (block) of a program. */
public class AdminProgramBlockQuestionsController extends Controller {

  private final ProgramService programService;
  private final FormFactory formFactory;

  @Inject
  public AdminProgramBlockQuestionsController(
      ProgramService programService, FormFactory formFactory) {
    this.programService = checkNotNull(programService);
    this.formFactory = checkNotNull(formFactory);
  }

  /** POST endpoint for adding one or more questions to a screen. */
  @Secure(authorizers = Labels.CIVIFORM_ADMIN)
  public Result create(Request request, long programId, long blockId) {
    DynamicForm requestData = formFactory.form().bindFromRequest(request);
    ImmutableList<Long> questionIds =
        requestData.rawData().entrySet().stream()
            .filter(formField -> formField.getKey().startsWith("question-"))
            .map(formField -> Long.valueOf(formField.getValue()))
            .collect(ImmutableList.toImmutableList());

    try {
      programService.addQuestionsToBlock(programId, blockId, questionIds);
    } catch (ProgramNotFoundException e) {
      return notFound(String.format("Program ID %d not found.", programId));
    } catch (ProgramBlockDefinitionNotFoundException e) {
      return notFound(String.format("Block ID %d not found for Program %d", blockId, programId));
    } catch (QuestionNotFoundException e) {
      return notFound(String.format("Question IDs %s not found", questionIds));
    } catch (DuplicateProgramQuestionException e) {
      return notFound(
          String.format(
              "Some Question IDs %s already exist in Program ID %d", questionIds, programId));
    }

    return redirect(controllers.admin.routes.AdminProgramBlocksController.edit(programId, blockId));
  }

  /** POST endpoint for removing a question from a screen. */
  @Secure(authorizers = Labels.CIVIFORM_ADMIN)
  public Result destroy(long programId, long blockDefinitionId, long questionDefinitionId) {
    try {
      programService.removeQuestionsFromBlock(
          programId, blockDefinitionId, ImmutableList.of(questionDefinitionId));
    } catch (IllegalPredicateOrderingException e) {
      return redirect(
              controllers.admin.routes.AdminProgramBlocksController.edit(
                  programId, blockDefinitionId))
          .flashing("error", e.getLocalizedMessage());
    } catch (ProgramNotFoundException e) {
      return notFound(String.format("Program ID %d not found.", programId));
    } catch (ProgramBlockDefinitionNotFoundException e) {
      return notFound(
          String.format("Block ID %d not found for Program %d", blockDefinitionId, programId));
    } catch (QuestionNotFoundException e) {
      return notFound(String.format("Question ID %s not found", questionDefinitionId));
    }

    return redirect(
        controllers.admin.routes.AdminProgramBlocksController.edit(programId, blockDefinitionId));
  }

  /** POST endpoint for editing whether or not a question is optional on a screen. */
  @Secure(authorizers = Labels.CIVIFORM_ADMIN)
  public Result setOptional(
      Request request, long programId, long blockDefinitionId, long questionDefinitionId) {
    ProgramQuestionDefinitionOptionalityForm programQuestionDefinitionOptionalityForm =
        formFactory
            .form(ProgramQuestionDefinitionOptionalityForm.class)
            .bindFromRequest(request)
            .get();

    try {
      programService.setProgramQuestionDefinitionOptionality(
          programId,
          blockDefinitionId,
          questionDefinitionId,
          programQuestionDefinitionOptionalityForm.getOptional());
    } catch (ProgramNotFoundException e) {
      return notFound(String.format("Program ID %d not found.", programId));
    } catch (ProgramBlockDefinitionNotFoundException e) {
      return notFound(
          String.format("Block ID %d not found for Program %d", blockDefinitionId, programId));
    } catch (ProgramQuestionDefinitionNotFoundException e) {
      return notFound(
          String.format(
              "Question ID %d not found in Block %d for program %d",
              questionDefinitionId, blockDefinitionId, programId));
    }

    return redirect(
        controllers.admin.routes.AdminProgramBlocksController.edit(programId, blockDefinitionId));
  }
}
