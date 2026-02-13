package views.admin.questions;

import com.google.common.collect.ImmutableList;
import controllers.admin.routes;
import java.util.Optional;
import lombok.Builder;
import lombok.Data;
import views.admin.BaseViewModel;

@Data
@Builder
public final class QuestionsListPageViewModel implements BaseViewModel {
  private final int totalQuestionCount;
  private final ImmutableList<QuestionTypeLink> questionTypeLinks;
  private final Optional<String> initialFilter;
  private final boolean translationManagementEnabled;

  private final ImmutableList<QuestionCardData> universalQuestions;
  private final ImmutableList<QuestionCardData> nonUniversalQuestions;
  private final ImmutableList<QuestionCardData> archivedQuestions;

  private final Optional<String> successMessage;
  private final Optional<String> errorMessage;

  public String getCreateQuestionUrl() {
    return routes.AdminQuestionController.index(Optional.empty()).url();
  }

  @Data
  @Builder
  public static final class QuestionCardData {
    private final String questionName;
    private final String adminName;
    private final String adminDescription;
    private final String questionHelpText;
    private final String questionType;
    private final boolean isUniversal;

    // Sort data attributes
    private final String lastModifiedTimestamp;
    private final int numReferencingPrograms;

    // Referencing programs summary
    private final int usedInCount;
    private final int addedToCount;
    private final int removedFromCount;
    private final int disabledCount;
    private final ImmutableList<String> usedProgramNames;
    private final ImmutableList<String> addedProgramNames;
    private final ImmutableList<String> removedProgramNames;
    private final boolean hasReferencingPrograms;

    // Draft row (if present)
    private final Optional<VersionRowData> draftRow;
    // Active row (if present)
    private final Optional<VersionRowData> activeRow;
  }

  @Data
  @Builder
  public static final class VersionRowData {
    private final long questionId;
    private final String questionAdminName;
    private final String lifecycleLabel;
    private final String editedOnText;
    private final Optional<String> translationStatus;
    private final boolean isEditable;

    // Action flags
    private final boolean showTranslateLink;
    private final boolean showDiscardDraftLink;
    private final boolean showArchiveLink;
    private final boolean showRestoreLink;
    private final boolean showArchiveBlockedModal;
    private final String archiveBlockedQuestionName;

    public String getShowUrl() {
      return routes.AdminQuestionController.show(questionId).url();
    }

    public String getEditUrl() {
      return routes.AdminQuestionController.edit(questionId).url();
    }

    public Optional<String> getTranslateUrl() {
      return showTranslateLink
          ? Optional.of(
              routes.AdminQuestionTranslationsController.redirectToFirstLocale(questionAdminName)
                  .url())
          : Optional.empty();
    }

    public Optional<String> getDiscardDraftUrl() {
      return showDiscardDraftLink
          ? Optional.of(routes.AdminQuestionController.discardDraft(questionId).url())
          : Optional.empty();
    }

    public Optional<String> getArchiveUrl() {
      return showArchiveLink
          ? Optional.of(routes.AdminQuestionController.archive(questionId).url())
          : Optional.empty();
    }

    public Optional<String> getRestoreUrl() {
      return showRestoreLink
          ? Optional.of(routes.AdminQuestionController.restore(questionId).url())
          : Optional.empty();
    }

    public Optional<String> getDeletionStatusLabel() {
      if (showRestoreLink) {
        return Optional.of("Restore archived");
      } else if (showArchiveLink) {
        return Optional.of("Archive");
      } else {
        return Optional.of("NOT ARCHIVEABLE TODO");
      }
    }

    public String getDeletionStatusUrl() {
      // return switch (activeAndDraftQuestions.getDeletionStatus(definition.getName())) {
      if (showRestoreLink) {
        return getRestoreUrl().orElse("");
      } else if (showArchiveLink) {
        return getArchiveUrl().orElse("");
      } else {
        return "";
      }
    }
  }

  @Data
  @Builder
  public static final class QuestionTypeLink {
    private final String label;
    private final String typeString;
    private final String id;

    public String getUrl() {
      return routes.AdminQuestionController.newOne(
              typeString, routes.AdminQuestionController.index(Optional.empty()).url())
          .url();
    }
  }
}
