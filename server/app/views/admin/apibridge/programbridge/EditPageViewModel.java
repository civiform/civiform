package views.admin.apibridge.programbridge;

import com.google.common.collect.ImmutableList;
import lombok.Builder;
import models.ApiBridgeConfigurationModel;
import services.program.BlockDefinition;
import services.program.ProgramDefinition;
import views.admin.BaseViewModel;

/** Holds data used to render the main program api bridge edit page */
@Builder
public record EditPageViewModel(
    ProgramDefinition programDefinition,
    Boolean isDraftProgram,
    ImmutableList<ApiBridgeConfigurationModel> enabledApiBridgeConfigurations,
    String questionScalarsJson)
    implements BaseViewModel {
  public String getSaveUrl() {
    return controllers.admin.apibridge.routes.ProgramBridgeController.save(programDefinition().id())
        .url();
  }

  public String getBridgeConfigurationUrl() {
    return controllers.admin.apibridge.routes.ProgramBridgeController.hxBridgeConfiguration(
            programDefinition().id())
        .url();
  }

  public String getProgramEditUrl() {
    Long firstBlockId =
        programDefinition().blockDefinitions().stream()
            .findFirst()
            .map(BlockDefinition::id)
            .orElse(1L);
    return controllers.admin.routes.AdminProgramBlocksController.edit(
            programDefinition().id(), firstBlockId)
        .url();
  }
}
