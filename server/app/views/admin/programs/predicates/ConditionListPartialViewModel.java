package views.admin.programs.predicates;

import com.google.common.collect.ImmutableList;
import controllers.admin.routes;
import lombok.Builder;
import services.program.predicate.PredicateUseCase;

/** Model for rendering the ConditionListPartial.html */
@Builder(toBuilder = true)
public record ConditionListPartialViewModel(
    long programId,
    long blockId,
    PredicateUseCase predicateUseCase,
    ImmutableList<EditConditionPartialViewModel> conditions)
    implements EditPredicateBaseViewModel {

  public String hxEditConditionEndpoint() {
    return routes.AdminProgramBlockPredicatesController.hxEditCondition(
            programId, blockId, predicateUseCase.name())
        .url();
  }
}
