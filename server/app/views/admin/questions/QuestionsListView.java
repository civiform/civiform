package views.admin.questions;

import static com.google.common.base.Preconditions.checkNotNull;
import static j2html.TagCreator.a;
import static j2html.TagCreator.br;
import static j2html.TagCreator.div;
import static j2html.TagCreator.each;
import static j2html.TagCreator.h1;
import static j2html.TagCreator.h2;
import static j2html.TagCreator.li;
import static j2html.TagCreator.p;
import static j2html.TagCreator.span;
import static j2html.TagCreator.ul;

import com.google.auto.value.AutoValue;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Sets;
import com.google.inject.Inject;
import j2html.tags.DomContent;
import j2html.tags.specialized.ButtonTag;
import j2html.tags.specialized.DivTag;
import j2html.tags.specialized.PTag;
import java.time.Instant;
import java.util.Collection;
import java.util.Comparator;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import models.DisplayMode;
import org.apache.commons.lang3.tuple.Pair;
import play.mvc.Http;
import play.twirl.api.Content;
import services.DeletionStatus;
import services.TranslationLocales;
import services.program.ProgramDefinition;
import services.question.ActiveAndDraftQuestions;
import services.question.types.QuestionDefinition;
import services.settings.SettingsManifest;
import views.AlertComponent;
import views.BaseHtmlView;
import views.HtmlBundle;
import views.ViewUtils;
import views.ViewUtils.ProgramDisplayType;
import views.admin.AdminLayout;
import views.admin.AdminLayout.NavPage;
import views.admin.AdminLayoutFactory;
import views.components.ButtonStyles;
import views.components.CreateQuestionButton;
import views.components.Icons;
import views.components.Modal;
import views.components.Modal.Width;
import views.components.QuestionBank;
import views.components.QuestionSortOption;
import views.components.TextFormatter;
import views.style.AdminStyles;
import views.style.BaseStyles;
import views.style.ReferenceClasses;
import views.style.StyleUtils;

/** Renders a page for viewing all active questions and draft questions. */
public final class QuestionsListView extends BaseHtmlView {

  private final AdminLayout layout;
  private final TranslationLocales translationLocales;
  private final ViewUtils viewUtils;
  private final SettingsManifest settingsManifest;

  @Inject
  public QuestionsListView(
      AdminLayoutFactory layoutFactory,
      TranslationLocales translationLocales,
      ViewUtils viewUtils,
      SettingsManifest settingsManifest) {
    this.layout = checkNotNull(layoutFactory).getLayout(NavPage.QUESTIONS);
    this.translationLocales = checkNotNull(translationLocales);
    this.viewUtils = checkNotNull(viewUtils);
    this.settingsManifest = checkNotNull(settingsManifest);
  }

  /**
   * Renders a page with an (optionally filtered) list view of all questions.
   *
   * @param activeAndDraftQuestions a list of all active and draft questions, including the programs
   *     that use them.
   * @param filter an optional filter with which to render the question bank. This filter will
   *     pre-populate the search bar, and can be overriden by the user at any point.
   * @param request the HTTP request
   * @return the rendered page
   */
  public Content render(
      ActiveAndDraftQuestions activeAndDraftQuestions,
      Optional<String> filter,
      Http.Request request) {
    String title = "All questions";

    Pair<DivTag, ImmutableList<Modal>> questionRowsAndModals =
        renderAllQuestionRows(activeAndDraftQuestions, request);

    DivTag contentDiv =
        div()
            .withClasses("px-4")
            .with(
                div()
                    .withClasses("content-div", "space-x-4")
                    .with(
                        h1(title),
                        div().withClass("flex-grow"),
                        CreateQuestionButton.renderCreateQuestionButton(
                            controllers.admin.routes.AdminQuestionController.index(Optional.empty())
                                .url(),
                            /* isPrimaryButton= */ true,
                            settingsManifest)),
                QuestionBank.renderFilterAndSort(
                    ImmutableList.of(
                        QuestionSortOption.LAST_MODIFIED,
                        QuestionSortOption.ADMIN_NAME,
                        QuestionSortOption.NUM_PROGRAMS),
                    filter))
            .with(div().withClass("mt-6").with(questionRowsAndModals.getLeft()))
            .with(renderSummary(activeAndDraftQuestions));
    HtmlBundle htmlBundle =
        layout
            .getBundle(request)
            .setTitle(title)
            .addModals(questionRowsAndModals.getRight())
            .addMainContent(contentDiv);
    addSuccessAndErrorToasts(htmlBundle, request.flash());
    return layout.renderCentered(htmlBundle);
  }

