package views.admin.programs;

import com.google.common.collect.ImmutableList;
import controllers.admin.routes;
import java.util.Optional;
import lombok.Builder;
import lombok.Data;
import views.admin.BaseViewModel;

/** ViewModel for the read-only program block show page (Thymeleaf). */
@Data
@Builder
public final class ProgramBlockShowPageViewModel implements BaseViewModel {

  // Program info header
  private final String programName;
  private final String programDescription;
  private final String adminNote;
  private final String categoriesText;
  private final boolean isActive;
  private final boolean isExternal;
  private final long programId;
  private final String programSlug;

  // Block sidebar
  private final ImmutableList<BlockListItem> blockList;
  private final long selectedBlockId;

  // Selected block detail
  private final String blockName;
  private final String blockDescription;
  private final ImmutableList<QuestionItem> questions;

  // Predicate sections
  private final PredicateData visibilityPredicate;
  private final boolean showEligibilitySection;
  private final boolean eligibilityIsGating;
  private final PredicateData eligibilityPredicate;

  public String getEditProgramUrl() {
    return routes.AdminProgramController.newVersionFrom(programId).url();
  }

  public Optional<String> getPreviewUrl() {
    return isExternal
        ? Optional.empty()
        : Optional.of(routes.AdminProgramPreviewController.preview(programSlug).url());
  }

  public Optional<String> getDownloadPdfUrl() {
    return isExternal
        ? Optional.empty()
        : Optional.of(routes.AdminProgramPreviewController.pdfPreview(programId).url());
  }

  /** Represents a block in the left sidebar navigation. */
  @Data
  @Builder
  public static final class BlockListItem {
    private final long id;
    private final String name;
    private final String questionCountText;
    private final long programId;
    private final boolean selected;
    private final boolean hasVisibilityPredicate;
    private final boolean hasNullQuestion;
    private final int indentLevel;
    private final ImmutableList<BlockListItem> children;

    public String getUrl() {
      return routes.AdminProgramBlocksController.show(programId, id).url();
    }
  }

  /** Represents a question displayed in the block detail panel. */
  @Data
  @Builder
  public static final class QuestionItem {
    private final String questionText;
    private final String helpText;
    private final String adminId;
    private final String questionTypeName;
    private final boolean isUniversal;
    private final boolean isMalformed;
    private final boolean showOptionalLabel;
    private final boolean isOptional;
    private final boolean isAddress;
    private final boolean addressCorrectionEnabled;
  }

  /**
   * Holds a pre-computed, human-readable representation of a visibility or eligibility predicate.
   */
  @Data
  @Builder
  public static final class PredicateData {
    /** True if a predicate is defined; false means the section should show the "empty" state. */
    private final boolean present;

    /**
     * Human-readable heading (may contain HTML markup). Only meaningful when {@link #present} is
     * true.
     */
    private final String heading;

    /**
     * List of condition strings (may contain HTML markup). Empty when the predicate has a single
     * condition or when {@link #present} is false.
     */
    private final ImmutableList<String> conditionList;
  }
}
