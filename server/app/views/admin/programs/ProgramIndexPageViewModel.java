package views.admin.programs;

import com.google.common.collect.ImmutableList;
import lombok.Builder;
import lombok.Data;
import services.program.ProgramType;
import views.BaseViewModel;

/** ViewModel for the CiviForm admin's program list page. */
@Data
@Builder
public final class ProgramIndexPageViewModel implements BaseViewModel {

  // Page heading, always "Program dashboard".
  private final String title;
  private final String explanation;

  // Flash toasts (success and/or error), rendered as hidden cf-toast-data divs.
  private final ImmutableList<Toast> toasts;

  private final String newProgramUrl;
  private final String importProgramUrl;

  // Trigger button id of the publish-all modal, null when there is nothing to publish.
  private final String publishAllTriggerId;

  // "In use" and "Disabled" links, empty when there are no disabled programs.
  private final ImmutableList<TabLink> tabs;

  // Program cards, in display order.
  private final ImmutableList<ProgramCard> programCards;

  // Modals for the page, in the same order the legacy view added them.
  private final ImmutableList<ModalModel> modals;

  /** A flash toast message. */
  @Data
  @Builder
  public static final class Toast {
    // Random UUID, mirroring ToastMessage's generated id.
    private final String id;
    private final String message;
    // "SUCCESS" or "ERROR" (ToastMessage.ToastType name).
    private final String type;
    private final boolean canDismiss;
  }

  /** A link that switches between the program list tabs. */
  @Data
  @Builder
  public static final class TabLink {
    private final String label;
    private final String url;
    private final boolean selected;
  }

  /** One program card, with a draft row and/or an active row. */
  @Data
  @Builder
  public static final class ProgramCard {
    private final String title;
    private final String shortDescription;
    // Drives the program type indicator.
    private final ProgramType programType;
    // Null when the program has no admin note, which hides the note paragraph.
    private final String adminNote;
    private final String visibilityState;
    // Sorted, comma-joined category names, or "None".
    private final String categoriesText;

    // Null when the program has no draft/active version.
    private final ProgramRow draftRow;
    private final ProgramRow activeRow;
  }

  /** One "draft" or "active" row within a program card. */
  @Data
  @Builder
  public static final class ProgramRow {
    // Program id; the template derives the extra-actions element ids from it.
    private final long id;
    // True for the active row, false for the draft row.
    private final boolean active;
    // True for the active row when a draft row is present (adds border-t).
    private final boolean secondRow;

    // Null when the program has no valid image, which shows the placeholder icon.
    private final String imageUrl;
    private final String imageAlt;

    // Human readable update timestamp and its date-only form, "unknown" when unset.
    private final String updatedDateTime;
    private final String updatedDate;
    private final int blockCount;
    private final int questionCount;

    // Null when the program has no universal question text to show.
    private final String universalQuestionsText;
    private final boolean translationComplete;

    private final ImmutableList<Action> rowActions;
    private final ImmutableList<Action> extraActions;

    public boolean getHasExtraActions() {
      return !extraActions.isEmpty();
    }
  }

  /**
   * One button in a program row. At most one of the behavior fields is set. A button with none of
   * them only has an id, and is the trigger of a modal connected by modal.ts.
   */
  @Data
  @Builder
  public static final class Action {
    // Null when the button has no id.
    private final String id;
    private final String label;
    // Icon fragment name in LegacySvgFragments.html ("iconEdit", ...).
    private final String iconFragment;
    // True for buttons that use the square-cornered dropdown style.
    private final boolean dropdownStyle;

    // Behavior: main.ts follows data-redirect-to on click.
    private final String redirectUrl;
    // Behavior: POST to this url through a hidden form with id formId.
    private final String postUrl;
    private final String formId;
    // Behavior: admin_programs.ts copies data-copyable-program-link on click.
    private final String copyableLink;
  }

  /** One legacy modal, rendered into the layout's #modal-container. */
  @Data
  @Builder
  public static final class ModalModel {
    /** Discriminates the modal content variants. */
    public enum Type {
      DEMOGRAPHICS_CSV,
      PUBLISH_PROGRAM,
      PUBLISH_ALL
    }

    private final Type type;
    private final String id;
    private final String title;
    // "lg:w-auto" (default) or "lg:w-1/3" (publish a single program).
    private final String widthClass;

    // DEMOGRAPHICS_CSV: form action. PUBLISH_PROGRAM and PUBLISH_ALL: POST url.
    private final String actionUrl;

    // PUBLISH_PROGRAM: true shows the missing universal questions warning.
    private final boolean missingUniversalQuestions;

    // PUBLISH_ALL fields.
    private final String programsHeading;
    private final ImmutableList<PublishAllProgram> draftPrograms;
    private final String questionsHeading;
    private final ImmutableList<PublishAllQuestion> draftQuestions;
  }

  /** One draft program listed in the publish-all modal. */
  @Data
  @Builder
  public static final class PublishAllProgram {
    private final String name;
    // Includes its surrounding spaces, e.g. " (Publicly visible) ".
    private final String visibilityText;
    // Null when the program has no universal question text to show.
    private final String universalQuestionsText;
    private final String editUrl;
  }

  /** One draft question listed in the publish-all modal. */
  @Data
  @Builder
  public static final class PublishAllQuestion {
    private final String text;
    // Question admin name, used as the Edit link's data-testid.
    private final String adminName;
    private final String editUrl;
  }
}
