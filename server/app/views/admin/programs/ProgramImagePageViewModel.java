package views.admin.programs;

import controllers.admin.routes;
import java.util.Optional;
import lombok.Builder;
import lombok.Getter;
import services.LocalizedStrings;
import services.program.ProgramDefinition;
import views.BaseViewModel;
import views.applicant.programindex.ProgramCardsSectionParamsFactory.ProgramCardParams;

/** View model for the Thymeleaf program image admin page. */
@Getter
@Builder
public final class ProgramImagePageViewModel implements BaseViewModel {

  private final ProgramEditStatus programEditStatus;
  private final ProgramDefinition program;

  /** Maximum program summary image upload size in megabytes (for client validation). */
  private final int maxFileSizeMb;

  /**
   * Card preview params when {@link ProgramCardPreview} succeeds. Empty means preview could not be
   * built (see controller); this page treats empty as a failed load for the error banner.
   */
  private final Optional<ProgramCardParams> programCardParams;

  /** Flash message after redirect, if present. */
  private final Optional<String> flashSuccess;

  /** Flash error after redirect, if present. */
  private final Optional<String> flashError;

  public String getBackUrl() {
    long id = program.id();
    return switch (programEditStatus) {
      case EDIT -> routes.AdminProgramBlocksController.index(id).url();
      case CREATION, CREATION_EDIT ->
          routes.AdminProgramController.edit(id, ProgramEditStatus.CREATION_EDIT.name()).url();
    };
  }

  public String getSubmitNavigationUrl() {
    return routes.AdminProgramImageController.uploadProgramImage(
            program.id(), programEditStatus.name())
        .url();
  }

  public boolean showContinueButton() {
    return programEditStatus == ProgramEditStatus.CREATION
        || programEditStatus == ProgramEditStatus.CREATION_EDIT;
  }

  public String getContinueNavigationUrl() {
    return routes.AdminProgramBlocksController.index(program.id()).url();
  }

  public boolean showCardPreview() {
    return programCardParams.isPresent();
  }

  public boolean hasSummaryImage() {
    return program.summaryImageFileKey().isPresent();
  }

  public String getSummaryImageDescriptionDefault() {
    return program.localizedSummaryImageDescription().map(LocalizedStrings::getDefault).orElse("");
  }
}
