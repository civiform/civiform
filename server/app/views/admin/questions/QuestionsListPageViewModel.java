package views.admin.questions;

import java.util.List;
import lombok.Builder;
import lombok.Data;
import views.BaseViewModel;

/** ViewModel for the questions list page (Thymeleaf). */
@Data
@Builder
public final class QuestionsListPageViewModel implements BaseViewModel {

  // Flash toasts (success and/or error), rendered as hidden cf-toast-data divs.
  private final List<Toast> toasts;

  // Pre-populated value for the question filter input.
  private final String filterValue;

  // "Create new question" dropdown entries, in QuestionType declaration order.
  private final List<CreateQuestionOption> createQuestionOptions;

  // Options for the sort select, first option selected by default.
  private final List<SortOption> sortOptions;

  // Question cards, already sorted and grouped the way the legacy view grouped
  // them: universal, all other, and marked-for-archival.
  private final List<QuestionRow> universalRows;
  private final List<QuestionRow> otherRows;
  private final List<QuestionRow> archivedRows;

  // Total number of distinct question names.
  private final int totalQuestionCount;

  // Modals for the page, in the same order the legacy view accumulated them
  // (per sorted card: referencing modal, then draft row modals, then active
  // row modals).
  private final List<ModalModel> modals;

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

  /** One entry in the "Create new question" dropdown. */
  @Data
  @Builder
  public static final class CreateQuestionOption {
    // "create-<type>-question"
    private final String id;
    private final String url;
    private final String label;
    // Question type icon fragment name in LegacySvgFragments.html ("iconAddress", ...).
    private final String iconFragment;
    // "svg-link-<icon name>", mirroring Icons.questionTypeSvgWithId.
    private final String svgLinkId;
  }

  /** One option of the sort select. */
  @Data
  @Builder
  public static final class SortOption {
    private final String value;
    private final String label;
    private final boolean selected;
  }

  /** One question card (a question with its draft and/or active version rows). */
  @Data
  @Builder
  public static final class QuestionRow {
    // Text of the universal badge, or null when the question is not universal.
    private final String universalBadgeText;
    // Question type icon fragment name in LegacySvgFragments.html ("iconAddress", ...).
    private final String iconFragment;
    // Markdown-formatted, sanitized HTML (rendered with th:utext).
    private final String questionTextHtml;
    private final String helpTextHtml;
    private final String adminName;
    private final String adminDescription;
    // Sort data attributes.
    private final String lastModified;
    private final int numReferencingPrograms;
    // Referencing-programs cell lines ("Used in 2 programs." etc).
    private final List<String> referencingLines;
    // Trigger button id of the referencing-programs modal ("<modalId>-button"),
    // or null when there are no referencing programs.
    private final String seeListTriggerId;
    // Null when there is no draft/active version.
    private final VersionRow draftRow;
    private final VersionRow activeRow;
  }

  /** One "draft" or "active" row within a question card. */
  @Data
  @Builder
  public static final class VersionRow {
    // True for the "active" row when a draft row is present (adds border-t).
    private final boolean secondRow;
    private final String badgeText;
    private final String badgeBgClass;
    private final String badgeTextClass;
    private final String editedOnTimeText;
    private final String editedOnDateText;
    private final boolean translationComplete;
    // Redirect target of the Edit button.
    private final String editUrl;
    private final boolean editVisible;
    // "extra-actions-uuid-<uuid>"
    private final String extraActionsButtonId;
    private final List<ExtraAction> extraActions;

    public boolean getHasExtraActions() {
      return !extraActions.isEmpty();
    }
  }

  /** One entry in a version row's extra-actions dropdown. */
  @Data
  @Builder
  public static final class ExtraAction {
    /** Discriminates the dropdown entry variants. */
    public enum Kind {
      // Redirect button to the translations page.
      TRANSLATE,
      // Trigger button of the discard-draft confirmation modal.
      DISCARD,
      // POST link button to the archive endpoint.
      ARCHIVE,
      // POST link button to the restore endpoint.
      RESTORE,
      // Trigger button of the referencing-programs modal explaining why the
      // question can't be archived.
      ARCHIVE_BLOCKED
    }

    private final Kind kind;
    // TRANSLATE: redirect url. ARCHIVE/RESTORE: POST url.
    private final String url;
    // ARCHIVE/RESTORE: random id of the hidden POST form.
    private final String formId;
    // DISCARD/ARCHIVE_BLOCKED: id of the modal trigger button.
    private final String triggerButtonId;
  }

  /** One legacy modal, rendered into the layout's #modal-container. */
  @Data
  @Builder
  public static final class ModalModel {
    /** Discriminates the modal content variants. */
    public enum Type {
      REFERENCING_PROGRAMS,
      DISCARD_DRAFT
    }

    private final Type type;
    private final String id;
    private final String title;
    // "lg:w-1/2" (referencing programs) or "lg:w-1/4" (discard draft).
    private final String widthClass;

    // REFERENCING_PROGRAMS fields.
    // True for the can't-archive variant, which prepends an explanation header.
    private final boolean showCantArchiveHeader;
    private final List<String> usedProgramNames;
    private final List<String> addedProgramNames;
    private final List<String> removedProgramNames;

    // DISCARD_DRAFT fields.
    private final String discardUrl;
    // Random id of the hidden POST form inside the confirm button.
    private final String discardFormId;
  }
}
