package mapping.admin.programs;

import com.google.common.collect.ImmutableList;
import java.time.Instant;
import services.program.ActiveAndDraftPrograms;
import services.program.ProgramDefinition;
import services.program.ProgramType;
import views.admin.programs.ProgramAdminListPageViewModel;

/** Maps data to the ProgramAdminListPageViewModel for the program admin list page. */
public final class ProgramAdminListPageMapper {

  public ProgramAdminListPageViewModel map(
      ActiveAndDraftPrograms programs, ImmutableList<String> authorizedPrograms, String baseUrl) {
    ImmutableList<ProgramAdminListPageViewModel.ProgramCardData> programCards =
        programs.getActivePrograms().stream()
            .filter(program -> authorizedPrograms.contains(program.adminName()))
            .sorted(
                (a, b) -> {
                  boolean aIsPreScreener = a.programType().equals(ProgramType.PRE_SCREENER_FORM);
                  boolean bIsPreScreener = b.programType().equals(ProgramType.PRE_SCREENER_FORM);
                  if (aIsPreScreener != bIsPreScreener) {
                    return aIsPreScreener ? -1 : 1;
                  }
                  int timeCompare =
                      b.lastModifiedTime()
                          .orElse(Instant.EPOCH)
                          .compareTo(a.lastModifiedTime().orElse(Instant.EPOCH));
                  if (timeCompare != 0) {
                    return timeCompare;
                  }
                  return a.localizedName()
                      .getDefault()
                      .toLowerCase()
                      .compareTo(b.localizedName().getDefault().toLowerCase());
                })
            .map(program -> buildCardData(program, baseUrl))
            .collect(ImmutableList.toImmutableList());

    return ProgramAdminListPageViewModel.builder().programs(programCards).build();
  }

  private ProgramAdminListPageViewModel.ProgramCardData buildCardData(
      ProgramDefinition program, String baseUrl) {
    ProgramType programType = program.programType();
    boolean isExternal = programType.equals(ProgramType.EXTERNAL);

    return ProgramAdminListPageViewModel.ProgramCardData.builder()
        .programName(program.localizedName().getDefault())
        .adminName(program.adminName())
        .programType(programType.getValue())
        .lastUpdatedMillis(program.lastModifiedTime().map(i -> i.toEpochMilli()).orElse(0L))
        .programId(program.id())
        .isExternalProgram(isExternal)
        .slug(program.slug())
        .baseUrl(baseUrl)
        .build();
  }
}
