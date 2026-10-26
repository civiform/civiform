package mapping.admin.programs;

import com.google.common.collect.ImmutableList;
import java.util.Optional;
import java.util.UUID;
import services.program.ProgramDefinition;
import views.admin.programs.ManageProgramAdminsPageViewModel;

/** Maps a program and its admins to the ManageProgramAdminsPageViewModel. */
public final class ManageProgramAdminsPageMapper {

  /**
   * Maps the program, its current admins and the error of a failed add to the view model.
   *
   * @param program the program whose admins are being managed
   * @param existingAdminEmails email addresses of the program's current admins
   * @param errorMessage the error of a failed add attempt, shown as an error toast
   */
  public ManageProgramAdminsPageViewModel map(
      ProgramDefinition program,
      ImmutableList<String> existingAdminEmails,
      Optional<String> errorMessage) {
    return ManageProgramAdminsPageViewModel.builder()
        .programAdminName(program.adminName())
        .backUrl(controllers.admin.routes.AdminProgramController.index().url())
        .addAdminUrl(
            controllers.admin.routes.ProgramAdminManagementController.add(program.id()).url())
        .deleteAdminUrl(
            controllers.admin.routes.ProgramAdminManagementController.delete(program.id()).url())
        .existingAdminEmails(existingAdminEmails)
        // The legacy ToastMessage.errorNonLocalized prefixed the message, and every
        // ToastMessage gets a random id.
        .errorMessage(errorMessage.map(error -> "Error: " + error).orElse(null))
        .errorToastId(errorMessage.isPresent() ? UUID.randomUUID().toString() : null)
        .build();
  }
}
