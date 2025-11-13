package views.admin.programs.predicates;

import controllers.admin.routes;
import lombok.Builder;
import services.program.predicate.PredicateUseCase;

/** Model for rendering AddFirstConditionPartial.html */
@Builder
public record AddFirstConditionPartialViewModel(
    long programId, long blockId, PredicateUseCase predicateUseCase)
    implements EditPredicateBaseViewModel {

  public String hxEditConditionEndpoint() {
    return routes.AdminProgramBlockPredicatesController.hxEditCondition(
            programId, blockId, predicateUseCase.name())
        .url();
  }
}
