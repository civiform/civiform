package views.admin.programs.predicates;

import com.google.common.collect.ImmutableList;
import controllers.admin.routes;
import services.program.predicate.PredicateExpressionNodeType;
import views.admin.BaseViewModel;

/**
 * A base view model for pages that need to edit a predicate. It includes methods that are shared
 * across all predicate editing views.
 */
public interface EditPredicateBaseViewModel extends BaseViewModel {

  default String hxEditConditionEndpoint() {
    return routes.AdminProgramBlockPredicatesController.hxEditCondition().url();
  }

  default ImmutableList<PredicateExpressionNodeType> operatorNodeTypes() {
    return ImmutableList.of(PredicateExpressionNodeType.AND, PredicateExpressionNodeType.OR);
  }
}
