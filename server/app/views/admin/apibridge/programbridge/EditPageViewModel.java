package views.admin.apibridge.programbridge;

import com.google.common.collect.ImmutableList;
import lombok.Builder;
import models.ApiBridgeConfigurationModel;
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
    return controllers.admin.routes.AdminProgramBlocksController.edit(programDefinition().id(), 1)
        .url();
  }
}
