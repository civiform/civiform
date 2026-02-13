package views.admin.programs;

import com.google.common.collect.ImmutableMap;
import controllers.admin.routes;
import java.util.Optional;
import lombok.Builder;
import lombok.Data;
import views.admin.BaseViewModel;

@Data
@Builder
public final class ProgramImagePageViewModel implements BaseViewModel {
  private final long programId;
  private final String editStatus;
  private final String programAdminName;
  private final boolean hasTranslatableLocales;
  private final String existingDescription;
  private final boolean hasExistingDescription;
  private final boolean manageTranslationsDisabled;

  // File upload
  private final String imageUploadFormAction;
  private final String imageUploadFormClass;
  private final ImmutableMap<String, String> imageUploadFormFields;
  private final boolean fileInputDisabled;
  private final int fileLimitMb;

  // Delete image
  private final boolean hasExistingImage;

  // Card preview
  private final String cardPreviewHtml;

  // Creation flow
  private final boolean showContinueButton;

  // Flash messages
  private final Optional<String> successMessage;
  private final Optional<String> errorMessage;

  public String getBackUrl() {
    ProgramEditStatus editStatusEnum = ProgramEditStatus.getStatusFromString(editStatus);
    return switch (editStatusEnum) {
      case EDIT -> routes.AdminProgramBlocksController.index(programId).url();
      case CREATION, CREATION_EDIT ->
          routes.AdminProgramController.edit(programId, ProgramEditStatus.CREATION_EDIT.name())
              .url();
    };
  }

  public String getDescriptionFormActionUrl() {
    return routes.AdminProgramImageController.updateDescription(programId, editStatus).url();
  }

  public Optional<String> getManageTranslationsUrl() {
    if (!hasTranslatableLocales) {
      return Optional.empty();
    }
    return Optional.of(
        controllers.admin.routes.AdminProgramTranslationsController.redirectToFirstLocale(
                programAdminName)
            .url());
  }

  public String getDeleteFileUrl() {
    return routes.AdminProgramImageController.deleteFileKey(programId, editStatus).url();
  }

  public String getContinueUrl() {
    return routes.AdminProgramBlocksController.index(programId).url();
  }
}
