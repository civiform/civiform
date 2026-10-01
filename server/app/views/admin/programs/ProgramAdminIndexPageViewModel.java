package views.admin.programs;

import com.google.common.collect.ImmutableList;
import lombok.Builder;
import lombok.Data;
import services.program.ProgramType;
import views.BaseViewModel;

/**
 * ViewModel for the program admin's program list page (Thymeleaf), the 1:1 port of {@link
 * ProgramAdministratorProgramListView}.
 */
@Data
@Builder
public final class ProgramAdminIndexPageViewModel implements BaseViewModel {

  // Page heading, always "Your programs"
  private final String title;

  // Cards for the active programs the admin administers, already sorted the way the
  // legacy page listed them.
  private final ImmutableList<ProgramCard> programCards;

  /**
   * One program card. This page only ever shows the active row of a card, the legacy counterpart of
   * {@code ProgramCardFactory.ProgramCardData} with an active program and no draft.
   */
  @Data
  @Builder
  public static final class ProgramCard {
    // Program id; the template derives the extra-actions element ids from it.
    private final long id;
    private final String title;
    private final String shortDescription;
    // Drives the program type indicator and the "Forms"/"Applications" button label.
    private final ProgramType programType;
    private final boolean preScreenerForm;
    // Null when the program has no admin note, which hides the note paragraph.
    private final String adminNote;
    private final String visibilityState;
    // Sorted, comma-joined category names, or "None".
    private final String categoriesText;

    // Human readable publish timestamp and its date-only form, "unknown" when unset.
    private final String publishedDateTime;
    private final String publishedDate;
    private final int blockCount;
    private final int questionCount;

    // Only CiviForm admins see the program image column; program admins get an
    // empty placeholder div.
    private final boolean showImageColumn;
    // Null when the program has no public image, which shows the gray placeholder icon.
    private final String imageUrl;
    private final String imageAltText;

    // Share link and applications actions, both null for external programs.
    private final String programLink;
    private final String applicationsUrl;
  }
}
