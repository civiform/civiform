package views.admin.programs.predicates;

import com.google.common.collect.ImmutableList;
import controllers.admin.routes;
import lombok.Builder;
import services.program.predicate.PredicateUseCase;

/** Model for rendering the ConditionListPartial.html */
@Builder(toBuilder = true)
public record SubconditionListPartialViewModel(
    long programId,
    long blockId,
    long conditionId,
    PredicateUseCase predicateUseCase,
    ImmutableList<EditSubconditionPartialViewModel> subconditions)
    implements EditPredicateBaseViewModel {
  public String hxAddSubconditionEndpoint() {
    return routes.AdminProgramBlockPredicatesController.hxAddSubcondition(
            programId, blockId, predicateUseCase.name())
        .url();
  }

  public String hxEditSubconditionEndpoint() {
    return routes.AdminProgramBlockPredicatesController.hxEditSubcondition(
            programId, blockId, predicateUseCase.name())
        .url();
  }
}
