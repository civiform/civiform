package mapping.admin.programs;

import com.google.common.collect.ImmutableList;
import controllers.admin.routes;
import java.time.Instant;
import java.util.Comparator;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import lombok.Builder;
import models.ProgramTab;
import services.DateConverter;
import services.cloud.PublicFileNameFormatter;
import services.program.ActiveAndDraftPrograms;
import services.program.ProgramDefinition;
import services.program.ProgramType;
import services.question.ActiveAndDraftQuestions;
import services.question.types.QuestionDefinition;
import views.ProgramImageUtils;
import views.admin.programs.ProgramEditStatus;
import views.admin.programs.ProgramIndexPageViewModel;
import views.admin.programs.ProgramIndexPageViewModel.Action;
import views.admin.programs.ProgramIndexPageViewModel.ModalModel;
import views.admin.programs.ProgramIndexPageViewModel.ProgramCard;
import views.admin.programs.ProgramIndexPageViewModel.ProgramRow;
import views.admin.programs.ProgramIndexPageViewModel.PublishAllProgram;
import views.admin.programs.ProgramIndexPageViewModel.PublishAllQuestion;
import views.admin.programs.ProgramIndexPageViewModel.TabLink;
import views.admin.programs.ProgramIndexPageViewModel.Toast;

/** Maps data to the ProgramIndexPageViewModel for the CiviForm admin's program list page. */
public final class ProgramIndexPageMapper {

  private static final String DEMOGRAPHICS_ACTION_TEXT = "Download demographic data (CSV)";

  /**
   * The data the page is built from.
   *
   * @param programs the programs listed on the selected tab
   * @param allDraftPrograms every draft program, including disabled ones, which are also published
   *     when publishing all drafts. Only called when the publish-all modal is shown.
   * @param questions the active and draft questions
   * @param upToDateQuestions the latest version of each question, used to find universal questions
   * @param selectedTab the tab being rendered
   * @param anyDisabledPrograms whether the tab links are shown
   * @param civicEntityShortName the short name of the civic entity, used in the page explanation
   * @param baseUrl the site's base URL, prefixed to the copyable program link
   * @param hasTranslatableLocales whether the "Manage translations" action is shown
   * @param canViewApplications whether the current admin can see a program's applications
   * @param isTranslationComplete whether a program's translations are complete
   * @param publicImageUrl turns a program's summary image file key into its display URL
   * @param dateConverter renders the update timestamps
   * @param successMessage flash message shown as a success toast
   * @param errorMessage flash message shown as an error toast
   */
  @Builder
  public record Params(
      ActiveAndDraftPrograms programs,
      Supplier<ImmutableList<ProgramDefinition>> allDraftPrograms,
      ActiveAndDraftQuestions questions,
      ImmutableList<QuestionDefinition> upToDateQuestions,
      ProgramTab selectedTab,
      boolean anyDisabledPrograms,
      String civicEntityShortName,
      String baseUrl,
      boolean hasTranslatableLocales,
      Predicate<ProgramDefinition> canViewApplications,
      Predicate<ProgramDefinition> isTranslationComplete,
      Function<String, String> publicImageUrl,
      DateConverter dateConverter,
      Optional<String> successMessage,
      Optional<String> errorMessage) {}

  /** Builds the view model for the program list page. */
  public ProgramIndexPageViewModel map(Params params) {
    ImmutableList<Long> universalQuestionIds =
        params.upToDateQuestions().stream()
            .filter(QuestionDefinition::isUniversal)
            .map(QuestionDefinition::getId)
            .collect(ImmutableList.toImmutableList());
    Optional<ModalModel> publishAllModal = buildPublishAllModal(params, universalQuestionIds);

    ImmutableList.Builder<ModalModel> modals = ImmutableList.builder();
    modals.add(buildDemographicsModal());
    params.programs().getDraftPrograms().stream()
        .map(program -> buildPublishProgramModal(program, universalQuestionIds))
        .forEach(modals::add);
    publishAllModal.ifPresent(modals::add);

    return ProgramIndexPageViewModel.builder()
        .title("Program dashboard")
        .explanation("Create, edit and publish programs in " + params.civicEntityShortName())
        .toasts(buildToasts(params.successMessage(), params.errorMessage()))
        .newProgramUrl(routes.AdminProgramController.newOne().url())
        .importProgramUrl(routes.AdminImportController.index().url())
        .publishAllTriggerId(publishAllModal.map(modal -> modal.getId() + "-button").orElse(null))
        .tabs(buildTabs(params))
        .programCards(buildCards(params, universalQuestionIds))
        .modals(modals.build())
        .build();
  }

