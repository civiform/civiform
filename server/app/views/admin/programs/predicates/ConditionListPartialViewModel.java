package views.admin.programs.predicates;

import com.google.common.collect.ImmutableList;
import controllers.admin.routes;
import lombok.Builder;
import services.program.predicate.PredicateLogicalOperator;
import services.program.predicate.PredicateUseCase;

/** Model for rendering the ConditionListPartial.html */
@Builder(toBuilder = true)
public record ConditionListPartialViewModel(
    long programId,
    long blockId,
    PredicateUseCase predicateUseCase,
    PredicateLogicalOperator predicateLogicalOperator,
    ImmutableList<EditConditionPartialViewModel> conditions)
    implements EditPredicateBaseViewModel {

  public String hxAddConditionEndpoint() {
    return routes.AdminProgramBlockPredicatesController.hxAddCondition(
            programId, blockId, predicateUseCase.name())
        .url();
  }
}
