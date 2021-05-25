package controllers.admin;

import static com.google.common.base.Preconditions.checkNotNull;
import static play.mvc.Results.notFound;
import static play.mvc.Results.ok;

import auth.Authorizers;
import com.google.common.collect.ImmutableList;
import javax.inject.Inject;
import org.pac4j.play.java.Secure;
import play.mvc.Http.Request;
import play.mvc.Result;
import services.program.BlockDefinition;
import services.program.ProgramBlockDefinitionNotFoundException;
import services.program.ProgramDefinition;
import services.program.ProgramNotFoundException;
import services.program.ProgramService;
import services.question.QuestionService;
import views.admin.programs.ProgramBlockPredicatesEditView;

public class AdminProgramBlockPredicatesController {
  private final ProgramService programService;
//  private final QuestionService questionService;
  private final ProgramBlockPredicatesEditView predicatesEditView;

  @Inject
  public AdminProgramBlockPredicatesController(
      ProgramService programService,
//      QuestionService questionService,
      ProgramBlockPredicatesEditView predicatesEditView) {
    this.programService = checkNotNull(programService);
//    this.questionService = checkNotNull(questionService);
    this.predicatesEditView = checkNotNull(predicatesEditView);
  }

  @Secure(authorizers = Authorizers.Labels.UAT_ADMIN)
  public Result edit(Request request, long programId, long blockDefinitionId) {
    try {
      ProgramDefinition programDefinition = programService.getProgramDefinition(programId);
      BlockDefinition blockDefinition = programDefinition.getBlockDefinition(blockDefinitionId);
      return ok(
          predicatesEditView.render(
              request, programDefinition, blockDefinition, ImmutableList.of()));
    } catch (ProgramNotFoundException | ProgramBlockDefinitionNotFoundException e) {
      return notFound(e.toString());
    }
  }

  // TODO(natsid): Another `edit` method takes all of the above args PLUS a question definition ID
  //  (query param).
}
