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
   * Card preview params when {@link controllers.admin.ProgramCardPreviewController} succeeds. Empty
   * means preview could not be built (see controller); this page treats empty as a failed load for
   * the error banner.
   */
  private final Optional<ProgramCardParams> cardPreviewParams;

  /**
   * True when preview params are absent after the controller attempted to load them. Today the
   * controller only leaves this empty after an exception, not on a successful empty response.
   */
  public boolean isCardPreviewFailed() {
    return cardPreviewParams.isEmpty();
  }

  public String getBackUrl() {
    long id = program.id();
    return switch (programEditStatus) {
      case EDIT -> routes.AdminProgramBlocksController.index(id).url();
      case CREATION, CREATION_EDIT ->
          routes.AdminProgramController.edit(id, ProgramEditStatus.CREATION_EDIT.name()).url();
    };
  }

  public String getSubmitNavigationUrl() {
    return routes.AdminProgramImageController.updateDescription(
            program.id(), programEditStatus.name())
        .url();
  }

  public String hxUploadProgramImageUrl() {
    return routes.AdminProgramImageController.hxUploadProgramImage(
            program.id(), programEditStatus.name())
        .url();
  }

  public String submitButtonMessageKey() {
    return programEditStatus == ProgramEditStatus.CREATION
        ? "button.adminProgramImage.continue"
        : "button.adminProgramImage.save";
  }

  public boolean showCardPreview() {
    return cardPreviewParams.isPresent();
  }

  public boolean hasSummaryImage() {
    return program.summaryImageFileKey().isPresent();
  }

  public String getSummaryImageDescriptionDefault() {
    return program.localizedSummaryImageDescription().map(LocalizedStrings::getDefault).orElse("");
  }
}
