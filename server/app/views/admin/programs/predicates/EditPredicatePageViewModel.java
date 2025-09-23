package views.admin.programs.predicates;

import static views.ViewUtils.ProgramDisplayType.DRAFT;

import com.google.common.collect.ImmutableList;
import controllers.admin.routes;
import services.program.BlockDefinition;
import services.program.ProgramDefinition;
import services.program.predicate.PredicateAction;
import services.program.predicate.PredicateUseCase;
import views.admin.programs.ProgramEditStatus;
import views.admin.programs.ProgramHeader;

public record EditPredicatePageViewModel(
    ProgramDefinition programDefinition,
    BlockDefinition blockDefinition,
    PredicateUseCase predicateUseCase)
    implements EditPredicateBaseViewModel {

  public ProgramHeader programHeader() {
    return new ProgramHeader(programDefinition, DRAFT);
  }

  public String blockEditUrl() {
    return routes.AdminProgramBlocksController.edit(programDefinition.id(), blockDefinition.id())
        .url();
  }

  public String programEditUrl() {
    return routes.AdminProgramController.edit(programDefinition.id(), ProgramEditStatus.EDIT.name())
        .url();
  }

  public String blockName() {
    return blockDefinition.name();
  }

  public ImmutableList<PredicateAction> visibilityActions() {
    return ImmutableList.of(PredicateAction.HIDE_BLOCK, PredicateAction.SHOW_BLOCK);
  }

  public PredicateAction eligibilityAction() {
    return PredicateAction.ELIGIBLE_BLOCK;
  }
}
