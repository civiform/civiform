package views.admin.programs.predicates;

import lombok.Data;
import play.data.validation.Constraints;

/**
 * Holds data POSTed when requesting a view to delete a predicate condition. This is a class because
 * Play doesn't currently support databinding records.
 */
@Data
public final class DeleteConditionCommand {
  @Constraints.Required private Long conditionId;
}
