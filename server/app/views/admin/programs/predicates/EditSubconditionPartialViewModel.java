package views.admin.programs.predicates;

import com.google.common.collect.ImmutableList;
import controllers.admin.AdminProgramBlockPredicatesController.OptionElement;
import controllers.admin.AdminProgramBlockPredicatesController.ScalarOptionElement;
import controllers.admin.routes;
import java.util.Optional;
import lombok.Builder;
import services.program.predicate.PredicateUseCase;
import views.admin.BaseViewModel;

/**
 * Partial view for rendering EditSubconditionPartial.html. This partial is used for editing a
 * subcondition within a condition of a predicate.
 */
@Builder(toBuilder = true)
public record EditSubconditionPartialViewModel(
    long programId,
    long blockId,
    PredicateUseCase predicateUseCase,
    Optional<String> selectedQuestionType,
    Optional<String> selectedOperator,
    Optional<String> selectedScalar,
    String userEnteredValue,
    String secondUserEnteredValue,
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

  public String hxDeleteSubconditionEndpoint() {
    return routes.AdminProgramBlockPredicatesController.hxDeleteSubcondition(
            programId, blockId, predicateUseCase.name())
        .url();
  }

  public boolean hasSelectedQuestion() {
    return questionOptions.stream().anyMatch(OptionElement::selected);
  }

  public PredicateValuesInputPartialViewModel predicateValuesInputModel(
      long conditionId, long subconditionId) {
    return PredicateValuesInputPartialViewModel.builder()
        .conditionId(conditionId)
        .subconditionId(subconditionId)
        .questionType(selectedQuestionType)
        .operator(selectedOperator)
        .scalar(selectedScalar)
        .userEnteredValue(userEnteredValue)
        .secondUserEnteredValue(secondUserEnteredValue)
        .valueOptions(valueOptions)
        .hasSelectedQuestion(hasSelectedQuestion())
        .build();
  }
}
