package views.admin.programs;

import com.google.common.collect.ImmutableList;
import controllers.admin.routes;
import java.util.Optional;
import lombok.Builder;
import lombok.Data;
import views.admin.BaseViewModel;

/** ViewModel for the editable (draft) program block edit page (Thymeleaf). */
@Data
@Builder
public final class ProgramBlockEditPageViewModel implements BaseViewModel {

  // Program info header
  private final String programName;
  private final String programDescription;
  private final String adminNote;
  private final String categoriesText;
  private final boolean isExternal;
  private final boolean hasMalformedQuestions;
  private final String programSlug;
  private final boolean apiBridgeEnabled;
  private final boolean hasEligibilityPredicate;
  private final boolean hasVisibilityPredicate;

  // Block sidebar
  private final ImmutableList<BlockListItem> blockList;
  private final long programId;
  private final long selectedBlockId;

  // Add screen
  private final boolean enumeratorImprovementsEnabled;

  // Selected block detail
  private final String blockName;
  private final String blockDescription;
  private final boolean isEnumeratorBlock;
  private final boolean hasEnumeratorQuestion;

  // Block panel buttons
  private final boolean canDeleteBlock;
  private final boolean showDeleteButton;
  private final String deleteModalItemsText;

  // Block description modal fields
  private final String blockRawName;
  private final String blockRawDescription;
  private final boolean isRepeated;
  private final String namePrefix;

  // Questions
  private final ImmutableList<EditQuestionItem> questions;

  // Question bank
  private final boolean showQuestionBank;
  private final ImmutableList<QuestionBankItem> universalBankQuestions;
  private final ImmutableList<QuestionBankItem> nonUniversalBankQuestions;

  // Flash messages
  private final Optional<String> successMessage;
  private final Optional<String> errorMessage;

  // Predicate display data
  private final boolean eligibilityIsGating;
  private final boolean isPrescreenerForm;
  private final Optional<String> visibilityPredicateHeading;
  private final Optional<ImmutableList<String>> visibilityPredicateConditionList;
  private final Optional<String> eligibilityPredicateHeading;
  private final Optional<ImmutableList<String>> eligibilityPredicateConditionList;

  public String getEditProgramDetailsUrl() {
    return routes.AdminProgramController.edit(programId, ProgramEditStatus.EDIT.name()).url();
  }

  public String getEditProgramImageUrl() {
    return routes.AdminProgramImageController.index(programId, ProgramEditStatus.EDIT.name()).url();
  }

  public Optional<String> getEditBridgeDefinitionsUrl() {
    return apiBridgeEnabled
        ? Optional.of(
            controllers.admin.apibridge.routes.ProgramBridgeController.edit(programId).url())
        : Optional.empty();
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

  public String getCreateBlockUrl() {
    return routes.AdminProgramBlocksController.create(programId).url();
  }

  public String getBlockUpdateUrl() {
    return routes.AdminProgramBlocksController.update(programId, selectedBlockId).url();
  }

  public String getBlockDeleteUrl() {
    return routes.AdminProgramBlocksController.delete(programId, selectedBlockId).url();
  }

  public String getAddQuestionUrl() {
    return controllers.admin.routes.AdminProgramBlockQuestionsController.create(
            programId, selectedBlockId)
        .url();
  }

  public String getQuestionCreateRedirectUrl() {
    String editPageRedirectUrl =
        controllers.admin.routes.AdminProgramBlocksController.edit(programId, selectedBlockId).url()
            + "?sqb=true";
    return controllers.admin.routes.AdminQuestionController.newOne("text", editPageRedirectUrl)
        .url();
  }

  public String getEditVisibilityUrl() {
    return controllers.admin.routes.AdminProgramBlockPredicatesController.editVisibility(
            programId, selectedBlockId)
        .url();
  }

  public String getEditEligibilityUrl() {
    return controllers.admin.routes.AdminProgramBlockPredicatesController.editEligibility(
            programId, selectedBlockId)
        .url();
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
    private final boolean showMoveUp;
    private final boolean showMoveDown;
    private final ImmutableList<BlockListItem> children;

    public String getEditUrl() {
      return routes.AdminProgramBlocksController.edit(programId, id).url();
    }

    public String getMoveUpUrl() {
      return routes.AdminProgramBlocksController.move(programId, id).url();
    }

    public String getMoveDownUrl() {
      return routes.AdminProgramBlocksController.move(programId, id).url();
    }
  }

  /** Represents a question displayed in the editable block detail panel. */
  @Data
  @Builder
  public static final class EditQuestionItem {
    private final long questionId;
    private final String questionText;
    private final String helpText;
    private final String adminId;
    private final String questionTypeName;
    private final boolean isUniversal;
    private final boolean isMalformed;
    private final boolean showOptionalToggle;
    private final boolean isOptional;
    private final boolean isAddress;
    private final boolean addressCorrectionEnabled;
    private final boolean showAddressCorrectionToggle;
    private final boolean showMoveUp;
    private final boolean showMoveDown;
    private final int moveUpPosition;
    private final int moveDownPosition;
    private final boolean canRemove;
    private final long programId;
    private final long blockId;

    public String getToggleOptionalUrl() {
      return controllers.admin.routes.AdminProgramBlockQuestionsController.setOptional(
              programId, blockId, questionId)
          .url();
    }

    public String getToggleAddressCorrectionUrl() {
      return controllers.admin.routes.AdminProgramBlockQuestionsController
          .toggleAddressCorrectionEnabledState(programId, blockId, questionId)
          .url();
    }

    public String getMoveUpUrl() {
      return controllers.admin.routes.AdminProgramBlockQuestionsController.move(
              programId, blockId, questionId)
          .url();
    }

    public String getMoveDownUrl() {
      return controllers.admin.routes.AdminProgramBlockQuestionsController.move(
              programId, blockId, questionId)
          .url();
    }

    public String getEditUrl() {
      return routes.AdminQuestionController.edit(questionId).url();
    }

    public String getDeleteUrl() {
      return controllers.admin.routes.AdminProgramBlockQuestionsController.delete(
              programId, blockId, questionId)
          .url();
    }
  }

  /** Represents a question in the question bank panel. */
  @Data
  @Builder
  public static final class QuestionBankItem {
    private final long id;
    private final String questionText;
    private final String helpText;
    private final String adminId;
    private final String adminNote;
    private final boolean isUniversal;
    private final String questionTypeName;
    private final String relevantFilterText;
    private final String lastModifiedTime;
  }
}
