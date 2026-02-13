package mapping.admin.questions;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Sets;
import java.time.Instant;
import java.util.Comparator;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import models.DisplayMode;
import services.DateConverter;
import services.DeletionStatus;
import services.TranslationLocales;
import services.program.ProgramDefinition;
import services.question.ActiveAndDraftQuestions;
import services.question.QuestionService;
import services.question.types.QuestionDefinition;
import services.question.types.QuestionType;
import views.admin.questions.QuestionsListPageViewModel;

/** Maps question data to the QuestionsListPageViewModel for the questions list page. */
public final class QuestionsListPageMapper {

  public QuestionsListPageViewModel map(
      ActiveAndDraftQuestions activeAndDraftQuestions,
      Optional<String> filter,
      boolean translationManagementEnabled,
      boolean hasTranslatableLocales,
      DateConverter dateConverter,
      QuestionService questionService,
      TranslationLocales translationLocales,
      boolean yesNoQuestionEnabled,
      boolean mapQuestionEnabled,
      Optional<String> successMessage,
      Optional<String> errorMessage) {

    Comparator<QuestionsListPageViewModel.QuestionCardData> comparator =
        Comparator.<QuestionsListPageViewModel.QuestionCardData, Boolean>comparing(
                QuestionsListPageViewModel.QuestionCardData::isUniversal)
            .thenComparing(QuestionsListPageViewModel.QuestionCardData::getLastModifiedTimestamp)
            .reversed()
            .thenComparing(card -> card.getQuestionName().toLowerCase(Locale.ROOT));

    ImmutableList.Builder<QuestionsListPageViewModel.QuestionCardData> universalBuilder =
        ImmutableList.builder();
    ImmutableList.Builder<QuestionsListPageViewModel.QuestionCardData> nonUniversalBuilder =
        ImmutableList.builder();
    ImmutableList.Builder<QuestionsListPageViewModel.QuestionCardData> archivedBuilder =
        ImmutableList.builder();

    ImmutableList<QuestionsListPageViewModel.QuestionCardData> allCards =
        activeAndDraftQuestions.getQuestionNames().stream()
            .map(
                name ->
                    buildQuestionCard(
                        name,
                        activeAndDraftQuestions,
                        translationManagementEnabled,
                        hasTranslatableLocales,
                        dateConverter,
                        questionService,
                        translationLocales))
            .sorted(comparator)
            .collect(ImmutableList.toImmutableList());

    for (QuestionsListPageViewModel.QuestionCardData card : allCards) {
      DeletionStatus deletionStatus =
          activeAndDraftQuestions.getDeletionStatus(card.getAdminName());
      if (deletionStatus == DeletionStatus.PENDING_DELETION) {
        archivedBuilder.add(card);
      } else if (card.isUniversal()) {
        universalBuilder.add(card);
      } else {
        nonUniversalBuilder.add(card);
      }
    }

    return QuestionsListPageViewModel.builder()
        .totalQuestionCount(activeAndDraftQuestions.getQuestionNames().size())
        .questionTypeLinks(buildQuestionTypeLinks(yesNoQuestionEnabled, mapQuestionEnabled))
        .initialFilter(filter)
        .translationManagementEnabled(translationManagementEnabled)
        .universalQuestions(universalBuilder.build())
        .nonUniversalQuestions(nonUniversalBuilder.build())
        .archivedQuestions(archivedBuilder.build())
        .successMessage(successMessage)
        .errorMessage(errorMessage)
        .build();
  }

