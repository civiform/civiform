package views.admin.programs.predicates;

import com.google.common.collect.ImmutableList;
import controllers.admin.AdminProgramBlockPredicatesController.OptionElement;
import controllers.admin.AdminProgramBlockPredicatesController.ScalarOptionElement;
import controllers.admin.routes;
import java.util.Optional;
import lombok.Builder;
import services.program.predicate.PredicateLogicalOperator;
import services.program.predicate.PredicateUseCase;

/** Model for rendering the EditConditionPartial.html */
@Builder(toBuilder = true)
public record EditConditionPartialViewModel(
    long programId,
    long blockId,
    PredicateUseCase predicateUseCase,
    PredicateLogicalOperator subconditionLogicalOperator,
    ImmutableList<EditSubconditionPartialViewModel> subconditions,
    ImmutableList<OptionElement> questionOptions,
    ImmutableList<ScalarOptionElement> scalarOptions,
    ImmutableList<OptionElement> operatorOptions,
    ImmutableList<OptionElement> valueOptions)
    implements EditPredicateBaseViewModel {

  public String hxAddConditionEndpoint() {
    return routes.AdminProgramBlockPredicatesController.hxAddCondition(
            programId, blockId, predicateUseCase.name())
        .url();
  }

  public String hxEditSubconditionEndpoint() {
    return routes.AdminProgramBlockPredicatesController.hxEditSubcondition(
            programId, blockId, predicateUseCase.name())
        .url();
  }

  public String hxDeleteConditionEndpoint() {
    return routes.AdminProgramBlockPredicatesController.hxDeleteCondition(
            programId, blockId, predicateUseCase.name())
        .url();
  }

  public EditSubconditionPartialViewModel emptySubconditionViewModel() {
    return EditSubconditionPartialViewModel.builder()
        .programId(programId)
        .blockId(blockId)
        .predicateUseCase(predicateUseCase)
        .selectedQuestionType(Optional.empty())
        .questionOptions(questionOptions)
        .scalarOptions(scalarOptions)
        .operatorOptions(operatorOptions)
        .valueOptions(valueOptions)
        .autofocus(false)
        .shouldAnnounceChanges(false)
        .build();
  }

  public SubconditionListPartialViewModel subconditionListModel(Long conditionId) {
    return SubconditionListPartialViewModel.builder()
        .programId(programId)
        .blockId(blockId)
        .conditionId(conditionId)
        .predicateUseCase(predicateUseCase)
        .predicateLogicalOperator(subconditionLogicalOperator)
        .subconditions(subconditions)
        .build();
  }
}
