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
import services.program.ProgramService;

public class AdminProgramBlockQuestionsController extends Controller {

  private final ProgramService programService;
  private final FormFactory formFactory;

  @Inject
  public AdminProgramBlockQuestionsController(ProgramService programService,
      FormFactory formFactory) {
    this.programService = checkNotNull(programService);
    this.formFactory = checkNotNull(formFactory);
  }

  @Secure(authorizers = Labels.UAT_ADMIN)
  public Result create(Request request, long programId, long blockId) {
    DynamicForm requestData = formFactory.form().bindFromRequest(request);

    ImmutableList<Long> questionIds = requestData.rawData().entrySet().stream()
        .filter(formField -> formField.getKey().startsWith("question-"))
        .map(formField -> Long.getLong(formField.getValue()))
        .collect(ImmutableList.toImmutableList());

    programService.addQuestionsToBlock(programId, blockId, questionIds);

    return redirect(controllers.admin.routes.AdminProgramBlocksController.edit(programId, blockId));
  }

  @Secure(authorizers = Labels.UAT_ADMIN)
  public Result destroy(Request request, long programId, long blockId, long questionId) {
    DynamicForm requestData = formFactory.form().bindFromRequest(request);
    System.out.println(requestData.rawData());
    return redirect(controllers.admin.routes.AdminProgramBlocksController.edit(programId, blockId));
  }
}
