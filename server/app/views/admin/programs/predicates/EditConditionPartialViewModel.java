package views.admin.programs.predicates;

import com.google.common.collect.ImmutableList;
import controllers.admin.AdminProgramBlockPredicatesController.OptionElement;
import controllers.admin.AdminProgramBlockPredicatesController.ScalarOptionElement;
import controllers.admin.routes;
import lombok.Builder;
import services.program.predicate.PredicateUseCase;

/** Model for rendering the EditConditionPartial.html */
@Builder
public record EditConditionPartialViewModel(
    long programId,
    long blockId,
    PredicateUseCase predicateUseCase,
    long conditionId,
    ImmutableList<OptionElement> questionOptions,
    ImmutableList<ScalarOptionElement> scalarOptions,
    ImmutableList<OptionElement> operatorOptions)
    implements EditPredicateBaseViewModel {

  public String hxEditConditionEndpoint() {
    return routes.AdminProgramBlockPredicatesController.hxEditCondition(
            programId, blockId, predicateUseCase.name())
        .url();
  }

  public String hxEditSubconditionEndpoint() {
    return routes.AdminProgramBlockPredicatesController.hxEditSubcondition(
            programId, blockId, predicateUseCase.name())
        .url();
  }
}