  private DivTag renderSummary(ActiveAndDraftQuestions activeAndDraftQuestions) {
    // The total question count should be equivalent to the number of rows in the
    // displayed table, where we have a single entry for a question that is active
    // and has a draft.
    return div(String.format(
            "Total questions: %d", activeAndDraftQuestions.getQuestionNames().size()))
        .withClasses("question-summary");
  }

  private static QuestionDefinition getDisplayQuestion(QuestionCardData cardData) {
    return cardData.draftQuestion().orElseGet(cardData.activeQuestion()::get);
  }

  private static boolean isQuestionPendingDeletion(
      QuestionCardData card, ActiveAndDraftQuestions activeAndDraftQuestions) {
    return isQuestionPendingDeletion(getDisplayQuestion(card), activeAndDraftQuestions);
  }

  private static boolean isQuestionPendingDeletion(
      QuestionDefinition question, ActiveAndDraftQuestions activeAndDraftQuestions) {
    return activeAndDraftQuestions.getDeletionStatus(question.getName())
        == DeletionStatus.PENDING_DELETION;
  }

  private Pair<DivTag, ImmutableList<Modal>> renderAllQuestionRows(
      ActiveAndDraftQuestions activeAndDraftQuestions, Http.Request request) {
    ImmutableList<QuestionCardData> cards =
        activeAndDraftQuestions.getQuestionNames().stream()
            .map(
                name -> {
                  ActiveAndDraftQuestions.ReferencingPrograms referencingPrograms =
                      activeAndDraftQuestions.getReferencingPrograms(name);
                  return QuestionCardData.builder()
                      .setActiveQuestion(activeAndDraftQuestions.getActiveQuestionDefinition(name))
                      .setDraftQuestion(activeAndDraftQuestions.getDraftQuestionDefinition(name))
                      .setReferencingPrograms(
                          createReferencingPrograms(
                              referencingPrograms.activeReferences(),
                              referencingPrograms.draftReferences()))
                      .build();
                })
            .sorted(
                Comparator.<QuestionCardData, Boolean>comparing(
                        card -> getDisplayQuestion(card).isUniversal())
                    .thenComparing(
                        card ->
                            getDisplayQuestion(card).getLastModifiedTime().orElse(Instant.EPOCH))
                    .reversed()
                    .thenComparing(
                        card ->
                            getDisplayQuestion(card)
                                .getQuestionText()
                                .getDefault()
                                .toLowerCase(Locale.ROOT)))
            .collect(ImmutableList.toImmutableList());

    ImmutableList.Builder<DomContent> universalQuestionRows = ImmutableList.builder();
    ImmutableList.Builder<DomContent> nonArchivedQuestionRows = ImmutableList.builder();
    ImmutableList.Builder<DomContent> archivedQuestionRows = ImmutableList.builder();
    ImmutableList.Builder<Modal> modals = ImmutableList.builder();
    for (QuestionCardData card : cards) {
      Pair<DivTag, ImmutableList<Modal>> rowAndModals =
          renderQuestionCard(card, activeAndDraftQuestions, request);
      if (isQuestionPendingDeletion(card, activeAndDraftQuestions)) {
        archivedQuestionRows.add(rowAndModals.getLeft());
      } else if (getDisplayQuestion(card).isUniversal()) {
        universalQuestionRows.add(rowAndModals.getLeft());
      } else {
        nonArchivedQuestionRows.add(rowAndModals.getLeft());
      }
      modals.addAll(rowAndModals.getRight());
    }

    ImmutableList<DomContent> universalQuestionContent = universalQuestionRows.build();
    ImmutableList<DomContent> nonArchivedQuestionContent = nonArchivedQuestionRows.build();
    ImmutableList<DomContent> archivedQuestionContent = archivedQuestionRows.build();
    DivTag questionContent = div();
    if (!universalQuestionContent.isEmpty()) {
      questionContent.with(
          div()
              .withId("questions-list-universal")
              .withClasses(ReferenceClasses.SORTABLE_QUESTIONS_CONTAINER)
              .with(h2("Universal questions").withClasses(AdminStyles.SEMIBOLD_HEADER))
              .with(
                  AlertComponent.renderSlimInfoAlert(
                      "We recommend using Universal questions in your program for all personal and"
                          + " contact information questions."))
              .with(universalQuestionContent));
    }
    questionContent.with(
        div()
            .withId("questions-list-non-universal")
            .withClasses(ReferenceClasses.SORTABLE_QUESTIONS_CONTAINER)
            .condWith(
                !universalQuestionContent.isEmpty(),
                h2("All other questions").withClasses(AdminStyles.SEMIBOLD_HEADER))
            .with(nonArchivedQuestionContent));
    if (!archivedQuestionContent.isEmpty()) {
      questionContent.with(
          div()
              .withId("questions-list-archived")
              .with(h2("Marked for archival").withClasses(AdminStyles.SEMIBOLD_HEADER))
              .with(archivedQuestionContent));
    }
    return Pair.of(questionContent, modals.build());
  }

