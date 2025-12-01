package views.admin.programs.predicates;

import com.google.common.collect.ImmutableList;
import controllers.admin.AdminProgramBlockPredicatesController.OptionElement;
import java.util.Optional;
import lombok.Builder;
import views.admin.BaseViewModel;

/**
 * Partial view for rendering PredicateValuesInputPartial.html. This partial is used for editing
 * subcondition values within a condition of a predicate.
 */
@Builder(toBuilder = true)
public record PredicateValuesInputPartialViewModel(
    long conditionId,
    long subconditionId,
    Optional<String> questionType,
    Optional<String> operator,
    Optional<String> scalar,
    String userEnteredValue,
    String secondUserEnteredValue,
    ImmutableList<OptionElement> valueOptions,
    boolean hasSelectedQuestion)
    implements BaseViewModel {}