  private static ImmutableList<Toast> buildToasts(
      Optional<String> successMessage, Optional<String> errorMessage) {
    ImmutableList.Builder<Toast> toasts = ImmutableList.builder();

    successMessage.ifPresent(
        message ->
            toasts.add(
                Toast.builder()
                    .id(UUID.randomUUID().toString())
                    .message(message)
                    .type("SUCCESS")
                    .canDismiss(true)
                    .build()));
    errorMessage.ifPresent(
        message ->
            toasts.add(
                Toast.builder()
                    .id(UUID.randomUUID().toString())
                    .message("Error: " + message)
                    .type("ERROR")
                    .canDismiss(false)
                    .build()));

    return toasts.build();
  }

  private static ImmutableList<TabLink> buildTabs(Params params) {
    if (!params.anyDisabledPrograms()) {
      return ImmutableList.of();
    }

    return ImmutableList.of(
        buildTab(
            ProgramTab.IN_USE, params.selectedTab(), routes.AdminProgramController.index().url()),
        buildTab(
            ProgramTab.DISABLED,
            params.selectedTab(),
            routes.AdminProgramController.indexDisabled().url()));
  }

  private static TabLink buildTab(ProgramTab tab, ProgramTab selectedTab, String url) {
    return TabLink.builder()
        .label(tab.getTabName())
        .url(url)
        .selected(tab.equals(selectedTab))
        .build();
  }

  private ImmutableList<ProgramCard> buildCards(
      Params params, ImmutableList<Long> universalQuestionIds) {
    ActiveAndDraftPrograms programs = params.programs();

    return programs.getProgramNames().stream()
        .sorted(
            Comparator.comparing(
                name ->
                    displayProgram(
                        programs.getActiveProgramDefinition(name),
                        programs.getDraftProgramDefinition(name)),
                programTypeThenLastModifiedThenNameComparator()))
        .map(
            name ->
                buildCard(
                    programs.getActiveProgramDefinition(name),
                    programs.getDraftProgramDefinition(name),
                    params,
                    universalQuestionIds))
        .collect(ImmutableList.toImmutableList());
  }

  /** The card shows the draft version's details when there is one. */
  private static ProgramDefinition displayProgram(
      Optional<ProgramDefinition> activeProgram, Optional<ProgramDefinition> draftProgram) {
    return draftProgram.orElseGet(activeProgram::get);
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
      Optional<ProgramDefinition> activeProgram,
      Optional<ProgramDefinition> draftProgram,
      Params params,
      ImmutableList<Long> universalQuestionIds) {
    ProgramDefinition displayProgram = displayProgram(activeProgram, draftProgram);
    ImmutableList<String> categoryNames =
        displayProgram.categories().stream()
            .map(category -> category.getDefaultName())
            .sorted()
            .collect(ImmutableList.toImmutableList());

    return ProgramCard.builder()
        .title(displayProgram.localizedName().getDefault())
        .shortDescription(displayProgram.localizedShortDescription().getDefault())
        .programType(displayProgram.programType())
        .adminNote(
            displayProgram.adminDescription().isBlank() ? null : displayProgram.adminDescription())
        .visibilityState(displayProgram.displayMode().visibilityState)
        .categoriesText(categoryNames.isEmpty() ? "None" : String.join(", ", categoryNames))
        .draftRow(
            draftProgram
                .map(
                    program ->
                        buildRow(
                            program,
                            /* isActive= */ false,
                            /* secondRow= */ false,
                            draftRowActions(program),
                            draftRowExtraActions(program, params),
                            params,
                            universalQuestionIds))
                .orElse(null))
        .activeRow(
            activeProgram
                .map(
                    program ->
                        buildRow(
                            program,
                            /* isActive= */ true,
                            /* secondRow= */ draftProgram.isPresent(),
                            activeRowActions(program, params),
                            activeRowExtraActions(program, draftProgram.isEmpty(), params),
                            params,
                            universalQuestionIds))
                .orElse(null))
        .build();
  }

