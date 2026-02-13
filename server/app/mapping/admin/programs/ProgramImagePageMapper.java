package mapping.admin.programs;

import com.google.common.collect.ImmutableMap;
import java.util.Optional;
import services.LocalizedStrings;
import services.program.ProgramDefinition;
import views.admin.programs.ProgramEditStatus;
import views.admin.programs.ProgramImagePageViewModel;

/** Maps data to the ProgramImagePageViewModel. */
public final class ProgramImagePageMapper {

  public ProgramImagePageViewModel map(
      ProgramDefinition programDefinition,
      String editStatus,
      boolean hasTranslatableLocales,
      String imageUploadFormAction,
      String imageUploadFormClass,
      ImmutableMap<String, String> imageUploadFormFields,
      int fileLimitMb,
      String cardPreviewHtml,
      Optional<String> successMessage,
      Optional<String> errorMessage) {
    ProgramEditStatus editStatusEnum = ProgramEditStatus.getStatusFromString(editStatus);

    String existingDescription =
        programDefinition
            .localizedSummaryImageDescription()
            .map(LocalizedStrings::getDefault)
            .orElse("");
    boolean hasExistingDescription = !existingDescription.isBlank();

    boolean showContinueButton =
        editStatusEnum == ProgramEditStatus.CREATION
            || editStatusEnum == ProgramEditStatus.CREATION_EDIT;

    return ProgramImagePageViewModel.builder()
        .programId(programDefinition.id())
        .editStatus(editStatus)
        .programAdminName(programDefinition.adminName())
        .hasTranslatableLocales(hasTranslatableLocales)
        .existingDescription(existingDescription)
        .hasExistingDescription(hasExistingDescription)
        .manageTranslationsDisabled(!hasExistingDescription)
        .imageUploadFormAction(imageUploadFormAction)
        .imageUploadFormClass(imageUploadFormClass)
        .imageUploadFormFields(imageUploadFormFields)
        .fileInputDisabled(!hasExistingDescription)
        .fileLimitMb(fileLimitMb)
        .hasExistingImage(programDefinition.summaryImageFileKey().isPresent())
        .cardPreviewHtml(cardPreviewHtml)
        .showContinueButton(showContinueButton)
        .successMessage(successMessage)
        .errorMessage(errorMessage)
        .build();
  }
}