  private QuestionsListPageViewModel.QuestionCardData buildQuestionCard(
      String name,
      ActiveAndDraftQuestions activeAndDraftQuestions,
      boolean translationManagementEnabled,
      boolean hasTranslatableLocales,
      DateConverter dateConverter,
      QuestionService questionService,
      TranslationLocales translationLocales) {
    Optional<QuestionDefinition> activeQuestion =
        activeAndDraftQuestions.getActiveQuestionDefinition(name);
    Optional<QuestionDefinition> draftQuestion =
        activeAndDraftQuestions.getDraftQuestionDefinition(name);

    QuestionDefinition latestDefinition = draftQuestion.orElseGet(activeQuestion::get);

    ActiveAndDraftQuestions.ReferencingPrograms referencingPrograms =
        activeAndDraftQuestions.getReferencingPrograms(name);

    // Build grouped referencing programs using set operations
    ImmutableMap<String, ProgramDefinition> activeProgramsMap =
        referencingPrograms.activeReferences().stream()
            .collect(
                ImmutableMap.toImmutableMap(ProgramDefinition::adminName, Function.identity()));
    ImmutableMap<String, ProgramDefinition> draftProgramsMap =
        referencingPrograms.draftReferences().stream()
            .collect(
                ImmutableMap.toImmutableMap(ProgramDefinition::adminName, Function.identity()));
    ImmutableMap<String, ProgramDefinition> draftDisabledProgramsMap =
        referencingPrograms.draftReferences().stream()
            .filter(program -> program.displayMode() == DisplayMode.DISABLED)
            .collect(
                ImmutableMap.toImmutableMap(ProgramDefinition::adminName, Function.identity()));

    Set<String> usedSet = Sets.intersection(activeProgramsMap.keySet(), draftProgramsMap.keySet());
    Set<String> addedSet =
        Sets.difference(
            Sets.difference(draftProgramsMap.keySet(), activeProgramsMap.keySet()),
            draftDisabledProgramsMap.keySet());
    Set<String> removedSet = Sets.difference(activeProgramsMap.keySet(), draftProgramsMap.keySet());
    Set<String> disabledSet =
        Sets.difference(draftDisabledProgramsMap.keySet(), activeProgramsMap.keySet());

    ImmutableList<String> usedProgramNames =
        usedSet.stream()
            .map(n -> draftProgramsMap.get(n).localizedName().getDefault())
            .sorted()
            .collect(ImmutableList.toImmutableList());
    ImmutableList<String> addedProgramNames =
        addedSet.stream()
            .map(n -> draftProgramsMap.get(n).localizedName().getDefault())
            .sorted()
            .collect(ImmutableList.toImmutableList());
    ImmutableList<String> removedProgramNames =
        removedSet.stream()
            .map(n -> activeProgramsMap.get(n).localizedName().getDefault())
            .sorted()
            .collect(ImmutableList.toImmutableList());
    boolean hasReferencingPrograms =
        !usedSet.isEmpty()
            || !addedSet.isEmpty()
            || !removedSet.isEmpty()
            || !disabledSet.isEmpty();

    DeletionStatus deletionStatus = activeAndDraftQuestions.getDeletionStatus(name);

    // Build draft row
    Optional<QuestionsListPageViewModel.VersionRowData> draftRow = Optional.empty();
    if (draftQuestion.isPresent()) {
      QuestionDefinition draft = draftQuestion.get();
      boolean hasDraftAndActive = activeQuestion.isPresent();
      String editedOn =
          "Edited on "
              + draft
                  .getLastModifiedTime()
                  .map(dateConverter::renderDateTimeHumanReadable)
                  .orElse("unknown");
      Optional<String> translationStatus = Optional.empty();
      if (translationManagementEnabled) {
        translationStatus =
            Optional.of(
                questionService.isTranslationComplete(translationLocales, draft)
                    ? "Translation complete"
                    : "Translation incomplete");
      }
      boolean showTranslateLink = hasTranslatableLocales;
      boolean showDiscardDraftLink = hasDraftAndActive;

      boolean showArchiveLink = false;
      boolean showRestoreLink = false;
      boolean showArchiveBlockedModal = false;
      if (deletionStatus == DeletionStatus.PENDING_DELETION) {
        showRestoreLink = true;
      } else if (deletionStatus == DeletionStatus.DELETABLE) {
        showArchiveLink = true;
      } else if (deletionStatus == DeletionStatus.NOT_DELETABLE) {
        showArchiveBlockedModal = true;
      }

      String lifecycleLabel;
      if (deletionStatus == DeletionStatus.PENDING_DELETION) {
        lifecycleLabel = "Pending deletion";
      } else {
        lifecycleLabel = "Draft";
      }

      draftRow =
          Optional.of(
              QuestionsListPageViewModel.VersionRowData.builder()
                  .questionId(draft.getId())
                  .questionAdminName(draft.getName())
                  .lifecycleLabel(lifecycleLabel)
                  .editedOnText(editedOn)
                  .translationStatus(translationStatus)
                  .isEditable(true)
                  .showTranslateLink(showTranslateLink)
                  .showDiscardDraftLink(showDiscardDraftLink)
                  .showArchiveLink(showArchiveLink)
                  .showRestoreLink(showRestoreLink)
                  .showArchiveBlockedModal(showArchiveBlockedModal)
                  .archiveBlockedQuestionName(name)
                  .build());
    }

    // Build active row
    Optional<QuestionsListPageViewModel.VersionRowData> activeRow = Optional.empty();
    if (activeQuestion.isPresent()) {
      QuestionDefinition active = activeQuestion.get();
      boolean isEditable = draftQuestion.isEmpty();
      String editedOn =
          "Edited on "
              + active
                  .getLastModifiedTime()
                  .map(dateConverter::renderDateTimeHumanReadable)
                  .orElse("unknown");
      Optional<String> translationStatus = Optional.empty();
      if (translationManagementEnabled && isEditable) {
        translationStatus =
            Optional.of(
                questionService.isTranslationComplete(translationLocales, active)
                    ? "Translation complete"
                    : "Translation incomplete");
      }
      boolean showTranslateLink2 =
          hasTranslatableLocales && translationManagementEnabled && isEditable;

      boolean showArchiveLink2 = false;
      boolean showRestoreLink2 = false;
      boolean showArchiveBlockedModal2 = false;
      if (isEditable) {
        if (deletionStatus == DeletionStatus.PENDING_DELETION) {
          showRestoreLink2 = true;
        } else if (deletionStatus == DeletionStatus.DELETABLE) {
          showArchiveLink2 = true;
        } else if (deletionStatus == DeletionStatus.NOT_DELETABLE) {
          showArchiveBlockedModal2 = true;
        }
      }

      activeRow =
          Optional.of(
              QuestionsListPageViewModel.VersionRowData.builder()
                  .questionId(active.getId())
                  .questionAdminName(active.getName())
                  .lifecycleLabel("Active")
                  .editedOnText(editedOn)
                  .translationStatus(translationStatus)
                  .isEditable(isEditable)
                  .showTranslateLink(showTranslateLink2)
                  .showDiscardDraftLink(false)
                  .showArchiveLink(showArchiveLink2)
                  .showRestoreLink(showRestoreLink2)
                  .showArchiveBlockedModal(showArchiveBlockedModal2)
                  .archiveBlockedQuestionName(name)
                  .build());
    }

    String questionHelpText =
        latestDefinition.getQuestionHelpText().isEmpty()
            ? ""
            : latestDefinition.getQuestionHelpText().getDefault();

    return QuestionsListPageViewModel.QuestionCardData.builder()
        .questionName(latestDefinition.getQuestionText().getDefault())
        .adminName(latestDefinition.getName())
        .adminDescription(latestDefinition.getDescription())
        .questionHelpText(questionHelpText)
        .questionType(latestDefinition.getQuestionType().toString())
        .isUniversal(latestDefinition.isUniversal())
        .lastModifiedTimestamp(
            latestDefinition.getLastModifiedTime().orElse(Instant.EPOCH).toString())
        .numReferencingPrograms(usedSet.size() + addedSet.size())
        .usedInCount(usedSet.size())
        .addedToCount(addedSet.size())
        .removedFromCount(removedSet.size())
        .disabledCount(disabledSet.size())
        .usedProgramNames(usedProgramNames)
        .addedProgramNames(addedProgramNames)
        .removedProgramNames(removedProgramNames)
        .hasReferencingPrograms(hasReferencingPrograms)
        .draftRow(draftRow)
        .activeRow(activeRow)
        .build();
  }

  private ImmutableList<QuestionsListPageViewModel.QuestionTypeLink> buildQuestionTypeLinks(
      boolean yesNoQuestionEnabled, boolean mapQuestionEnabled) {
    ImmutableList.Builder<QuestionsListPageViewModel.QuestionTypeLink> links =
        ImmutableList.builder();
    for (QuestionType type : QuestionType.values()) {
      if (type == QuestionType.NULL_QUESTION) {
        continue;
      }
      if (type == QuestionType.YES_NO && !yesNoQuestionEnabled) {
        continue;
      }
      if (type == QuestionType.MAP && !mapQuestionEnabled) {
        continue;
      }
      String typeString = type.toString().toLowerCase(Locale.ROOT);
      links.add(
          QuestionsListPageViewModel.QuestionTypeLink.builder()
              .label(type.getLabel())
              .typeString(typeString)
              .id("create-" + typeString + "-question")
              .build());
    }
    return links.build();
  }
}