  private ProgramRow buildRow(
      ProgramDefinition program,
      boolean isActive,
      boolean secondRow,
      ImmutableList<Action> rowActions,
      ImmutableList<Action> extraActions,
      Params params,
      ImmutableList<Long> universalQuestionIds) {
    Optional<Instant> updatedTime = program.lastModifiedTime();
    DateConverter dateConverter = params.dateConverter();
    Optional<String> imageUrl = imageUrl(program, params.publicImageUrl());

    return ProgramRow.builder()
        .id(program.id())
        .active(isActive)
        .secondRow(secondRow)
        .imageUrl(imageUrl.orElse(null))
        .imageAlt(
            imageUrl
                .map(url -> ProgramImageUtils.getProgramImageAltText(program, Locale.getDefault()))
                .orElse(null))
        .updatedDateTime(
            updatedTime.map(dateConverter::renderDateTimeHumanReadable).orElse("unknown"))
        .updatedDate(updatedTime.map(dateConverter::renderDate).orElse("unknown"))
        .blockCount(program.getBlockCount())
        .questionCount(program.getQuestionCount())
        .universalQuestionsText(universalQuestionText(program, universalQuestionIds).orElse(null))
        .translationComplete(params.isTranslationComplete().test(program))
        .rowActions(rowActions)
        .extraActions(extraActions)
        .build();
  }

  private static Optional<String> imageUrl(
      ProgramDefinition program, Function<String, String> publicImageUrl) {
    return program
        .summaryImageFileKey()
        .filter(PublicFileNameFormatter::isFileKeyForPublicProgramImage)
        .map(publicImageUrl);
  }

  private static ImmutableList<Action> draftRowActions(ProgramDefinition program) {
    return ImmutableList.of(
        Action.builder()
            .id(publishModalId(program) + "-button")
            .label("Publish")
            .iconFragment("iconPublish")
            .build(),
        Action.builder()
            .id("program-edit-link-" + program.id())
            .label("Edit")
            .iconFragment("iconEdit")
            .redirectUrl(routes.AdminProgramBlocksController.index(program.id()).url())
            .build());
  }

  private static ImmutableList<Action> draftRowExtraActions(
      ProgramDefinition program, Params params) {
    ImmutableList.Builder<Action> actions = ImmutableList.builder();

    manageProgramAdminsAction(program).ifPresent(actions::add);
    manageTranslationsAction(program, params).ifPresent(actions::add);
    if (!isExternal(program)) {
      actions.add(
          Action.builder()
              .label("Manage application statuses")
              .iconFragment("iconFlaky")
              .dropdownStyle(true)
              .redirectUrl(routes.AdminProgramStatusesController.index(program.id()).url())
              .build());
    }
    exportProgramAction(program).ifPresent(actions::add);

    return actions.build();
  }

  private static ImmutableList<Action> activeRowActions(ProgramDefinition program, Params params) {
    ImmutableList.Builder<Action> actions = ImmutableList.builder();

    actions.add(
        Action.builder()
            .id("program-view-link-" + program.id())
            .label("View")
            .iconFragment("iconView")
            .redirectUrl(routes.AdminProgramBlocksController.readOnlyIndex(program.id()).url())
            .build());
    if (!isExternal(program)) {
      actions.add(
          Action.builder()
              .label("Share link")
              .iconFragment("iconContentCopy")
              .copyableLink(
                  params.baseUrl()
                      + controllers.applicant.routes.ApplicantProgramsController.show(
                              program.slug())
                          .url())
              .build());
    }

    return actions.build();
  }

