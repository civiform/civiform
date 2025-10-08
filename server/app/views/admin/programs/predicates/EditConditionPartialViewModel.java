package views.admin.programs.predicates;

import com.google.common.collect.ImmutableList;
import controllers.admin.routes;
import services.program.predicate.PredicateUseCase;
import services.question.types.QuestionDefinition;

/** Model for rendering the EditConditionPartial.html */
public record EditConditionPartialViewModel(
    long programId,
    long blockId,
    PredicateUseCase predicateUseCase,
    long conditionId,
    ImmutableList<QuestionDefinition> questions)
    implements EditPredicateBaseViewModel {
  public String hxEditConditionEndpoint() {
    return routes.AdminProgramBlockPredicatesController.hxEditCondition(
            programId, blockId, predicateUseCase.name())
        .url();
  }
}
