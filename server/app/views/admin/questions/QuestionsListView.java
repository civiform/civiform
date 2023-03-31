package views.admin.questions;

import static com.google.common.base.Preconditions.checkNotNull;
import static featureflags.FeatureFlag.PHONE_QUESTION_TYPE_ENABLED;
import static j2html.TagCreator.a;
import static j2html.TagCreator.br;
import static j2html.TagCreator.div;
import static j2html.TagCreator.each;
import static j2html.TagCreator.h1;
import static j2html.TagCreator.input;
import static j2html.TagCreator.li;
import static j2html.TagCreator.p;
import static j2html.TagCreator.span;
import static j2html.TagCreator.ul;

import com.google.auto.value.AutoValue;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Sets;
import com.google.inject.Inject;
import featureflags.FeatureFlags;
import j2html.tags.DomContent;
import j2html.tags.specialized.ButtonTag;
import j2html.tags.specialized.DivTag;
import j2html.tags.specialized.InputTag;
import j2html.tags.specialized.PTag;
import java.time.Instant;
import java.util.Collection;
import java.util.Comparator;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import org.apache.commons.lang3.tuple.Pair;
import play.mvc.Http;
import play.twirl.api.Content;
import services.TranslationLocales;
import services.program.ProgramDefinition;
import services.question.ActiveAndDraftQuestions;
import services.question.types.QuestionDefinition;
import views.BaseHtmlView;
import views.HtmlBundle;
import views.ViewUtils;
import views.ViewUtils.ProgramDisplayType;
import views.admin.AdminLayout;
import views.admin.AdminLayout.NavPage;
import views.admin.AdminLayoutFactory;
import views.components.CreateQuestionButton;
import views.components.Icons;
import views.components.Modal;
import views.components.Modal.Width;
import views.components.SvgTag;
import views.components.ToastMessage;
import views.style.AdminStyles;
import views.style.BaseStyles;
import views.style.ReferenceClasses;
import views.style.StyleUtils;

/** Renders a page for viewing all active questions and draft questions. */
public final class QuestionsListView extends BaseHtmlView {

  private final AdminLayout layout;
  private final TranslationLocales translationLocales;
  private final ViewUtils viewUtils;
  private final FeatureFlags featureFlags;

  @Inject
  public QuestionsListView(
    AdminLayoutFactory layoutFactory,
    TranslationLocales translationLocales,
    ViewUtils viewUtils, FeatureFlags featureFlags) {
    this.layout = checkNotNull(layoutFactory).getLayout(NavPage.QUESTIONS);
    this.translationLocales = checkNotNull(translationLocales);
    this.viewUtils = checkNotNull(viewUtils);
    this.featureFlags = checkNotNull(featureFlags);
    ;
  }

  /** Renders a page with a list view of all questions. */
  public Content render(ActiveAndDraftQuestions activeAndDraftQuestions, Http.Request request) {
    String title = "All Questions";

    Pair<DivTag, ImmutableList<Modal>> questionRowsAndModals =
        renderAllQuestionRows(activeAndDraftQuestions, request);

    InputTag filterInput =
        input()
            .withId("question-bank-filter")
            .withType("text")
            .withName("questionFilter")
            .withPlaceholder("Search questions")
            .withClasses(
                "h-10",
                "px-10",
                "pr-5",
                "w-full",
                "rounded-full",
                "text-sm",
                "border",
                "border-gray-200",
                "shadow",
                StyleUtils.focus("outline-none"));
    SvgTag filterIcon = Icons.svg(Icons.SEARCH).withClasses("h-4", "w-4");
    DivTag filterIconDiv = div().withClasses("absolute", "ml-4", "mt-3", "mr-4").with(filterIcon);
    DivTag filterDiv =
        div().withClasses("mt-6", "mb-2", "relative").with(filterIconDiv, filterInput);

    DivTag contentDiv =
        div()
            .withClasses("px-4")
            .with(
                div()
                    .withClasses("flex", "items-center", "space-x-4", "mt-12")
                    .with(
                        h1(title),
                        div().withClass("flex-grow"),
                        CreateQuestionButton.renderCreateQuestionButton(
                            controllers.admin.routes.AdminQuestionController.index().url(),
                            /* isPrimaryButton= */ true,
                          /* phoneQuestionTypeEnabled= */ featureFlags.getFlagEnabled(request,PHONE_QUESTION_TYPE_ENABLED))),
                filterDiv,
                div()
                    .withClasses("mt-10", "flex")
                    .with(
                        div().withClass("flex-grow"),
                        p("Sorting by most recently updated").withClass("text-sm")))
            .with(div().withClass("mt-6").with(questionRowsAndModals.getLeft()))
            .with(renderSummary(activeAndDraftQuestions));
    HtmlBundle htmlBundle =
        layout
            .getBundle()
            .setTitle(title)
            .addModals(questionRowsAndModals.getRight())
            .addMainContent(contentDiv);

    Http.Flash flash = request.flash();
    if (flash.get("success").isPresent()) {
      htmlBundle.addToastMessages(
          ToastMessage.success(flash.get("success").get()).setDismissible(false));
    } else if (flash.get("error").isPresent()) {
      htmlBundle.addToastMessages(
          ToastMessage.error(flash.get("error").get()).setDismissible(false));
    }

    return layout.renderCentered(htmlBundle);
  }