  private static ImmutableList<Action> activeRowExtraActions(
      ProgramDefinition program, boolean hasNoDraft, Params params) {
    ImmutableList.Builder<Action> actions = ImmutableList.builder();

    // External programs don't have applications hosted on CiviForm.
    if (!isExternal(program) && params.canViewApplications().test(program)) {
      actions.add(
          Action.builder()
              .label(
                  program.programType().equals(ProgramType.PRE_SCREENER_FORM)
                      ? "Forms"
                      : "Applications")
              .iconFragment("iconTextSnippet")
              .redirectUrl(applicationsUrl(program))
              .build());
    }

    // Editing an active program creates a new draft, so it's only offered when there is none yet.
    if (hasNoDraft) {
      actions.add(
          Action.builder()
              .id("program-new-version-link-" + program.id())
              .label("Edit")
              .iconFragment("iconEdit")
              .dropdownStyle(true)
              .postUrl(routes.AdminProgramController.newVersionFrom(program.id()).url())
              .formId("program-new-version-form-" + program.id())
              .build());
      manageTranslationsAction(program, params).ifPresent(actions::add);
    }
    manageProgramAdminsAction(program).ifPresent(actions::add);
    exportProgramAction(program).ifPresent(actions::add);

    return actions.build();
  }

  private static String applicationsUrl(ProgramDefinition program) {
    return routes.AdminApplicationController.index(
            program.id(),
            /* search= */ Optional.empty(),
            /* page= */ Optional.empty(),
            /* fromDate= */ Optional.empty(),
            /* untilDate= */ Optional.empty(),
            /* applicationStatus= */ Optional.empty(),
            /* selectedApplicationUri= */ Optional.empty(),
            /* showDownloadModal= */ Optional.empty(),
            /* message= */ Optional.empty())
        .url();
  }

  private static Optional<Action> manageTranslationsAction(
      ProgramDefinition program, Params params) {
    if (!params.hasTranslatableLocales()) {
      return Optional.empty();
    }

    return Optional.of(
        Action.builder()
            .id("program-translations-link-" + program.id())
            .label("Manage translations")
            .iconFragment("iconLanguage")
            .dropdownStyle(true)
            .redirectUrl(
                routes.AdminProgramTranslationsController.redirectToFirstLocale(program.adminName())
                    .url())
            .build());
  }

  private static Optional<Action> manageProgramAdminsAction(ProgramDefinition program) {
    // External programs don't have program administrators, since they cannot edit external
    // programs.
    if (isExternal(program)) {
      return Optional.empty();
    }

    return Optional.of(
        Action.builder()
            .id("manage-program-admin-link-" + program.id())
            .label("Manage program admins")
            .iconFragment("iconGroup")
            .dropdownStyle(true)
            .redirectUrl(routes.ProgramAdminManagementController.edit(program.id()).url())
            .build());
  }

  private static Optional<Action> exportProgramAction(ProgramDefinition program) {
    // External programs cannot be exported/imported.
    if (isExternal(program)) {
      return Optional.empty();
    }

    return Optional.of(
        Action.builder()
            .label("Export program")
            .iconFragment("iconDownload")
            .dropdownStyle(true)
            .redirectUrl(routes.AdminExportController.index(program.id()).url())
            .build());
  }

  private static boolean isExternal(ProgramDefinition program) {
    return program.programType().equals(ProgramType.EXTERNAL);
  }

  private static int countMissingUniversalQuestions(
      ProgramDefinition program, ImmutableList<Long> universalQuestionIds) {
    // External programs don't have questions, so universal questions don't apply to them.
    if (isExternal(program)) {
      return 0;
    }

    return (int)
        universalQuestionIds.stream()
            .filter(id -> !program.getQuestionIdsInProgram().contains(id))
            .count();
  }

  private static Optional<String> universalQuestionText(
      ProgramDefinition program, ImmutableList<Long> universalQuestionIds) {
    int universalCount = universalQuestionIds.size();
    if (isExternal(program) || universalCount == 0) {
      return Optional.empty();
    }

    int missingCount = countMissingUniversalQuestions(program, universalQuestionIds);
    String text =
        missingCount == 0 ? "all" : universalCount - missingCount + " of " + universalCount;

    return Optional.of("Contains " + text + " universal questions ");
  }