  @AutoValue
  abstract static class QuestionCardData {
    abstract Optional<QuestionDefinition> activeQuestion();

    abstract Optional<QuestionDefinition> draftQuestion();

    abstract GroupedReferencingPrograms referencingPrograms();

    static Builder builder() {
      return new AutoValue_QuestionsListView_QuestionCardData.Builder();
    }

    @AutoValue.Builder
    abstract static class Builder {
      abstract Builder setActiveQuestion(Optional<QuestionDefinition> v);

      abstract Builder setDraftQuestion(Optional<QuestionDefinition> v);

      abstract Builder setReferencingPrograms(GroupedReferencingPrograms v);

      abstract QuestionCardData build();
    }
  }

  /**
   * Renders a card in the question list. The card contains question text, help text, active and
   * draft versions, list of programs questions is used in and buttons to edit the question.
   */
  private Pair<DivTag, ImmutableList<Modal>> renderQuestionCard(
      QuestionCardData cardData,
      ActiveAndDraftQuestions activeAndDraftQuestions,
      Http.Request request) {
    if (cardData.draftQuestion().isEmpty() && cardData.activeQuestion().isEmpty()) {
      throw new IllegalArgumentException("Did not receive a valid question.");
    }
    QuestionDefinition latestDefinition =
        cardData.draftQuestion().orElseGet(cardData.activeQuestion()::get);

    ImmutableList.Builder<Modal> modals = ImmutableList.builder();
    Pair<DivTag, ImmutableList<Modal>> referencingProgramAndModal =
        renderReferencingPrograms(latestDefinition.getName(), cardData.referencingPrograms());
    modals.addAll(referencingProgramAndModal.getRight());

    DivTag row =
        div()
            .withClasses("flex")
            .with(renderInfoCell(latestDefinition))
            .with(referencingProgramAndModal.getLeft());

    DivTag draftAndActiveRows = div().withClasses("flex-grow");
    if (cardData.draftQuestion().isPresent()) {
      Pair<DivTag, ImmutableList<Modal>> draftRow =
          renderActiveOrDraftRow(/* isActive= */ false, cardData, activeAndDraftQuestions, request);
      modals.addAll(draftRow.getRight());
      draftAndActiveRows.with(draftRow.getLeft());
    }
    if (cardData.activeQuestion().isPresent()) {
      Pair<DivTag, ImmutableList<Modal>> activeRow =
          renderActiveOrDraftRow(/* isActive= */ true, cardData, activeAndDraftQuestions, request);
      modals.addAll(activeRow.getRight());
      draftAndActiveRows.with(activeRow.getLeft());
    }
    row.with(draftAndActiveRows);

    DivTag adminNote =
        div()
            .withClasses("py-7")
            .with(
                span("Admin ID: ").withClasses("font-bold"),
                span(latestDefinition.getName()),
                br(),
                span("Admin note: ").withClasses("font-bold"),
                span(latestDefinition.getDescription()));

    DivTag rowWithAdminNote =
        div()
            .withClasses(
                ReferenceClasses.QUESTION_BANK_ELEMENT,
                "question-bank-element",
                ReferenceClasses.ADMIN_QUESTION_TABLE_ROW)
            .condWith(
                getDisplayQuestion(cardData).isUniversal(),
                ViewUtils.makeUniversalBadge(latestDefinition, "mt-4"))
            .with(row)
            .with(adminNote)
            // Add data attributes used for sorting.
            .withData(QuestionSortOption.ADMIN_NAME.getDataAttribute(), latestDefinition.getName())
            .withData(
                QuestionSortOption.LAST_MODIFIED.getDataAttribute(),
                latestDefinition.getLastModifiedTime().orElse(Instant.EPOCH).toString())
            .withData(
                QuestionSortOption.NUM_PROGRAMS.getDataAttribute(),
                Integer.toString(cardData.referencingPrograms().getTotalNumReferencingPrograms()));
    return Pair.of(rowWithAdminNote, modals.build());
  }

