package mapping.admin.programs;

import com.google.common.collect.ImmutableList;
import java.util.Optional;
import services.program.ProgramDefinition;
import views.admin.programs.ManageProgramAdminsPageViewModel;

/** Maps data to the ManageProgramAdminsPageViewModel. */
public final class ManageProgramAdminsPageMapper {

  public ManageProgramAdminsPageViewModel map(
      ProgramDefinition program,
      ImmutableList<String> existingAdminEmails,
      Optional<String> errorMessage) {
    ImmutableList<ManageProgramAdminsPageViewModel.AdminRow> adminRows =
        existingAdminEmails.stream()
            .map(
                email ->
                    ManageProgramAdminsPageViewModel.AdminRow.builder()
                        .email(email)
                        .programId(program.id())
                        .build())
            .collect(ImmutableList.toImmutableList());

    return ManageProgramAdminsPageViewModel.builder()
        .programName(program.adminName())
        .programId(program.id())
        .existingAdmins(adminRows)
        .errorMessage(errorMessage)
        .build();
  }
}