  private DivTag renderSummary(ActiveAndDraftQuestions activeAndDraftQuestions) {
    // The total question count should be equivalent to the number of rows in the displayed table,
    // where we have a single entry for a question that is active and has a draft.
    return div(String.format(
            "Total Questions: %d", activeAndDraftQuestions.getQuestionNames().size()))
        .withClasses("float-right", "text-base", "px-4", "my-2");
  }

  private static QuestionDefinition getDisplayQuestion(QuestionCardData cardData) {
    return cardData.draftQuestion().orElseGet(cardData.activeQuestion()::get);
  }

  private Pair<DivTag, ImmutableList<Modal>> renderAllQuestionRows(
      ActiveAndDraftQuestions activeAndDraftQuestions, Http.Request request) {
    ImmutableList.Builder<DomContent> rows = ImmutableList.builder();
    ImmutableList.Builder<Modal> modals = ImmutableList.builder();
    ImmutableList<QuestionCardData> cards =
        activeAndDraftQuestions.getQuestionNames().stream()
            .map(
                name ->
                    QuestionCardData.builder()
                        .setActiveQuestion(
                            activeAndDraftQuestions.getActiveQuestionDefinition(name))
                        .setDraftQuestion(activeAndDraftQuestions.getDraftQuestionDefinition(name))
                        .build())
            .sorted(
                Comparator.<QuestionCardData, Instant>comparing(
                        card ->
                            getDisplayQuestion(card).getLastModifiedTime().orElse(Instant.EPOCH))
                    .reversed()
                    .thenComparing(
                        card ->
                            getDisplayQuestion(card).getQuestionText().getDefault().toLowerCase()))
            .collect(ImmutableList.toImmutableList());

    for (QuestionCardData card : cards) {
      Pair<DivTag, ImmutableList<Modal>> rowAndModals =
          renderQuestionCard(card, activeAndDraftQuestions, request);
      rows.add(rowAndModals.getLeft());
      modals.addAll(rowAndModals.getRight());
    }
    return Pair.of(div().with(rows.build()), modals.build());
  }

  @AutoValue
  abstract static class QuestionCardData {
    abstract Optional<QuestionDefinition> activeQuestion();

    abstract Optional<QuestionDefinition> draftQuestion();

    static Builder builder() {
      return new AutoValue_QuestionsListView_QuestionCardData.Builder();
    }

    @AutoValue.Builder
    abstract static class Builder {
      abstract Builder setActiveQuestion(Optional<QuestionDefinition> v);

      abstract Builder setDraftQuestion(Optional<QuestionDefinition> v);

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
        renderReferencingPrograms(latestDefinition.getName(), activeAndDraftQuestions);
    modals.addAll(referencingProgramAndModal.getRight());

    DivTag row =
        div()
            .withClasses("flex")
            .with(renderInfoCell(latestDefinition))
            .with(referencingProgramAndModal.getLeft());