  /**
   * Renders single "draft" or "active" row within a question row. Question can have up to two such
   * rows, one "draft" and one "active".
   */
  private Pair<DivTag, ImmutableList<Modal>> renderActiveOrDraftRow(
      boolean isActive,
      QuestionCardData cardData,
      ActiveAndDraftQuestions activeAndDraftQuestions,
      Http.Request request) {
    QuestionDefinition question =
        isActive ? cardData.activeQuestion().get() : cardData.draftQuestion().get();
    boolean isSecondRow =
        isActive
            && activeAndDraftQuestions.getDraftQuestionDefinition(question.getName()).isPresent();

    Pair<DivTag, ImmutableList<Modal>> actionsCellAndModal =
        renderActionsCell(isActive, cardData, activeAndDraftQuestions, request);

    ProgramDisplayType displayType = ProgramDisplayType.ACTIVE;
    if (!isActive) {
      displayType =
          isQuestionPendingDeletion(question, activeAndDraftQuestions)
              ? ProgramDisplayType.PENDING_DELETION
              : ProgramDisplayType.DRAFT;
    }
    PTag badge =
        ViewUtils.makeLifecycleBadge(displayType, "ml-2", StyleUtils.responsiveXLarge("ml-8"));

    DivTag row =
        div()
            .withClasses("py-7", "row-element", isSecondRow ? "border-t" : "")
            .with(badge)
            .with(div().withClasses("flex-grow"))
            .with(
                div()
                    .withClasses("ml-4", StyleUtils.responsiveXLarge("ml-10"))
                    .with(viewUtils.renderEditOnText("Edited on ", question.getLastModifiedTime())))
            .with(actionsCellAndModal.getLeft());

    asRedirectElement(
        row, controllers.admin.routes.AdminQuestionController.show(question.getId()).url());

    return Pair.of(row, actionsCellAndModal.getRight());
  }

  private DivTag renderInfoCell(QuestionDefinition definition) {
    DivTag questionText =
        div()
            .withClasses("question-bank-info-cell")
            .with(
                Icons.questionTypeSvg(definition.getQuestionType())
                    .withClasses("w-6", "h-6", "shrink-0"))
            .with(
                div()
                    .with(
                        TextFormatter.formatTextForAdmins(
                            definition.getQuestionText().getDefault()))
                    .withClasses(
                        ReferenceClasses.ADMIN_QUESTION_TITLE,
                        "pl-4",
                        "text-xl",
                        "break-words",
                        "max-w-full"));
    String questionDescriptionString =
        definition.getQuestionHelpText().isEmpty()
            ? ""
            : definition.getQuestionHelpText().getDefault();
    DivTag questionDescription =
        div(
            div()
                .with(TextFormatter.formatTextForAdmins(questionDescriptionString))
                .withClasses("pl-10"));
    return div()
        .withClasses("py-7", "w-1/4", "flex", "flex-col", "justify-between")
        .with(div().with(questionText).with(questionDescription));
  }

