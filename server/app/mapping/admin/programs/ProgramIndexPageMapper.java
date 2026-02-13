package mapping.admin.programs;

import com.google.common.collect.ImmutableList;
import java.util.Optional;
import models.CategoryModel;
import models.ProgramTab;
import services.program.ActiveAndDraftPrograms;
import services.program.ProgramDefinition;
import services.program.ProgramNotFoundException;
import services.program.ProgramService;
import services.program.ProgramType;
import services.question.ActiveAndDraftQuestions;
import services.question.types.QuestionDefinition;
import views.admin.programs.ProgramEditStatus;
import views.admin.programs.ProgramIndexPageViewModel;
import views.components.Icons;

/** Maps data to the ProgramIndexPageViewModel. */
public final class ProgramIndexPageMapper {

  public ProgramIndexPageViewModel map(
      ActiveAndDraftPrograms programs,
      ActiveAndDraftQuestions questions,
      ProgramTab selectedTab,
      String civicEntityName,
      ImmutableList<Long> universalQuestionIds,
      ActiveAndDraftPrograms allPrograms,
      ImmutableList<QuestionDefinition> draftQuestions,
      String baseUrl,
      ProgramService programService,
      Optional<String> successMessage,
      Optional<String> errorMessage) {
    boolean showPublishAll = allPrograms.anyDraft() || questions.draftVersionHasAnyEdits();

    ImmutableList<ProgramIndexPageViewModel.ProgramCardData> programCards =
        programs.getProgramNames().stream()
            .map(
                name ->
                    buildProgramCardData(
                        programs.getActiveProgramDefinition(name),
                        programs.getDraftProgramDefinition(name),
                        universalQuestionIds,
                        baseUrl,
                        programService))
            .sorted(
                (a, b) -> {
                  boolean aIsPreScreener = a.getProgramType().equals("common_intake_form");
                  boolean bIsPreScreener = b.getProgramType().equals("common_intake_form");
                  if (aIsPreScreener != bIsPreScreener) {
                    return aIsPreScreener ? -1 : 1;
                  }
                  int timeCompare =
                      Long.compare(b.getLastUpdatedMillis(), a.getLastUpdatedMillis());
                  if (timeCompare != 0) {
                    return timeCompare;
                  }
                  return a.getProgramName()
                      .toLowerCase()
                      .compareTo(b.getProgramName().toLowerCase());
                })
            .collect(ImmutableList.toImmutableList());

    ImmutableList<ProgramDefinition> sortedDraftPrograms =
        allPrograms.getDraftPrograms().stream()
            .sorted(java.util.Comparator.comparing(ProgramDefinition::adminName))
            .collect(ImmutableList.toImmutableList());

    ImmutableList<ProgramIndexPageViewModel.PublishDraftProgramItem> draftProgramItems =
        sortedDraftPrograms.stream()
            .map(p -> buildPublishDraftProgramItem(p, universalQuestionIds))
            .collect(ImmutableList.toImmutableList());

    ImmutableList<QuestionDefinition> sortedDraftQuestionDefs =
        draftQuestions.stream()
            .sorted(java.util.Comparator.comparing(QuestionDefinition::getName))
            .collect(ImmutableList.toImmutableList());

    ImmutableList<ProgramIndexPageViewModel.PublishDraftQuestionItem> draftQuestionItems =
        sortedDraftQuestionDefs.stream()
            .map(
                q ->
                    ProgramIndexPageViewModel.PublishDraftQuestionItem.builder()
                        .name(q.getQuestionText().getDefault())
                        .editUrl(
                            controllers.admin.routes.AdminQuestionController.edit(q.getId()).url())
                        .build())
            .collect(ImmutableList.toImmutableList());

    return ProgramIndexPageViewModel.builder()
        .civicEntityName(civicEntityName)
        .hasDisabledPrograms(programService.anyDisabledPrograms())
        .inUseTabSelected(selectedTab.equals(ProgramTab.IN_USE))
        .showPublishAllButton(showPublishAll)
        .programs(programCards)
        .draftProgramCount(sortedDraftPrograms.size())
        .draftQuestionCount(draftQuestions.size())
        .draftProgramsForPublish(draftProgramItems)
        .draftQuestionsForPublish(draftQuestionItems)
        .successMessage(successMessage)
        .errorMessage(errorMessage)
        .build();
  }

