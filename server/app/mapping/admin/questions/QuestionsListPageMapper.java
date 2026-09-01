package mapping.admin.questions;

import com.google.common.base.CaseFormat;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Sets;
import controllers.admin.routes;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.function.Predicate;
import models.DisplayMode;
import repository.VersionRepository.PublishProgramPreview;
import services.DateConverter;
import services.DeletionStatus;
import services.RandomStringUtils;
import services.TranslationLocales;
import services.program.ProgramDefinition;
import services.question.ActiveAndDraftQuestions;
import services.question.types.QuestionDefinition;
import services.question.types.QuestionType;
import views.admin.questions.QuestionsListPageViewModel;
import views.admin.questions.QuestionsListPageViewModel.CreateQuestionOption;
import views.admin.questions.QuestionsListPageViewModel.ExtraAction;
import views.admin.questions.QuestionsListPageViewModel.ModalModel;
import views.admin.questions.QuestionsListPageViewModel.QuestionRow;
import views.admin.questions.QuestionsListPageViewModel.SortOption;
import views.admin.questions.QuestionsListPageViewModel.Toast;
import views.admin.questions.QuestionsListPageViewModel.VersionRow;
import views.components.QuestionSortOption;
import views.components.TextFormatter;

/** Maps data to the QuestionsListPageViewModel. */
public final class QuestionsListPageMapper {

  /** Builds the view model for the questions list page. */
  public QuestionsListPageViewModel map(
      ActiveAndDraftQuestions activeAndDraftQuestions,
      Optional<String> filter,
      TranslationLocales translationLocales,
      Predicate<QuestionDefinition> isTranslationComplete,
      DateConverter dateConverter,
      boolean enumeratorImprovementsEnabled,
      Optional<String> successMessage,
      Optional<String> errorMessage) {

    List<Toast> toasts = new ArrayList<>();
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

    ImmutableList<CardData> cards = buildSortedCards(activeAndDraftQuestions);

    List<QuestionRow> universalRows = new ArrayList<>();
    List<QuestionRow> otherRows = new ArrayList<>();
    List<QuestionRow> archivedRows = new ArrayList<>();
    List<ModalModel> modals = new ArrayList<>();
    for (CardData card : cards) {
      QuestionRow row =
          buildQuestionRow(
              card,
              activeAndDraftQuestions,
              translationLocales,
              isTranslationComplete,
              dateConverter,
              modals);
      if (isPendingDeletion(card.displayQuestion(), activeAndDraftQuestions)) {
        archivedRows.add(row);
      } else if (card.displayQuestion().isUniversal()) {
        universalRows.add(row);
      } else {
        otherRows.add(row);
      }
    }

    return QuestionsListPageViewModel.builder()
        .toasts(toasts)
        .filterValue(filter.orElse(""))
        .createQuestionOptions(buildCreateQuestionOptions(enumeratorImprovementsEnabled))
        .sortOptions(buildSortOptions())
        .universalRows(universalRows)
        .otherRows(otherRows)
        .archivedRows(archivedRows)
        .totalQuestionCount(activeAndDraftQuestions.getQuestionNames().size())
        .modals(modals)
        .build();
  }

  /**
   * Builds the "Create new question" dropdown entries, mirroring
   * CreateQuestionButton.renderCreateQuestionButtonForQuestionListPage.
   */
  private ImmutableList<CreateQuestionOption> buildCreateQuestionOptions(
      boolean enumeratorImprovementsEnabled) {
    String redirectUrl = routes.AdminQuestionController.index(Optional.empty()).url();
    ImmutableList.Builder<CreateQuestionOption> options = ImmutableList.builder();

    for (QuestionType type : QuestionType.values()) {
      if (type == QuestionType.NULL_QUESTION) {
        continue;
      }
      if (type == QuestionType.ENUMERATOR && enumeratorImprovementsEnabled) {
        continue;
      }
      String typeString = type.toString().toLowerCase(Locale.ROOT);
      options.add(
          CreateQuestionOption.builder()
              .id(String.format("create-%s-question", typeString))
              .url(
                  routes.AdminQuestionController.newOne(
                          typeString,
                          redirectUrl,
                          /* enumeratorQuestion= */ Optional.empty(),
                          /* isRepeatingBlock= */ Optional.of("true"))
                      .url())
              .label(type.getLabel())
              .iconFragment(questionTypeIconFragment(type))
              .svgLinkId(String.format("svg-link-%s", questionTypeIconName(type)))
              .build());
    }

    return options.build();
  }