  /**
   * Renders text describing how programs use specified question and provides a link to show dialog
   * listing all such programs.
   */
  private Pair<DivTag, ImmutableList<Modal>> renderReferencingPrograms(
      String questionName, GroupedReferencingPrograms groupedReferencingPrograms) {
    Optional<Modal> maybeReferencingProgramsModal =
        makeReferencingProgramsModal(
            questionName, groupedReferencingPrograms, /* modalHeader= */ Optional.empty());

    DivTag tag =
        div()
            .withClasses(
                ReferenceClasses.ADMIN_QUESTION_PROGRAM_REFERENCE_COUNTS,
                "ml-4",
                StyleUtils.responsiveXLarge("ml-10"),
                "py-7",
                "w-1/4");
    if (groupedReferencingPrograms.isEmpty()) {
      tag.with(p("Used in 0 programs."));
    } else {
      if (!groupedReferencingPrograms.usedPrograms().isEmpty()) {
        int numPrograms = groupedReferencingPrograms.usedPrograms().size();
        tag.with(p(formatReferencingProgramsText("Used in", numPrograms, "program")));
      }

      if (!groupedReferencingPrograms.addedPrograms().isEmpty()) {
        int numPrograms = groupedReferencingPrograms.addedPrograms().size();
        tag.with(p(formatReferencingProgramsText("Added to", numPrograms, "program in use")));
      }

      if (!groupedReferencingPrograms.removedPrograms().isEmpty()) {
        int numPrograms = groupedReferencingPrograms.removedPrograms().size();
        tag.with(p(formatReferencingProgramsText("Removed from", numPrograms, "program")));
      }

      if (!groupedReferencingPrograms.disabledPrograms().isEmpty()) {
        int numPrograms = groupedReferencingPrograms.disabledPrograms().size();
        tag.with(p(formatReferencingProgramsText("Added to ", numPrograms, "disabled program")));
      }
    }

    if (maybeReferencingProgramsModal.isPresent()) {
      tag.with(
          a().withId(maybeReferencingProgramsModal.get().getTriggerButtonId())
              .withClasses(
                  "cursor-pointer",
                  "font-medium",
                  "underline",
                  BaseStyles.TEXT_CIVIFORM_BLUE,
                  StyleUtils.hover("text-black"))
              .withText("See list"));
    }
    return Pair.of(
        tag, maybeReferencingProgramsModal.map(ImmutableList::of).orElse(ImmutableList.of()));
  }

  private static String formatReferencingProgramsText(
      String prefix, int numPrograms, String suffix) {
    return String.format(
        "%s %d %s.",
        prefix,
        numPrograms,
        (numPrograms > 1 ? suffix.replaceAll("\\bprogram\\b", "programs") : suffix));
  }

  @AutoValue
  abstract static class GroupedReferencingPrograms {
    abstract ImmutableList<ProgramDefinition> usedPrograms();

    abstract ImmutableList<ProgramDefinition> addedPrograms();

    abstract ImmutableList<ProgramDefinition> removedPrograms();

    /**
     * Returns an immutable list of program definitions that are currently in draft status with
     * disabled visibility.
     */
    abstract ImmutableList<ProgramDefinition> disabledPrograms();

    static Builder builder() {
      return new AutoValue_QuestionsListView_GroupedReferencingPrograms.Builder();
    }

    boolean isEmpty() {
      boolean usedAndAddedAndRemovedProgramsIsEmpty =
          usedPrograms().isEmpty() && addedPrograms().isEmpty() && removedPrograms().isEmpty();
      return usedAndAddedAndRemovedProgramsIsEmpty && disabledPrograms().isEmpty();
    }

    int getTotalNumReferencingPrograms() {
      return usedPrograms().size() + addedPrograms().size();
    }

    @AutoValue.Builder
    abstract static class Builder {
      abstract Builder setUsedPrograms(ImmutableList<ProgramDefinition> usedPrograms);

      abstract Builder setAddedPrograms(ImmutableList<ProgramDefinition> addedPrograms);

      abstract Builder setRemovedPrograms(ImmutableList<ProgramDefinition> removedPrograms);

      abstract Builder setDisabledPrograms(ImmutableList<ProgramDefinition> disabledPrograms);

      abstract GroupedReferencingPrograms build();
    }
  }

