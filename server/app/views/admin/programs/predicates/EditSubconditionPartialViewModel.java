package views.admin.programs.predicates;

import com.google.common.collect.ImmutableList;
import controllers.admin.AdminProgramBlockPredicatesController.OptionElement;
import controllers.admin.AdminProgramBlockPredicatesController.ScalarOptionElement;
import controllers.admin.routes;
import java.util.Optional;
import lombok.Builder;
import services.program.predicate.PredicateUseCase;
import services.program.predicate.Operator;
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
    Optional<String> selectedQuestionType,
    Optional<String> selectedOperator,
    ImmutableList<OptionElement> questionOptions,
    ImmutableList<ScalarOptionElement> scalarOptions,
    ImmutableList<OptionElement> operatorOptions,
    ImmutableList<OptionElement> valueOptions)
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
