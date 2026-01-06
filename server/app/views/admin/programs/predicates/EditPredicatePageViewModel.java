package views.admin.programs.predicates;

import static views.ViewUtils.ProgramDisplayType.DRAFT;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import controllers.admin.routes;
import lombok.Builder;
import services.program.BlockDefinition;
import services.program.ProgramDefinition;
import services.program.predicate.PredicateAction;
import services.program.predicate.PredicateUseCase;
import views.admin.programs.ProgramEditStatus;
import views.admin.programs.ProgramHeader;

@Builder
public record EditPredicatePageViewModel(
    ProgramDefinition programDefinition,
    BlockDefinition blockDefinition,
    PredicateUseCase predicateUseCase,
    ImmutableMap<String, ImmutableList<String>> operatorScalarMap,
    ImmutableList<EditConditionPartialViewModel> prePopulatedConditions,
    boolean hasAvailableQuestions,
    String eligibilityMessage)
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

  public String updatePredicateEndpoint() {
    return routes.AdminProgramBlockPredicatesController.updatePredicate(
            programDefinition.id(), blockDefinition.id(), predicateUseCase.name())
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

  public String hxAddConditionEndpoint() {
    return routes.AdminProgramBlockPredicatesController.hxAddCondition(
            programDefinition.id(), blockDefinition.id(), predicateUseCase.name())
        .url();
  }

  public String hxDeleteAllConditionsEndpoint() {
    return routes.AdminProgramBlockPredicatesController.hxDeleteAllConditions(
            programDefinition.id(), blockDefinition.id(), predicateUseCase.name())
        .url();
  }

  public boolean screenHasPredicates() {
    return this.prePopulatedConditions.size() > 0;
  }

  public ConditionListPartialViewModel newConditionList() {
    return ConditionListPartialViewModel.builder()
        .programId(programDefinition.id())
        .blockId(blockDefinition.id())
        .predicateUseCase(predicateUseCase)
        .conditions(prePopulatedConditions)
        .build();
  }
}