  private GroupedReferencingPrograms createReferencingPrograms(
      Collection<ProgramDefinition> activePrograms, Collection<ProgramDefinition> draftPrograms) {
    ImmutableMap<String, ProgramDefinition> activeProgramsMap =
        activePrograms.stream()
            .collect(
                ImmutableMap.toImmutableMap(ProgramDefinition::adminName, Function.identity()));

    ImmutableMap<String, ProgramDefinition> draftDisabledProgramsMap =
        draftPrograms.stream()
            .filter(program -> program.displayMode() == DisplayMode.DISABLED)
            .collect(
                ImmutableMap.toImmutableMap(ProgramDefinition::adminName, Function.identity()));

    ImmutableMap<String, ProgramDefinition> draftProgramsMap =
        draftPrograms.stream()
            .collect(
                ImmutableMap.toImmutableMap(ProgramDefinition::adminName, Function.identity()));

    // Use set operations to collect programs into 4 sets.
    Set<String> usedSet = Sets.intersection(activeProgramsMap.keySet(), draftProgramsMap.keySet());
    Set<String> addedSet = Sets.difference(draftProgramsMap.keySet(), activeProgramsMap.keySet());
    addedSet = Sets.difference(addedSet, draftDisabledProgramsMap.keySet());
    Set<String> removedSet = Sets.difference(activeProgramsMap.keySet(), draftProgramsMap.keySet());
    Set<String> disabledSet =
        Sets.difference(draftDisabledProgramsMap.keySet(), activeProgramsMap.keySet());

    ImmutableList<ProgramDefinition> usedPrograms =
        usedSet.stream()
            .map(draftProgramsMap::get)
            .sorted(Comparator.comparing(ProgramDefinition::adminName))
            .collect(ImmutableList.toImmutableList());
    ImmutableList<ProgramDefinition> addedPrograms =
        addedSet.stream()
            .map(draftProgramsMap::get)
            .sorted(Comparator.comparing(ProgramDefinition::adminName))
            .collect(ImmutableList.toImmutableList());
    ImmutableList<ProgramDefinition> removedPrograms =
        removedSet.stream()
            .map(activeProgramsMap::get)
            .sorted(Comparator.comparing(ProgramDefinition::adminName))
            .collect(ImmutableList.toImmutableList());
    ImmutableList<ProgramDefinition> disabledPrograms =
        disabledSet.stream()
            .map(draftDisabledProgramsMap::get)
            .sorted(Comparator.comparing(ProgramDefinition::adminName))
            .collect(ImmutableList.toImmutableList());
    return GroupedReferencingPrograms.builder()
        .setUsedPrograms(usedPrograms)
        .setAddedPrograms(addedPrograms)
        .setRemovedPrograms(removedPrograms)
        .setDisabledPrograms(disabledPrograms)
        .build();
  }

  private Optional<Modal> makeReferencingProgramsModal(
      String questionName,
      GroupedReferencingPrograms referencingPrograms,
      Optional<DomContent> modalHeader) {
    if (referencingPrograms.isEmpty()) {
      return Optional.empty();
    }

    DivTag referencingProgramModalContent = div().withClasses("flex-row", "space-y-6");
    if (modalHeader.isPresent()) {
      referencingProgramModalContent.with(modalHeader.get());
    }
    referencingProgramModalContent
        .condWith(
            !referencingPrograms.usedPrograms().isEmpty(),
            div()
                .with(
                    referencingProgramList(
                        "This question is used in:", referencingPrograms.usedPrograms()))
                .withClass(ReferenceClasses.ADMIN_QUESTION_PROGRAM_REFERENCE_COUNTS_USED))
        .condWith(
            !referencingPrograms.addedPrograms().isEmpty(),
            div()
                .with(
                    referencingProgramList(
                        "This question is added to:", referencingPrograms.addedPrograms()))
                .withClass(ReferenceClasses.ADMIN_QUESTION_PROGRAM_REFERENCE_COUNTS_ADDED))
        .condWith(
            !referencingPrograms.removedPrograms().isEmpty(),
            div()
                .with(
                    referencingProgramList(
                        "This question is removed from:", referencingPrograms.removedPrograms()))
                .withClass(ReferenceClasses.ADMIN_QUESTION_PROGRAM_REFERENCE_COUNTS_REMOVED))
        .with(
            p("Note: This list does not automatically refresh. If edits are made to a program"
                    + " in a separate tab, they won't be reflected until the page has been"
                    + " refreshed.")
                .withClass("text-sm"));

    return Optional.of(
        Modal.builder()
            .setModalId(Modal.randomModalId())
            .setLocation(Modal.Location.ADMIN_FACING)
            .setContent(referencingProgramModalContent)
            .setModalTitle(String.format("Programs referencing %s", questionName))
            .setWidth(Width.HALF)
            .build());
  }

