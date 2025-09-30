package views.admin.programs.predicates;

import com.google.common.collect.ImmutableList;
import controllers.admin.AdminProgramBlockPredicatesController.OptionElement;
import controllers.admin.AdminProgramBlockPredicatesController.ScalarOptionElement;
import controllers.admin.routes;
import lombok.Builder;
import services.program.predicate.PredicateUseCase;
import views.admin.BaseViewModel;

/**
 * Partial view for rendering EditSubconditionPartial.html. This partial is used for editing a
 * subcondition within a condition of a predicate.
 */
@Builder
public record EditSubconditionPartialViewModel(
    long programId,
    long blockId,
    PredicateUseCase predicateUseCase,
    long conditionId,
    long subconditionId,
    ImmutableList<OptionElement> questionOptions,
    ImmutableList<ScalarOptionElement> scalarOptions,
    ImmutableList<OptionElement> operatorOptions)
    implements BaseViewModel {
  public String hxEditSubconditionEndpoint() {
    return routes.AdminProgramBlockPredicatesController.hxEditSubcondition(
            programId, blockId, predicateUseCase.name())
        .url();
  }

  public boolean hasSelectedQuestion() {
    return questionOptions.stream().anyMatch(OptionElement::selected);
  }
}
