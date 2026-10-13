package mapping.admin.programs;

import com.google.common.collect.ImmutableList;
import java.time.Instant;
import java.util.Comparator;
import java.util.Locale;
import java.util.Optional;
import services.DateConverter;
import services.program.ProgramDefinition;
import services.program.ProgramType;
import views.admin.programs.ProgramAdminIndexPageViewModel;
import views.admin.programs.ProgramAdminIndexPageViewModel.ProgramCard;

/** Maps data to the ProgramAdminIndexPageViewModel for the program admin's program list page. */
public final class ProgramAdminIndexPageMapper {

  /**
   * Maps the active programs the admin administers to the view model.
   *
   * @param activePrograms every active program
   * @param administeredProgramNames admin names of the programs the current admin administers
   * @param baseUrl the site's base URL, prefixed to the copyable program link
   * @param dateConverter renders the publish timestamps
   */
  public ProgramAdminIndexPageViewModel map(
      ImmutableList<ProgramDefinition> activePrograms,
      ImmutableList<String> administeredProgramNames,
      String baseUrl,
      DateConverter dateConverter) {
    ImmutableList<ProgramCard> programCards =
        activePrograms.stream()
            .filter(program -> administeredProgramNames.contains(program.adminName()))
            .sorted(programTypeThenLastModifiedThenNameComparator())
            .map(program -> buildCard(program, baseUrl, dateConverter))
            .collect(ImmutableList.toImmutableList());

    return ProgramAdminIndexPageViewModel.builder()
        .title("Your programs")
        .programCards(programCards)
        .build();
  }

  /** Pre-screener forms first, then most recently modified, then by name. */
  private static Comparator<ProgramDefinition> programTypeThenLastModifiedThenNameComparator() {
    Comparator<ProgramDefinition> c =
        Comparator.comparingInt(
            program -> program.programType().equals(ProgramType.PRE_SCREENER_FORM) ? 0 : 1);

    return c.thenComparing(
            program -> program.lastModifiedTime().orElse(Instant.EPOCH), Comparator.reverseOrder())
        .thenComparing(program -> program.localizedName().getDefault().toLowerCase(Locale.ROOT));
  }

  private ProgramCard buildCard(
      ProgramDefinition program, String baseUrl, DateConverter dateConverter) {
    Optional<Instant> lastModifiedTime = program.lastModifiedTime();
    ImmutableList<String> categoryNames =
        program.categories().stream()
            .map(category -> category.getDefaultName())
            .sorted()
            .collect(ImmutableList.toImmutableList());

    ProgramCard.ProgramCardBuilder builder =
        ProgramCard.builder()
            .id(program.id())
            .title(program.localizedName().getDefault())
            .shortDescription(program.localizedShortDescription().getDefault())
            .programType(program.programType())
            .preScreenerForm(program.isPreScreenerForm())
            .adminNote(program.adminDescription().isBlank() ? null : program.adminDescription())
            .visibilityState(program.displayMode().visibilityState)
            .categoriesText(categoryNames.isEmpty() ? "None" : String.join(", ", categoryNames))
            .publishedDateTime(
                lastModifiedTime.map(dateConverter::renderDateTimeHumanReadable).orElse("unknown"))
            .publishedDate(lastModifiedTime.map(dateConverter::renderDate).orElse("unknown"))
            .blockCount(program.getBlockCount())
            .questionCount(program.getQuestionCount());

    // External programs have neither a share link nor applications to view.
    ProgramType programType = program.programType();
    if (programType.equals(ProgramType.DEFAULT)
        || programType.equals(ProgramType.PRE_SCREENER_FORM)) {
      builder
          .programLink(
              baseUrl
                  + controllers.applicant.routes.ApplicantProgramsController.show(program.slug())
                      .url())
          .applicationsUrl(
              controllers.admin.routes.AdminApplicationController.index(
                      program.id(),
                      /* search= */ Optional.empty(),
                      /* page= */ Optional.empty(),
                      /* fromDate= */ Optional.empty(),
                      /* untilDate= */ Optional.empty(),
                      /* applicationStatus= */ Optional.empty(),
                      /* selectedApplicationUri= */ Optional.empty(),
                      /* showDownloadModal= */ Optional.empty(),
                      /* message= */ Optional.empty())
                  .url());
    }

    return builder.build();
  }
}
