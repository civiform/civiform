package mapping.admin.programs;

import com.google.common.collect.ImmutableList;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import services.LocalizedStrings;
import services.program.ProgramDefinition;
import services.statuses.StatusDefinitions;
import views.admin.programs.ProgramStatusesPageViewModel;

/** Maps data to the ProgramStatusesPageViewModel for the program statuses page. */
public final class ProgramStatusesPageMapper {

  public ProgramStatusesPageViewModel map(
      ProgramDefinition program,
      StatusDefinitions activeStatusDefinitions,
      boolean hasTranslatableLocales,
      Optional<String> successMessage,
      Optional<String> errorMessage) {
    AtomicInteger counter = new AtomicInteger(0);
    ImmutableList<ProgramStatusesPageViewModel.StatusData> statusDataList =
        activeStatusDefinitions.getStatuses().stream()
            .map(
                status -> {
                  int idx = counter.getAndIncrement();
                  return ProgramStatusesPageViewModel.StatusData.builder()
                      .statusText(status.statusText())
                      .hasEmail(status.localizedEmailBodyText().isPresent())
                      .emailBody(
                          status
                              .localizedEmailBodyText()
                              .map(LocalizedStrings::getDefault)
                              .orElse(""))
                      .isDefault(status.defaultStatus().orElse(false))
                      .configuredStatusText(status.statusText())
                      .modalId("edit-status-modal-" + idx)
                      .deleteModalId("delete-status-modal-" + idx)
                      .build();
                })
            .collect(ImmutableList.toImmutableList());

    return ProgramStatusesPageViewModel.builder()
        .programName(program.localizedName().getDefault())
        .programId(program.id())
        .programAdminName(program.adminName())
        .hasTranslatableLocales(hasTranslatableLocales)
        .statuses(statusDataList)
        .successMessage(successMessage)
        .errorMessage(errorMessage)
        .build();
  }
}