  private DivTag referencingProgramList(
      String title, ImmutableList<ProgramDefinition> referencingPrograms) {
    // TODO(#3162): Add ability to view a published program. Then add links to the
    // specific block that references the question.
    return div()
        .with(p(title).withClass("font-semibold"))
        .with(
            div()
                .with(
                    ul().withClasses("list-disc", "list-inside")
                        .with(
                            each(
                                referencingPrograms,
                                programReference -> {
                                  return li(programReference.localizedName().getDefault());
                                }))));
  }

  private ButtonTag renderQuestionEditLink(QuestionDefinition definition, boolean isVisible) {
    String link = controllers.admin.routes.AdminQuestionController.edit(definition.getId()).url();
    return asRedirectElement(
        makeSvgTextButton("Edit", Icons.EDIT)
            .withClasses(ButtonStyles.CLEAR_WITH_ICON, isVisible ? "" : "invisible"),
        link);
  }

  private Optional<ButtonTag> renderQuestionTranslationLink(QuestionDefinition definition) {
    if (translationLocales.translatableLocales().isEmpty()) {
      return Optional.empty();
    }
    String link =
        controllers.admin.routes.AdminQuestionTranslationsController.redirectToFirstLocale(
                definition.getName())
            .url();

    ButtonTag button =
        asRedirectElement(
            makeSvgTextButton("Manage translations", Icons.TRANSLATE)
                .withClasses(ButtonStyles.CLEAR_WITH_ICON_FOR_DROPDOWN),
            link);
    return Optional.of(button);
  }

  private Pair<DivTag, ImmutableList<Modal>> renderActionsCell(
      boolean isActive,
      QuestionCardData cardData,
      ActiveAndDraftQuestions activeAndDraftQuestions,
      Http.Request request) {
    QuestionDefinition question =
        isActive ? cardData.activeQuestion().get() : cardData.draftQuestion().get();
    ImmutableList.Builder<DomContent> extraActions = ImmutableList.builder();
    ImmutableList.Builder<Modal> modals = ImmutableList.builder();
    // some actions such as "edit" or "archive" need to be rendered only on one of
    // two rows.
    // If there is only "draft" or only "active" rows - render these actions.
    // If there are both "draft" and "active" versions - render edit actions only on
    // "draft".
    boolean isEditable =
        !isActive
            || activeAndDraftQuestions.getDraftQuestionDefinition(question.getName()).isEmpty();

    if (!isActive) {
      Optional<ButtonTag> maybeTranslationLink = renderQuestionTranslationLink(question);
      maybeTranslationLink.ifPresent(extraActions::add);
      if (activeAndDraftQuestions.getActiveQuestionDefinition(question.getName()).isPresent()) {
        Pair<DomContent, Modal> discardDraftButtonAndModal =
            renderDiscardDraftOption(question, request);
        extraActions.add(discardDraftButtonAndModal.getLeft());
        modals.add(discardDraftButtonAndModal.getRight());
      }
    }
    // Add Archive option only if current question is draft or it's active, but
    // there is no draft version of the question.
    if (isEditable) {
      Pair<DomContent, Optional<Modal>> archiveOptionsAndModal =
          renderArchiveOptions(cardData, question, activeAndDraftQuestions, request);
      extraActions.add(archiveOptionsAndModal.getLeft());
      archiveOptionsAndModal.getRight().ifPresent(modals::add);
    }

    // Build extra actions button and menu.
    String extraActionsButtonId = "extra-actions-" + Modal.randomModalId();
    ButtonTag extraActionsButton =
        makeSvgTextButton("", Icons.MORE_VERT)
            .withId(extraActionsButtonId)
            .withClasses(
                ButtonStyles.CLEAR_WITH_ICON,
                ReferenceClasses.WITH_DROPDOWN,
                "h-12",
                extraActions.build().isEmpty() ? "invisible" : "");
    DivTag result =
        div()
            .withClasses("flex", "space-x-2", "pr-6", "font-medium")
            .with(renderQuestionEditLink(question, isEditable))
            .with(
                div()
                    .withClass("relative")
                    .with(
                        extraActionsButton,
                        div()
                            .withId(extraActionsButtonId + "-dropdown")
                            .withClasses(
                                "hidden",
                                "flex",
                                "flex-col",
                                "border",
                                "bg-white",
                                "absolute",
                                "right-0",
                                "w-56",
                                "z-50")
                            .with(extraActions.build())));

    return Pair.of(result, modals.build());
  }