  private ImmutableList<SortOption> buildSortOptions() {
    ImmutableList<QuestionSortOption> sortOptions =
        ImmutableList.of(
            QuestionSortOption.LAST_MODIFIED,
            QuestionSortOption.ADMIN_NAME,
            QuestionSortOption.NUM_PROGRAMS);
    ImmutableList.Builder<SortOption> builder = ImmutableList.builder();
    boolean first = true;

    for (QuestionSortOption sortOption : sortOptions) {
      for (var optionValue : sortOption.getSelectOptions()) {
        builder.add(
            SortOption.builder()
                .value(optionValue.value())
                .label(optionValue.label())
                // The first option is the default sort order.
                .selected(first)
                .build());
        first = false;
      }
    }

    return builder.build();
  }

  private ImmutableList<CardData> buildSortedCards(
      ActiveAndDraftQuestions activeAndDraftQuestions) {
    return activeAndDraftQuestions.getQuestionNames().stream()
        .map(
            name -> {
              ActiveAndDraftQuestions.ReferencingPrograms referencingPrograms =
                  activeAndDraftQuestions.getReferencingPrograms(name);
              return new CardData(
                  activeAndDraftQuestions.getActiveQuestionDefinition(name),
                  activeAndDraftQuestions.getDraftQuestionDefinition(name),
                  createReferencingPrograms(
                      referencingPrograms.activeReferences(),
                      referencingPrograms.draftReferences()));
            })
        .sorted(
            Comparator.<CardData, Boolean>comparing(card -> card.displayQuestion().isUniversal())
                .thenComparing(
                    card -> card.displayQuestion().getLastModifiedTime().orElse(Instant.EPOCH))
                .reversed()
                .thenComparing(
                    card ->
                        card.displayQuestion()
                            .getQuestionText()
                            .getDefault()
                            .toLowerCase(Locale.ROOT)))
        .collect(ImmutableList.toImmutableList());
  }

  private QuestionRow buildQuestionRow(
      CardData card,
      ActiveAndDraftQuestions activeAndDraftQuestions,
      TranslationLocales translationLocales,
      Predicate<QuestionDefinition> isTranslationComplete,
      DateConverter dateConverter,
      List<ModalModel> modals) {
    Preconditions.checkArgument(
        card.draftQuestion().isPresent() || card.activeQuestion().isPresent(),
        "Did not receive a valid question.");

    QuestionDefinition latestDefinition = card.displayQuestion();

    Optional<ModalModel> referencingModal =
        makeReferencingProgramsModal(
            latestDefinition.getName(),
            card.referencingPrograms(),
            /* showCantArchiveHeader= */ false);
    referencingModal.ifPresent(modals::add);

    VersionRow draftRow = null;
    if (card.draftQuestion().isPresent()) {
      draftRow =
          buildVersionRow(
              /* isActive= */ false,
              card,
              activeAndDraftQuestions,
              translationLocales,
              isTranslationComplete,
              dateConverter,
              modals);
    }

    VersionRow activeRow = null;
    if (card.activeQuestion().isPresent()) {
      activeRow =
          buildVersionRow(
              /* isActive= */ true,
              card,
              activeAndDraftQuestions,
              translationLocales,
              isTranslationComplete,
              dateConverter,
              modals);
    }

    String universalBadgeText = null;
    if (latestDefinition.isUniversal()) {
      universalBadgeText =
          String.format(
              "Universal %s question",
              latestDefinition.getQuestionType().getLabel().toLowerCase(Locale.getDefault()));
    }

    String helpText =
        latestDefinition.getQuestionHelpText().isEmpty()
            ? ""
            : latestDefinition.getQuestionHelpText().getDefault();

    return QuestionRow.builder()
        .universalBadgeText(universalBadgeText)
        .iconFragment(questionTypeIconFragment(latestDefinition.getQuestionType()))
        .questionTextHtml(formatForAdmins(latestDefinition.getQuestionText().getDefault()))
        .helpTextHtml(formatForAdmins(helpText))
        .adminName(latestDefinition.getName())
        .adminDescription(latestDefinition.getDescription())
        .lastModified(latestDefinition.getLastModifiedTime().orElse(Instant.EPOCH).toString())
        .numReferencingPrograms(card.referencingPrograms().getTotalNumReferencingPrograms())
        .referencingLines(buildReferencingLines(card.referencingPrograms()))
        .seeListTriggerId(referencingModal.map(modal -> modal.getId() + "-button").orElse(null))
        .draftRow(draftRow)
        .activeRow(activeRow)
        .build();
  }