    DivTag draftAndActiveRows = div().withClasses("flex-grow");
    if (cardData.draftQuestion().isPresent()) {
      Pair<DivTag, ImmutableList<Modal>> draftRow =
          renderActiveOrDraftRow(
              /* isActive= */ false,
              cardData.draftQuestion().get(),
              activeAndDraftQuestions,
              request);
      modals.addAll(draftRow.getRight());
      draftAndActiveRows.with(draftRow.getLeft());
    }
    if (cardData.activeQuestion().isPresent()) {
      Pair<DivTag, ImmutableList<Modal>> activeRow =
          renderActiveOrDraftRow(
              /* isActive= */ true,
              cardData.activeQuestion().get(),
              activeAndDraftQuestions,
              request);
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
                "w-full",
                "my-4",
                "pl-6",
                "border-gray-300",
                "rounded-lg",
                "border",
                ReferenceClasses.ADMIN_QUESTION_TABLE_ROW)
            .with(row)
            .with(adminNote);
    return Pair.of(rowWithAdminNote, modals.build());
  }

  /**
   * Renders single "draft" or "active" row within a question row. Question can have up to two such
   * rows, one "draft" and one "active".
   */
  private Pair<DivTag, ImmutableList<Modal>> renderActiveOrDraftRow(
      boolean isActive,
      QuestionDefinition question,
      ActiveAndDraftQuestions activeAndDraftQuestions,
      Http.Request request) {
    boolean isSecondRow =
        isActive
            && activeAndDraftQuestions.getDraftQuestionDefinition(question.getName()).isPresent();

    Pair<DivTag, ImmutableList<Modal>> actionsCellAndModal =
        renderActionsCell(isActive, question, activeAndDraftQuestions, request);

    PTag badge =
        ViewUtils.makeBadge(
            isActive ? ProgramDisplayType.ACTIVE : ProgramDisplayType.DRAFT,
            "ml-2",
            StyleUtils.responsiveXLarge("ml-8"));

    DivTag row =
        div()
            .withClasses(
                "py-7",
                "flex",
                "flex-row",
                "items-center",
                StyleUtils.hover("bg-gray-100"),
                "cursor-pointer",
                isSecondRow ? "border-t" : "")
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
            .withClasses("font-bold", "text-black", "flex", "flex-row", "items-center")
            .with(
                Icons.questionTypeSvg(definition.getQuestionType())
                    .withClasses("w-6", "h-6", "shrink-0"))
            .with(
                div(definition.getQuestionText().getDefault())
                    .withClasses(ReferenceClasses.ADMIN_QUESTION_TITLE, "pl-4", "text-xl"));
    DivTag questionDescription =
        div(
            div(definition.getQuestionHelpText().isEmpty()
                    ? ""
                    : definition.getQuestionHelpText().getDefault())
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
      String questionName, ActiveAndDraftQuestions activeAndDraftQuestions) {
    ActiveAndDraftQuestions.ReferencingPrograms referencingPrograms =
        activeAndDraftQuestions.getReferencingPrograms(questionName);
    Collection<ProgramDefinition> activePrograms = referencingPrograms.activeReferences();
    Collection<ProgramDefinition> draftPrograms = referencingPrograms.draftReferences();
    GroupedReferencingPrograms groupedReferencingPrograms =
        createReferencingPrograms(activePrograms, draftPrograms);

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
        tag.with(p("Used in " + numPrograms + " program" + (numPrograms > 1 ? "s." : ".")));
      }
      if (!groupedReferencingPrograms.addedPrograms().isEmpty()) {
        int numPrograms = groupedReferencingPrograms.addedPrograms().size();
        tag.with(p("Added to " + numPrograms + " program" + (numPrograms > 1 ? "s." : ".")));
      }
      if (!groupedReferencingPrograms.removedPrograms().isEmpty()) {
        int numPrograms = groupedReferencingPrograms.removedPrograms().size();
        tag.with(p("Removed from " + numPrograms + " program" + (numPrograms > 1 ? "s." : ".")));
      }
    }
    if (maybeReferencingProgramsModal.isPresent()) {
      tag.with(
          a().withId(maybeReferencingProgramsModal.get().getTriggerButtonId())
              .withClasses(
                  "cursor-pointer",
                  "font-medium",
                  "underline",
                  BaseStyles.TEXT_SEATTLE_BLUE,
                  StyleUtils.hover("text-black"))
              .withText("See list"));
    }
    return Pair.of(
        tag, maybeReferencingProgramsModal.map(ImmutableList::of).orElse(ImmutableList.of()));
  }

