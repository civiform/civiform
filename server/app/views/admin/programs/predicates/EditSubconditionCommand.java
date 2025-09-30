package views.admin.programs.predicates;

import lombok.Data;
import play.data.validation.Constraints;

/**
 * Holds data POSTed when requesting a view to edit a subcondition within a condition of a
 * predicate. This is a class because Play doesn't currently support databinding records.
 */
@Data
public final class EditSubconditionCommand {
  @Constraints.Required private Long conditionId;
  @Constraints.Required private Long subconditionId;
}