  private VersionRow buildVersionRow(
      boolean isActive,
      CardData card,
      ActiveAndDraftQuestions activeAndDraftQuestions,
      TranslationLocales translationLocales,
      Predicate<QuestionDefinition> isTranslationComplete,
      DateConverter dateConverter,
      List<ModalModel> modals) {
    QuestionDefinition question =
        isActive ? card.activeQuestion().get() : card.draftQuestion().get();
    boolean isSecondRow =
        isActive
            && activeAndDraftQuestions.getDraftQuestionDefinition(question.getName()).isPresent();

    // Some actions such as "edit" or "archive" are rendered only on one of the
    // two rows: when both a draft and an active version exist, only the draft
    // row gets them.
    boolean isEditable =
        !isActive
            || activeAndDraftQuestions.getDraftQuestionDefinition(question.getName()).isEmpty();

    List<ExtraAction> extraActions = new ArrayList<>();
    boolean hasTranslations = !translationLocales.translatableLocales().isEmpty();
    if (!isActive) {
      if (hasTranslations) {
        extraActions.add(translateAction(question));
      }
      if (activeAndDraftQuestions.getActiveQuestionDefinition(question.getName()).isPresent()) {
        // The discard modal id is fixed, as in the legacy view: when several
        // questions have discardable drafts, the ids are duplicated and only
        // the first modal is wired up.
        modals.add(
            ModalModel.builder()
                .type(ModalModel.Type.DISCARD_DRAFT)
                .id("discard-confirmation-modal")
                .title("Discard draft?")
                .widthClass("lg:w-1/4")
                .discardUrl(routes.AdminQuestionController.discardDraft(question.getId()).url())
                .discardFormId(RandomStringUtils.randomAlphabetic(32))
                .build());
        extraActions.add(
            ExtraAction.builder()
                .kind(ExtraAction.Kind.DISCARD)
                .triggerButtonId("discard-confirmation-modal-button")
                .build());
      }
    }
    if (isActive && isEditable && hasTranslations) {
      extraActions.add(translateAction(question));
    }
    if (isEditable) {
      extraActions.add(buildArchiveAction(card, question, activeAndDraftQuestions, modals));
    }

    String badgeText;
    String badgeBgClass;
    String badgeTextClass;
    if (isActive) {
      badgeText = "Active";
      badgeBgClass = "bg-civiform-green-light";
      badgeTextClass = "text-civiform-green";
    } else if (isPendingDeletion(question, activeAndDraftQuestions)) {
      badgeText = "Archived";
      badgeBgClass = "bg-civiform-yellow-light";
      badgeTextClass = "text-civiform-yellow";
    } else {
      badgeText = "Draft";
      badgeBgClass = "bg-civiform-purple-light";
      badgeTextClass = "text-civiform-purple";
    }

    Optional<Instant> lastModified = question.getLastModifiedTime();

    return VersionRow.builder()
        .secondRow(isSecondRow)
        .badgeText(badgeText)
        .badgeBgClass(badgeBgClass)
        .badgeTextClass(badgeTextClass)
        .editedOnTimeText(
            lastModified.map(dateConverter::renderDateTimeHumanReadable).orElse("unknown"))
        .editedOnDateText(lastModified.map(dateConverter::renderDate).orElse("unknown"))
        .translationComplete(isTranslationComplete.test(question))
        .editUrl(routes.AdminQuestionController.edit(question.getId(), /* redirectUrl= */ "").url())
        .editVisible(isEditable)
        .extraActionsButtonId("extra-actions-uuid-" + UUID.randomUUID())
        .extraActions(extraActions)
        .build();
  }

  private ExtraAction translateAction(QuestionDefinition question) {
    return ExtraAction.builder()
        .kind(ExtraAction.Kind.TRANSLATE)
        .url(
            routes.AdminQuestionTranslationsController.redirectToFirstLocale(question.getName())
                .url())
        .build();
  }

