package views.admin.programs;

import static views.ViewUtils.ProgramDisplayType.DRAFT;

import controllers.admin.routes;
import services.program.BlockDefinition;
import services.program.ProgramDefinition;
import services.program.predicate.PredicateUseCase;
import views.admin.BaseViewModel;

public record ProgramPredicatesEditPageViewModel(
    ProgramDefinition programDefinition,
    BlockDefinition blockDefinition,
    PredicateUseCase predicateUseCase)
    implements BaseViewModel {

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
}