  private static ModalModel buildDemographicsModal() {
    return ModalModel.builder()
        .type(ModalModel.Type.DEMOGRAPHICS_CSV)
        .id("download-demographics-csv-modal")
        .title(DEMOGRAPHICS_ACTION_TEXT)
        .widthClass("lg:w-auto")
        .actionUrl(
            routes.AdminApplicationController.downloadDemographics(
                    /* fromDate= */ Optional.empty(), /* untilDate= */ Optional.empty())
                .url())
        .build();
  }

  private static String publishModalId(ProgramDefinition program) {
    return "publish-modal-" + program.slug();
  }

  private static ModalModel buildPublishProgramModal(
      ProgramDefinition program, ImmutableList<Long> universalQuestionIds) {
    String programName = program.localizedName().getDefault();
    String title =
        isExternal(program)
            ? "Are you sure you want to publish " + programName + "?"
            : "Are you sure you want to publish "
                + programName
                + " and all of its draft questions?";

    return ModalModel.builder()
        .type(ModalModel.Type.PUBLISH_PROGRAM)
        .id(publishModalId(program))
        .title(title)
        .widthClass("lg:w-1/3")
        .actionUrl(routes.AdminProgramController.publishProgram(program.id()).url())
        .missingUniversalQuestions(
            countMissingUniversalQuestions(program, universalQuestionIds) > 0)
        .build();
  }

  private Optional<ModalModel> buildPublishAllModal(
      Params params, ImmutableList<Long> universalQuestionIds) {
    // The publish-all modal and button are only shown if there is at least one draft.
    ActiveAndDraftQuestions questions = params.questions();
    if (!params.programs().anyDraft() && !questions.draftVersionHasAnyEdits()) {
      return Optional.empty();
    }

    ImmutableList<PublishAllProgram> draftPrograms =
        params.allDraftPrograms().get().stream()
            .sorted(Comparator.comparing(ProgramDefinition::adminName))
            .map(program -> buildPublishAllProgram(program, universalQuestionIds))
            .collect(ImmutableList.toImmutableList());
    ImmutableList<PublishAllQuestion> draftQuestions =
        questions.getDraftQuestions().stream()
            .sorted(Comparator.comparing(QuestionDefinition::getName))
            .map(ProgramIndexPageMapper::buildPublishAllQuestion)
            .collect(ImmutableList.toImmutableList());

    return Optional.of(
        ModalModel.builder()
            .type(ModalModel.Type.PUBLISH_ALL)
            .id("publish-all-programs-modal")
            .title("Do you want to publish all draft programs?")
            .widthClass("lg:w-auto")
            .actionUrl(routes.AdminProgramController.publish().url())
            .programsHeading(publishAllHeading(draftPrograms.size(), "program"))
            .draftPrograms(draftPrograms)
            .questionsHeading(publishAllHeading(draftQuestions.size(), "question"))
            .draftQuestions(draftQuestions)
            .build());
  }

  private static String publishAllHeading(int count, String noun) {
    return String.format("%d draft %s%s will be published:", count, noun, count == 1 ? "" : "s");
  }

  private static PublishAllProgram buildPublishAllProgram(
      ProgramDefinition program, ImmutableList<Long> universalQuestionIds) {
    String visibilityText =
        switch (program.displayMode()) {
          case DISABLED -> " (Hidden from applicants and Trusted Intermediaries) ";
          case HIDDEN_IN_INDEX -> " (Hidden from applicants) ";
          case PUBLIC -> " (Publicly visible) ";
          case SELECT_TI, TI_ONLY -> " ";
        };

    return PublishAllProgram.builder()
        .name(program.localizedName().getDefault())
        .visibilityText(visibilityText)
        .universalQuestionsText(universalQuestionText(program, universalQuestionIds).orElse(null))
        .editUrl(
            routes.AdminProgramController.edit(program.id(), ProgramEditStatus.EDIT.name()).url())
        .build();
  }

  private static PublishAllQuestion buildPublishAllQuestion(QuestionDefinition question) {
    return PublishAllQuestion.builder()
        .text(question.getQuestionText().getDefault())
        .adminName(question.getName())
        .editUrl(
            routes.AdminQuestionController.edit(
                    question.getId(), routes.AdminProgramController.index().url())
                .url())
        .build();
  }
}