  private ExtraAction buildArchiveAction(
      CardData card,
      QuestionDefinition question,
      ActiveAndDraftQuestions activeAndDraftQuestions,
      List<ModalModel> modals) {
    return switch (activeAndDraftQuestions.getDeletionStatus(question.getName())) {
      case PENDING_DELETION ->
          ExtraAction.builder()
              .kind(ExtraAction.Kind.RESTORE)
              .url(routes.AdminQuestionController.restore(question.getId()).url())
              .formId(RandomStringUtils.randomAlphabetic(32))
              .build();
      case DELETABLE ->
          ExtraAction.builder()
              .kind(ExtraAction.Kind.ARCHIVE)
              .url(routes.AdminQuestionController.archive(question.getId()).url())
              .formId(RandomStringUtils.randomAlphabetic(32))
              .build();
      case NOT_ACTIVE, NOT_DELETABLE -> {
        Optional<ModalModel> cantArchiveModal =
            makeReferencingProgramsModal(
                question.getName(), card.referencingPrograms(), /* showCantArchiveHeader= */ true);
        // The legacy view unconditionally dereferenced the modal here.
        Preconditions.checkState(
            cantArchiveModal.isPresent(),
            "Expected referencing programs for a question that cannot be archived.");
        modals.add(cantArchiveModal.get());

        yield ExtraAction.builder()
            .kind(ExtraAction.Kind.ARCHIVE_BLOCKED)
            .triggerButtonId(cantArchiveModal.get().getId() + "-button")
            .build();
      }
    };
  }

  private ImmutableList<String> buildReferencingLines(GroupedReferencingPrograms grouped) {
    if (grouped.isEmpty()) {
      return ImmutableList.of("Used in 0 programs.");
    }

    ImmutableList.Builder<String> lines = ImmutableList.builder();
    if (!grouped.usedPrograms().isEmpty()) {
      lines.add(formatReferencingProgramsText("Used in", grouped.usedPrograms().size(), "program"));
    }
    if (!grouped.addedPrograms().isEmpty()) {
      lines.add(
          formatReferencingProgramsText(
              "Added to", grouped.addedPrograms().size(), "program in use"));
    }
    if (!grouped.removedPrograms().isEmpty()) {
      lines.add(
          formatReferencingProgramsText(
              "Removed from", grouped.removedPrograms().size(), "program"));
    }
    if (!grouped.disabledPrograms().isEmpty()) {
      lines.add(
          formatReferencingProgramsText(
              "Added to ", grouped.disabledPrograms().size(), "disabled program"));
    }

    return lines.build();
  }

  private static String formatReferencingProgramsText(
      String prefix, int numPrograms, String suffix) {
    return String.format(
        "%s %d %s.",
        prefix,
        numPrograms,
        (numPrograms > 1 ? suffix.replaceAll("\\bprogram\\b", "programs") : suffix));
  }

  private Optional<ModalModel> makeReferencingProgramsModal(
      String questionName, GroupedReferencingPrograms grouped, boolean showCantArchiveHeader) {
    if (grouped.isEmpty()) {
      return Optional.empty();
    }

    return Optional.of(
        ModalModel.builder()
            .type(ModalModel.Type.REFERENCING_PROGRAMS)
            .id("uuid-" + UUID.randomUUID())
            .title(String.format("Programs referencing %s", questionName))
            .widthClass("lg:w-1/2")
            .showCantArchiveHeader(showCantArchiveHeader)
            .usedProgramNames(defaultNames(grouped.usedPrograms()))
            .addedProgramNames(defaultNames(grouped.addedPrograms()))
            .removedProgramNames(
                grouped.removedPrograms().stream()
                    .map(program -> program.localizedName().getDefault())
                    .collect(ImmutableList.toImmutableList()))
            .build());
  }

  private static ImmutableList<String> defaultNames(List<PublishProgramPreview> programs) {
    return programs.stream()
        .map(program -> program.localizedName().getDefault())
        .collect(ImmutableList.toImmutableList());
  }

  /**
   * The legacy icon name for a question type: the Icons enum constant lowercased, mirroring
   * Icons.getIconTypeFromQuestionType.
   */
  private static String questionTypeIconName(QuestionType type) {
    return switch (type) {
      case ADDRESS -> "address";
      case CHECKBOX -> "checkbox";
      case CURRENCY -> "currency";
      case DATE -> "date";
      case DROPDOWN -> "dropdown";
      case EMAIL -> "email";
      case FILEUPLOAD -> "fileupload";
      case ID -> "id";
      case MAP -> "map";
      case NAME -> "name";
      case NUMBER -> "number";
      case RADIO_BUTTON, YES_NO -> "radio_button";
      case ENUMERATOR -> "enumerator";
      case STATIC -> "annotation";
      case TEXT -> "text";
      case PHONE -> "phone";
      default -> "unknown";
    };
  }

  /** The question type's icon fragment name in LegacySvgFragments.html ("iconAddress", ...). */
  private static String questionTypeIconFragment(QuestionType type) {
    return "icon"
        + CaseFormat.LOWER_UNDERSCORE.to(CaseFormat.UPPER_CAMEL, questionTypeIconName(type));
  }