  @AutoValue
  abstract static class GroupedReferencingPrograms {
    abstract ImmutableList<ProgramDefinition> usedPrograms();

    abstract ImmutableList<ProgramDefinition> addedPrograms();

    abstract ImmutableList<ProgramDefinition> removedPrograms();

    static Builder builder() {
      return new AutoValue_QuestionsListView_GroupedReferencingPrograms.Builder();
    }

    boolean isEmpty() {
      return usedPrograms().isEmpty() && addedPrograms().isEmpty() && removedPrograms().isEmpty();
    }

    @AutoValue.Builder
    abstract static class Builder {
      abstract Builder setUsedPrograms(ImmutableList<ProgramDefinition> usedPrograms);

      abstract Builder setAddedPrograms(ImmutableList<ProgramDefinition> addedPrograms);

      abstract Builder setRemovedPrograms(ImmutableList<ProgramDefinition> removedPrograms);

      abstract GroupedReferencingPrograms build();
    }
  }

  private GroupedReferencingPrograms createReferencingPrograms(
      Collection<ProgramDefinition> activePrograms, Collection<ProgramDefinition> draftPrograms) {
    ImmutableMap<String, ProgramDefinition> activeProgramsMap =
        activePrograms.stream()
            .collect(
                ImmutableMap.toImmutableMap(ProgramDefinition::adminName, Function.identity()));
    ImmutableMap<String, ProgramDefinition> draftProgramsMap =
        draftPrograms.stream()
            .collect(
                ImmutableMap.toImmutableMap(ProgramDefinition::adminName, Function.identity()));

    // Use set operations to collect programs into 3 sets.
    Set<String> usedSet = Sets.intersection(activeProgramsMap.keySet(), draftProgramsMap.keySet());
    Set<String> addedSet = Sets.difference(draftProgramsMap.keySet(), activeProgramsMap.keySet());
    Set<String> removedSet = Sets.difference(activeProgramsMap.keySet(), draftProgramsMap.keySet());

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
    return GroupedReferencingPrograms.builder()
        .setUsedPrograms(usedPrograms)
        .setAddedPrograms(addedPrograms)
        .setRemovedPrograms(removedPrograms)
        .build();
  }

