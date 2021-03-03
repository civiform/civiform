package controllers.admin;

import static com.google.common.base.Preconditions.checkNotNull;

import auth.Authorizers.Labels;
import com.google.common.collect.ImmutableList;
import javax.inject.Inject;
import org.pac4j.play.java.Secure;
import play.data.DynamicForm;
import play.data.FormFactory;
import play.mvc.Controller;
import play.mvc.Http.Request;
import play.mvc.Result;
import services.program.DuplicateProgramQuestionException;
import services.program.ProgramBlockNotFoundException;
import services.program.ProgramNotFoundException;
import services.program.ProgramService;
import services.question.QuestionNotFoundException;

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
    } catch (ProgramBlockNotFoundException e) {
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
  public Result destroy(Request request, long programId, long blockId) {
    DynamicForm requestData = formFactory.form().bindFromRequest(request);

    ImmutableList<Long> questionIds =
        requestData.rawData().entrySet().stream()
            .filter(formField -> formField.getKey().startsWith("block-question-"))
            .map(formField -> Long.valueOf(formField.getValue()))
            .collect(ImmutableList.toImmutableList());

    try {
      programService.removeQuestionsFromBlock(programId, blockId, questionIds);
    } catch (ProgramNotFoundException e) {
      return notFound(String.format("Program ID %d not found.", programId));
    } catch (ProgramBlockNotFoundException e) {
      return notFound(String.format("Block ID %d not found for Program %d", blockId, programId));
    } catch (QuestionNotFoundException e) {
      return notFound(String.format("Question ID %s not found", questionIds));
    }

    return redirect(controllers.admin.routes.AdminProgramBlocksController.edit(programId, blockId));
  }
}