  private Pair<DomContent, Modal> renderDiscardDraftOption(
      QuestionDefinition definition, Http.Request request) {
    String link =
        controllers.admin.routes.AdminQuestionController.discardDraft(definition.getId()).url();

    ButtonTag discardConfirmButton =
        toLinkButtonForPost(
            makeSvgTextButton("Discard", Icons.DELETE)
                .withId("discard-button")
                .withClasses(ButtonStyles.SOLID_BLUE_WITH_ICON),
            link,
            request);

    DivTag discardConfirmationDiv =
        div(p("Are you sure you want to discard this draft?"), discardConfirmButton)
            .withClasses("p-6", "flex-row", "space-y-6");

    ButtonTag discardMenuButton =
        makeSvgTextButton("Discard Draft", Icons.DELETE)
            .withClasses(ButtonStyles.CLEAR_WITH_ICON_FOR_DROPDOWN);

    Modal modal =
        Modal.builder()
            .setModalId("discard-confirmation-modal")
            .setLocation(Modal.Location.ADMIN_FACING)
            .setContent(discardConfirmationDiv)
            .setModalTitle("Discard draft?")
            .setTriggerButtonContent(discardMenuButton)
            .setWidth(Width.FOURTH)
            .build();

    return Pair.of(modal.getButton(), modal);
  }

  private Pair<DomContent, Optional<Modal>> renderArchiveOptions(
      QuestionCardData cardData,
      QuestionDefinition definition,
      ActiveAndDraftQuestions activeAndDraftQuestions,
      Http.Request request) {
    switch (activeAndDraftQuestions.getDeletionStatus(definition.getName())) {
      case PENDING_DELETION:
        String restoreLink =
            controllers.admin.routes.AdminQuestionController.restore(definition.getId()).url();
        ButtonTag unarchiveButton =
            toLinkButtonForPost(
                makeSvgTextButton("Restore archived", Icons.UNARCHIVE)
                    .withClasses(ButtonStyles.CLEAR_WITH_ICON_FOR_DROPDOWN),
                restoreLink,
                request);
        return Pair.of(unarchiveButton, Optional.empty());
      case DELETABLE:
        String archiveLink =
            controllers.admin.routes.AdminQuestionController.archive(definition.getId()).url();
        ButtonTag archiveButton =
            toLinkButtonForPost(
                makeSvgTextButton("Archive", Icons.ARCHIVE)
                    .withClasses(ButtonStyles.CLEAR_WITH_ICON_FOR_DROPDOWN),
                archiveLink,
                request);
        return Pair.of(archiveButton, Optional.empty());
      default:
        DivTag modalHeader =
            div()
                .withClasses("p-2", "border", "border-gray-400", "bg-gray-200", "text-sm")
                .with(
                    span(
                        "This question cannot be archived since there are still programs"
                            + " using it. Please remove all usages from the below"
                            + " programs before attempting to archive."));

        GroupedReferencingPrograms referencingPrograms = cardData.referencingPrograms();
        Optional<Modal> maybeModal =
            makeReferencingProgramsModal(
                definition.getName(), referencingPrograms, Optional.of(modalHeader));
        ButtonTag cantArchiveButton =
            makeSvgTextButton("Archive", Icons.ARCHIVE)
                .withClasses(ButtonStyles.CLEAR_WITH_ICON_FOR_DROPDOWN)
                .withId(maybeModal.get().getTriggerButtonId());

        return Pair.of(cantArchiveButton, maybeModal);
    }
  }
}
