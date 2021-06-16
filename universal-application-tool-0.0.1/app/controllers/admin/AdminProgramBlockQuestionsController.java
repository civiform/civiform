package controllers.admin;

import static com.google.common.base.Preconditions.checkNotNull;

import auth.Authorizers.Labels;
import com.google.common.collect.ImmutableList;
import forms.ProgramQuestionDefinitionRequiredForm;
import javax.inject.Inject;
import org.pac4j.play.java.Secure;
import play.data.DynamicForm;
import play.data.FormFactory;
import play.mvc.Controller;
import play.mvc.Http.Request;
import play.mvc.Result;
import services.program.DuplicateProgramQuestionException;
import services.program.ProgramBlockDefinitionNotFoundException;
import services.program.ProgramNotFoundException;
import services.program.ProgramService;
import services.question.exceptions.QuestionNotFoundException;

public class AdminProgramBlockQuestionsController extends Controller {

  private final ProgramService programService;
  private final FormFactory formFactory;

  @Inject
  public AdminProgramBlockQuestionsController(
      ProgramService programService, FormFactory formFactory) {
    this.programService = checkNotNull(programService);
    this.formFactory = checkNotNull(formFactory);
  }

  @Secure(authorizers = Labels.UAT_ADMIN)
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

  @Secure(authorizers = Labels.UAT_ADMIN)
  public Result destroy(long programId, long blockDefinitionId, long questionDefinitionId) {
    try {
      programService.removeQuestionsFromBlock(
          programId, blockDefinitionId, ImmutableList.of(questionDefinitionId));
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

  @Secure(authorizers = Labels.UAT_ADMIN)
  public Result setRequired(
      Request request, long programId, long blockDefinitionId, long questionDefinitionId) {
    ProgramQuestionDefinitionRequiredForm programQuestionDefinitionRequiredForm =
        formFactory
            .form(ProgramQuestionDefinitionRequiredForm.class)
            .bindFromRequest(request)
            .get();

    try {
      programService.setProgramQuestionDefinitionRequired(
          programId,
          blockDefinitionId,
          questionDefinitionId,
          programQuestionDefinitionRequiredForm.getRequired());
    } catch (ProgramNotFoundException e) {
      return notFound(String.format("Program ID %d not found.", programId));
    } catch (ProgramBlockDefinitionNotFoundException e) {
      return notFound(
          String.format("Block ID %d not found for Program %d", blockDefinitionId, programId));
    }

    return redirect(
        controllers.admin.routes.AdminProgramBlocksController.edit(programId, blockDefinitionId));
  }
}
