package views.admin.programs.predicates;

import com.google.common.collect.ImmutableList;
import controllers.admin.AdminProgramBlockPredicatesController.OptionElement;
import java.util.Optional;
import lombok.Builder;
import services.question.types.QuestionType;
import views.admin.BaseViewModel;

/**
 * Partial view for rendering PredicateValuesInputPartial.html. This partial is used for editing
 * subcondition values within a condition of a predicate.
 *
 * @param conditionId The condition ID for this predicate condition.
 * @param subconditionId The subconditionId for this predicate subcondition.
 * @param questionType String representation of the {@link QuestionType} of the user-selected
 *     question.
 * @param userEnteredValue For question types that use HTML input elements, the user-entered value.
 * @param secondUserEnteredValue The second user-entered value, for question types that use HTML
 *     input elements. Relevant for BETWEEN operators, which accept two inputs.
 * @param valueOptions Selectable values, for question types that allow users to select from a set
 *     of pre-configured values. Determined by the selected question.
 */
@Builder(toBuilder = true)
public record PredicateValuesInputPartialViewModel(
    long conditionId,
    long subconditionId,
    Optional<String> questionType,
    String userEnteredValue,
    String secondUserEnteredValue,
    ImmutableList<OptionElement> valueOptions,
    boolean hasSelectedQuestion)
    implements BaseViewModel {}