  private Optional<Modal> makeReferencingProgramsModal(
      String questionName,
      GroupedReferencingPrograms referencingPrograms,
      Optional<DomContent> modalHeader) {
    if (referencingPrograms.isEmpty()) {
      return Optional.empty();
    }

    DivTag referencingProgramModalContent = div().withClasses("p-6", "flex-row", "space-y-6");
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
            .setContent(referencingProgramModalContent)
            .setModalTitle(String.format("Programs referencing %s", questionName))
            .setWidth(Width.HALF)
            .build());
  }

  private DivTag referencingProgramList(
      String title, ImmutableList<ProgramDefinition> referencingPrograms) {
    // TODO(#3162): Add ability to view a published program. Then add
    // links to the specific block that references the question.
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
                                  return li(programReference.adminName());
                                }))));
  }

  private ButtonTag renderQuestionEditLink(QuestionDefinition definition, boolean isVisible) {
    String link = controllers.admin.routes.AdminQuestionController.edit(definition.getId()).url();
    return asRedirectElement(
        makeSvgTextButton("Edit", Icons.EDIT)
            .withClasses(AdminStyles.TERTIARY_BUTTON_STYLES, isVisible ? "" : "invisible"),
        link);
  }

  private Optional<ButtonTag> renderQuestionTranslationLink(QuestionDefinition definition) {
    if (translationLocales.translatableLocales().isEmpty()) {
      return Optional.empty();
    }
    String link =
        controllers.admin.routes.AdminQuestionTranslationsController.redirectToFirstLocale(
                definition.getId())
            .url();

    ButtonTag button =
        asRedirectElement(
            makeSvgTextButton("Manage Translations", Icons.TRANSLATE)
                .withClasses(AdminStyles.TERTIARY_BUTTON_STYLES),
            link);
    return Optional.of(button);
  }

  private Pair<DivTag, ImmutableList<Modal>> renderActionsCell(
      boolean isActive,
      QuestionDefinition question,
      ActiveAndDraftQuestions activeAndDraftQuestions,
      Http.Request request) {
    ImmutableList.Builder<DomContent> extraActions = ImmutableList.builder();
    ImmutableList.Builder<Modal> modals = ImmutableList.builder();
    // some actions such as "edit" or "archive" need to be rendered only on one of two rows.
    // If there is only "draft" or only "active" rows - render these actions.
    // If there are both "draft" and "active" versions - render edit actions only on "draft".
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
    // Add Archive option only if current question is draft or it's active, but there is no
    // draft version of the question.
    if (isEditable) {
      Pair<DomContent, Optional<Modal>> archiveOptionsAndModal =
          renderArchiveOptions(question, activeAndDraftQuestions, request);
      extraActions.add(archiveOptionsAndModal.getLeft());
      archiveOptionsAndModal.getRight().ifPresent(modals::add);
    }

    // Build extra actions button and menu.
    String extraActionsButtonId = "extra-actions-" + Modal.randomModalId();
    ButtonTag extraActionsButton =
        makeSvgTextButton("", Icons.MORE_VERT)
            .withId(extraActionsButtonId)
            .withClasses(
                AdminStyles.TERTIARY_BUTTON_STYLES,
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
                .withClasses(AdminStyles.PRIMARY_BUTTON_STYLES),
            link,
            request);

    DivTag discardConfirmationDiv =
        div(p("Are you sure you want to discard this draft?"), discardConfirmButton)
            .withClasses("p-6", "flex-row", "space-y-6");

    ButtonTag discardMenuButton =
        makeSvgTextButton("Discard Draft", Icons.DELETE)
            .withClasses(AdminStyles.TERTIARY_BUTTON_STYLES);

    Modal modal =
        Modal.builder()
            .setModalId("discard-confirmation-modal")
            .setContent(discardConfirmationDiv)
            .setModalTitle("Discard draft?")
            .setTriggerButtonContent(discardMenuButton)
            .setWidth(Width.FOURTH)
            .build();

    return Pair.of(modal.getButton(), modal);
  }

  private Pair<DomContent, Optional<Modal>> renderArchiveOptions(
      QuestionDefinition definition,
      ActiveAndDraftQuestions activeAndDraftQuestions,
      Http.Request request) {
    switch (activeAndDraftQuestions.getDeletionStatus(definition.getName())) {
      case PENDING_DELETION:
        String restoreLink =
            controllers.admin.routes.AdminQuestionController.restore(definition.getId()).url();
        ButtonTag unarchiveButton =
            toLinkButtonForPost(
                makeSvgTextButton("Restore Archived", Icons.UNARCHIVE)
                    .withClasses(AdminStyles.TERTIARY_BUTTON_STYLES),
                restoreLink,
                request);
        return Pair.of(unarchiveButton, Optional.empty());
      case DELETABLE:
        String archiveLink =
            controllers.admin.routes.AdminQuestionController.archive(definition.getId()).url();
        ButtonTag archiveButton =
            toLinkButtonForPost(
                makeSvgTextButton("Archive", Icons.ARCHIVE)
                    .withClasses(AdminStyles.TERTIARY_BUTTON_STYLES),
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

        ActiveAndDraftQuestions.ReferencingPrograms programs =
            activeAndDraftQuestions.getReferencingPrograms(definition.getName());
        GroupedReferencingPrograms referencingPrograms =
            createReferencingPrograms(programs.activeReferences(), programs.draftReferences());
        Optional<Modal> maybeModal =
            makeReferencingProgramsModal(
                definition.getName(), referencingPrograms, Optional.of(modalHeader));
        ButtonTag cantArchiveButton =
            makeSvgTextButton("Archive", Icons.ARCHIVE)
                .withClasses(AdminStyles.TERTIARY_BUTTON_STYLES)
                .withId(maybeModal.get().getTriggerButtonId());

        return Pair.of(cantArchiveButton, maybeModal);
    }
  }
}
