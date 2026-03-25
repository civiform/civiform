package views.admin.programs.predicates;

import com.google.common.collect.ImmutableList;
import controllers.admin.AdminProgramBlockPredicatesController.OptionElement;
import controllers.admin.AdminProgramBlockPredicatesController.QuestionOptionElement;
import controllers.admin.AdminProgramBlockPredicatesController.ScalarOptionElement;
import controllers.admin.routes;
import java.util.Optional;
import lombok.Builder;
import services.program.predicate.PredicateUseCase;
import views.admin.BaseViewModel;

/**
 * Partial view for rendering EditSubconditionPartial.html. This partial is used for editing a
 * subcondition within a condition of a predicate.
 *
 * @param programId The programId for this CiviForm program.
 * @param blockId The ID for this program block (screen).
 * @param predicateUseCase The type of predicate. e.g. ELIGIBILITY or VISIBILITY.
 * @param selectedQuestionType The {@link QuestionType} of the user-selected question.
 * @param userEnteredValue For question types that use HTML input elements, the user-entered value.
 * @param secondUserEnteredValue The second user-entered value, for question types that use HTML
 *     input elements. Relevant for BETWEEN operators, which accept two inputs.
 * @param questionOptions Selectable questions for this predicate page.
 * @param scalarOptions Selectable scalars for this subcondition. Determined by the selected
 *     question.
 * @param operatorOptions Selectable operators for this subcondition. Determined by the selected
 *     question and scalar.
 * @param valueOptions Selectable values, for question types that allow users to select from a set
 *     of pre-configured values. Determined by the selected question.
 * @param autofocus Controls whether this subcondition will be focused on population.
 */
@Builder(toBuilder = true)
public record EditSubconditionPartialViewModel(
    long programId,
    long blockId,
    PredicateUseCase predicateUseCase,
    Optional<String> selectedQuestionType,
    String userEnteredValue,
    String secondUserEnteredValue,
    ImmutableList<QuestionOptionElement> questionOptions,
    ImmutableList<ScalarOptionElement> scalarOptions,
    ImmutableList<OptionElement> operatorOptions,
    ImmutableList<OptionElement> valueOptions,
    ImmutableList<String> invalidInputIds,
    boolean autofocus)
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
    return questionOptions.stream().anyMatch(QuestionOptionElement::selected);
  }

  public PredicateValuesInputPartialViewModel predicateValuesInputModel(
      long conditionId, long subconditionId) {
    return PredicateValuesInputPartialViewModel.builder()
        .conditionId(conditionId)
        .subconditionId(subconditionId)
        .questionType(selectedQuestionType)
        .userEnteredValue(userEnteredValue)
        .secondUserEnteredValue(secondUserEnteredValue)
        .valueOptions(valueOptions)
        .hasSelectedQuestion(hasSelectedQuestion())
        .invalidInputIds(invalidInputIds)
        .build();
  }
}
