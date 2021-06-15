package controllers.admin;

import static com.google.common.base.Preconditions.checkNotNull;
import static play.mvc.Results.notFound;
import static play.mvc.Results.ok;

import auth.Authorizers;
import controllers.CiviFormController;
import forms.BlockVisibilityPredicateForm;
import javax.inject.Inject;
import org.pac4j.play.java.Secure;
import play.data.Form;
import play.data.FormFactory;
import play.mvc.Http.Request;
import play.mvc.Result;
import services.applicant.question.Scalar;
import services.program.BlockDefinition;
import services.program.ProgramBlockDefinitionNotFoundException;
import services.program.ProgramDefinition;
import services.program.ProgramNotFoundException;
import services.program.ProgramService;
import services.program.predicate.LeafOperationExpressionNode;
import services.program.predicate.Operator;
import services.program.predicate.PredicateAction;
import services.program.predicate.PredicateDefinition;
import services.program.predicate.PredicateExpressionNode;
import services.program.predicate.PredicateValue;
import views.admin.programs.ProgramBlockPredicatesEditView;

public class AdminProgramBlockPredicatesController extends CiviFormController {
  private final ProgramService programService;
  private final ProgramBlockPredicatesEditView predicatesEditView;
  private final FormFactory formFactory;

  @Inject
  public AdminProgramBlockPredicatesController(
      ProgramService programService,
      ProgramBlockPredicatesEditView predicatesEditView,
      FormFactory formFactory) {
    this.programService = checkNotNull(programService);
    this.predicatesEditView = checkNotNull(predicatesEditView);
    this.formFactory = checkNotNull(formFactory);
  }

  @Secure(authorizers = Authorizers.Labels.UAT_ADMIN)
  public Result edit(Request request, long programId, long blockDefinitionId) {
    try {
      ProgramDefinition programDefinition = programService.getProgramDefinition(programId);
      BlockDefinition blockDefinition = programDefinition.getBlockDefinition(blockDefinitionId);
      return ok(
          predicatesEditView.render(
              request,
              programDefinition,
              blockDefinition,
              programDefinition.getAvailablePredicateQuestionDefinitions(blockDefinitionId)));
    } catch (ProgramNotFoundException e) {
      return notFound(String.format("Program ID %d not found.", programId));
    } catch (ProgramBlockDefinitionNotFoundException e) {
      return notFound(
          String.format("Block ID %d not found for Program %d", blockDefinitionId, programId));
    }
  }

  @Secure(authorizers = Authorizers.Labels.UAT_ADMIN)
  public Result update(Request request, long programId, long blockDefinitionId) {
    Form<BlockVisibilityPredicateForm> predicateFormWrapper =
        formFactory.form(BlockVisibilityPredicateForm.class).bindFromRequest(request);

    if (predicateFormWrapper.hasErrors()) {
      return redirect(
              routes.AdminProgramBlockPredicatesController.edit(programId, blockDefinitionId))
          .flashing("error", "Did not save visibility condition. Missing required fields.");
    } else {
      BlockVisibilityPredicateForm predicateForm = predicateFormWrapper.get();
      PredicateDefinition predicateDefinition =
          PredicateDefinition.create(
              PredicateExpressionNode.create(
                  LeafOperationExpressionNode.create(
                      predicateForm.getQuestionId(),
                      Scalar.valueOf(predicateForm.getScalar()),
                      Operator.valueOf(predicateForm.getOperator()),
                      PredicateValue.of(predicateForm.getPredicateValue()))),
              PredicateAction.valueOf(predicateForm.getPredicateAction()));

      try {
        programService.setBlockPredicate(programId, blockDefinitionId, predicateDefinition);
      } catch (ProgramNotFoundException e) {
        return notFound(String.format("Program ID %d not found.", programId));
      } catch (ProgramBlockDefinitionNotFoundException e) {
        return notFound(
            String.format("Block ID %d not found for Program %d", blockDefinitionId, programId));
      }

      return redirect(
              routes.AdminProgramBlockPredicatesController.edit(programId, blockDefinitionId))
          .flashing(
              "success",
              String.format("Saved visibility condition: %s", predicateDefinition));
    }
  }
}