  private static String formatForAdmins(String text) {
    // Same parameters as TextFormatter.formatTextForAdmins, which the legacy
    // view used.
    return TextFormatter.formatTextToSanitizedHTML(
        text,
        /* preserveEmptyLines= */ false,
        /* addRequiredIndicator= */ false,
        /* ariaLabelForNewTabs= */ "opens in a new tab");
  }

  private static boolean isPendingDeletion(
      QuestionDefinition question, ActiveAndDraftQuestions activeAndDraftQuestions) {
    return activeAndDraftQuestions.getDeletionStatus(question.getName())
        == DeletionStatus.PENDING_DELETION;
  }

  /** Groups the referencing programs the way the legacy view grouped them. */
  record GroupedReferencingPrograms(
      ImmutableList<PublishProgramPreview> usedPrograms,
      ImmutableList<PublishProgramPreview> addedPrograms,
      ImmutableList<ProgramDefinition> removedPrograms,
      ImmutableList<PublishProgramPreview> disabledPrograms) {

    boolean isEmpty() {
      return usedPrograms.isEmpty()
          && addedPrograms.isEmpty()
          && removedPrograms.isEmpty()
          && disabledPrograms.isEmpty();
    }

    int getTotalNumReferencingPrograms() {
      return usedPrograms.size() + addedPrograms.size();
    }
  }

  private GroupedReferencingPrograms createReferencingPrograms(
      Set<ProgramDefinition> activePrograms, Set<PublishProgramPreview> draftPrograms) {
    ImmutableMap<String, ProgramDefinition> activeProgramsMap =
        activePrograms.stream()
            .collect(
                ImmutableMap.toImmutableMap(ProgramDefinition::adminName, Function.identity()));

    ImmutableMap<String, PublishProgramPreview> draftDisabledProgramsMap =
        draftPrograms.stream()
            .filter(program -> program.displayMode() == DisplayMode.DISABLED)
            .collect(
                ImmutableMap.toImmutableMap(PublishProgramPreview::adminName, Function.identity()));

    ImmutableMap<String, PublishProgramPreview> draftProgramsMap =
        draftPrograms.stream()
            .collect(
                ImmutableMap.toImmutableMap(PublishProgramPreview::adminName, Function.identity()));

    // Use set operations to collect programs into 4 sets.
    Set<String> usedSet = Sets.intersection(activeProgramsMap.keySet(), draftProgramsMap.keySet());
    Set<String> addedSet = Sets.difference(draftProgramsMap.keySet(), activeProgramsMap.keySet());
    addedSet = Sets.difference(addedSet, draftDisabledProgramsMap.keySet());
    Set<String> removedSet = Sets.difference(activeProgramsMap.keySet(), draftProgramsMap.keySet());
    Set<String> disabledSet =
        Sets.difference(draftDisabledProgramsMap.keySet(), activeProgramsMap.keySet());

    ImmutableList<PublishProgramPreview> usedPrograms =
        usedSet.stream()
            .map(draftProgramsMap::get)
            .sorted(Comparator.comparing(PublishProgramPreview::adminName))
            .collect(ImmutableList.toImmutableList());
    ImmutableList<PublishProgramPreview> addedPrograms =
        addedSet.stream()
            .map(draftProgramsMap::get)
            .sorted(Comparator.comparing(PublishProgramPreview::adminName))
            .collect(ImmutableList.toImmutableList());
    ImmutableList<ProgramDefinition> removedPrograms =
        removedSet.stream()
            .map(activeProgramsMap::get)
            .sorted(Comparator.comparing(ProgramDefinition::adminName))
            .collect(ImmutableList.toImmutableList());
    ImmutableList<PublishProgramPreview> disabledPrograms =
        disabledSet.stream()
            .map(draftDisabledProgramsMap::get)
            .sorted(Comparator.comparing(PublishProgramPreview::adminName))
            .collect(ImmutableList.toImmutableList());

    return new GroupedReferencingPrograms(
        usedPrograms, addedPrograms, removedPrograms, disabledPrograms);
  }

  private record CardData(
      Optional<QuestionDefinition> activeQuestion,
      Optional<QuestionDefinition> draftQuestion,
      GroupedReferencingPrograms referencingPrograms) {
    QuestionDefinition displayQuestion() {
      return draftQuestion.orElseGet(activeQuestion::get);
    }
  }
}