  private ProgramIndexPageViewModel.ProgramCardData buildProgramCardData(
      Optional<ProgramDefinition> activeProgram,
      Optional<ProgramDefinition> draftProgram,
      ImmutableList<Long> universalQuestionIds,
      String baseUrl,
      ProgramService programService) {
    ProgramDefinition displayProgram = draftProgram.orElseGet(activeProgram::get);

    Optional<ProgramIndexPageViewModel.DraftInfo> draftInfo = Optional.empty();
    if (draftProgram.isPresent()) {
      ProgramDefinition draft = draftProgram.get();
      boolean isExternal = draft.programType().equals(ProgramType.EXTERNAL);
      draftInfo =
          Optional.of(
              ProgramIndexPageViewModel.DraftInfo.builder()
                  .programId(draft.id())
                  .programAdminName(draft.adminName())
                  .isExternalProgram(isExternal)
                  .universalQuestionsText(
                      generateUniversalQuestionText(draft, universalQuestionIds))
                  .usesAllUniversalQuestions(
                      draft.getQuestionIdsInProgram().containsAll(universalQuestionIds))
                  .translationComplete(isTranslationComplete(draft, programService))
                  .build());
    }

    Optional<ProgramIndexPageViewModel.ActiveInfo> activeInfo = Optional.empty();
    if (activeProgram.isPresent()) {
      ProgramDefinition active = activeProgram.get();
      boolean isExternal = active.programType().equals(ProgramType.EXTERNAL);
      boolean hasDraft = draftProgram.isPresent();

      activeInfo =
          Optional.of(
              ProgramIndexPageViewModel.ActiveInfo.builder()
                  .programId(active.id())
                  .programAdminName(active.adminName())
                  .slug(active.slug())
                  .baseUrl(baseUrl)
                  .isExternalProgram(isExternal)
                  .hasDraft(hasDraft)
                  .isPreScreenerForm(active.programType().equals(ProgramType.PRE_SCREENER_FORM))
                  .universalQuestionsText(
                      generateUniversalQuestionText(active, universalQuestionIds))
                  .usesAllUniversalQuestions(
                      active.getQuestionIdsInProgram().containsAll(universalQuestionIds))
                  .translationComplete(isTranslationComplete(active, programService))
                  .build());
    }

    return ProgramIndexPageViewModel.ProgramCardData.builder()
        .programName(displayProgram.localizedName().getDefault())
        .programTypeIndicator(getProgramTypeIndicator(displayProgram.programType()))
        .shortDescription(displayProgram.localizedShortDescription().getDefault())
        .longDescription(displayProgram.localizedDescription().getDefault())
        .categories(
            displayProgram.categories().stream().map(CategoryModel::getDefaultName).toList())
        .adminName(displayProgram.adminName())
        .programType(displayProgram.programType().getValue())
        .lastUpdatedMillis(displayProgram.lastModifiedTime().map(i -> i.toEpochMilli()).orElse(0L))
        .draft(draftInfo)
        .active(activeInfo)
        .build();
  }

  private ProgramIndexPageViewModel.PublishDraftProgramItem buildPublishDraftProgramItem(
      ProgramDefinition program, ImmutableList<Long> universalQuestionIds) {
    String visibilityText =
        switch (program.displayMode()) {
          case DISABLED -> " (Hidden from applicants and Trusted Intermediaries) ";
          case HIDDEN_IN_INDEX -> " (Hidden from applicants) ";
          case PUBLIC -> " (Publicly visible) ";
          case SELECT_TI, TI_ONLY -> " ";
        };

    return ProgramIndexPageViewModel.PublishDraftProgramItem.builder()
        .name(program.localizedName().getDefault())
        .visibilityText(visibilityText)
        .universalQuestionsText(generateUniversalQuestionText(program, universalQuestionIds))
        .editUrl(
            controllers.admin.routes.AdminProgramController.edit(
                    program.id(), ProgramEditStatus.EDIT.name())
                .url())
        .build();
  }

  private Optional<String> generateUniversalQuestionText(
      ProgramDefinition program, ImmutableList<Long> universalQuestionIds) {
    if (program.programType().equals(ProgramType.EXTERNAL)) {
      return Optional.empty();
    }
    int countAll = universalQuestionIds.size();
    if (countAll == 0) {
      return Optional.empty();
    }
    int countMissing =
        (int)
            universalQuestionIds.stream()
                .filter(id -> !program.getQuestionIdsInProgram().contains(id))
                .count();
    String text = countMissing == 0 ? "all" : (countAll - countMissing) + " of " + countAll;
    return Optional.of("Contains " + text + " universal questions ");
  }

  private boolean isTranslationComplete(ProgramDefinition program, ProgramService programService) {
    try {
      return programService.isTranslationComplete(program);
    } catch (ProgramNotFoundException e) {
      return false;
    }
  }

  private String getProgramTypeIndicator(ProgramType programType) {
    return switch (programType) {
      case PRE_SCREENER_FORM -> "Pre-Screener";
      case EXTERNAL -> "External program";
      default -> "";
    };
  }

  private Icons getProgramTypeIndicatorIcon(ProgramType programType) {
    return switch (programType) {
      case PRE_SCREENER_FORM -> Icons.CHECK;
      case EXTERNAL -> Icons.LABEL;
      default -> null;
    };
  }
}
