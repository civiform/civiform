package views.admin.programs;

import static com.google.common.base.Preconditions.checkNotNull;
import static j2html.TagCreator.a;
import static j2html.TagCreator.div;
import static j2html.TagCreator.each;
import static j2html.TagCreator.fieldset;
import static j2html.TagCreator.form;
import static j2html.TagCreator.h1;
import static j2html.TagCreator.h2;
import static j2html.TagCreator.legend;
import static j2html.TagCreator.li;
import static j2html.TagCreator.p;
import static j2html.TagCreator.span;
import static j2html.TagCreator.text;
import static j2html.TagCreator.ul;

import auth.CiviFormProfile;
import com.google.common.collect.ImmutableList;
import com.google.inject.Inject;
import com.typesafe.config.Config;
import controllers.admin.routes;
import j2html.tags.DomContent;
import j2html.tags.specialized.ATag;
import j2html.tags.specialized.ButtonTag;
import j2html.tags.specialized.DivTag;
import j2html.tags.specialized.FormTag;
import j2html.tags.specialized.LiTag;
import java.util.Comparator;
import java.util.Optional;
import java.util.concurrent.CompletionException;
import models.ProgramTab;
import play.mvc.Http;
import play.mvc.Http.HttpVerbs;
import play.twirl.api.Content;
import services.AlertType;
import services.program.ActiveAndDraftPrograms;
import services.program.ProgramDefinition;
import services.program.ProgramNotFoundException;
import services.program.ProgramService;
import services.program.ProgramType;
import services.question.ActiveAndDraftQuestions;
import services.question.ReadOnlyQuestionService;
import services.question.types.QuestionDefinition;
import services.settings.SettingsManifest;
import views.AlertComponent;
import views.BaseHtmlView;
import views.HtmlBundle;
import views.admin.AdminLayout;
import views.admin.AdminLayout.NavPage;
import views.admin.AdminLayoutFactory;
import views.components.ButtonStyles;
import views.components.FieldWithLabel;
import views.components.Icons;
import views.components.LinkElement;
import views.components.Modal;
import views.components.ProgramCardFactory;
import views.style.AdminStyles;
import views.style.ReferenceClasses;
import views.style.StyleUtils;

/** Renders a page so the admin can view all active programs and draft programs. */
public final class ProgramIndexView extends BaseHtmlView {
  private final AdminLayout layout;
  private final String baseUrl;
  private final ProgramCardFactory programCardFactory;
  private final ProgramService programService;
  private final SettingsManifest settingsManifest;

  @Inject
  public ProgramIndexView(
      AdminLayoutFactory layoutFactory,
      Config config,
      SettingsManifest settingsManifest,
      ProgramCardFactory programCardFactory,
      ProgramService programService) {
    this.layout = checkNotNull(layoutFactory).getLayout(NavPage.PROGRAMS);
    this.baseUrl = checkNotNull(config).getString("base_url");
    this.programCardFactory = checkNotNull(programCardFactory);
    this.programService = checkNotNull(programService);
    this.settingsManifest = checkNotNull(settingsManifest);
  }

  public Content render(
      ActiveAndDraftPrograms programs,
      ReadOnlyQuestionService readOnlyQuestionService,
      Http.Request request,
      ProgramTab selectedTab,
      Optional<CiviFormProfile> profile) {
    String pageTitle = "Program dashboard";

    // Revisit if we introduce internationalization because the word order could change in other
    // languages.
    String pageExplanation =
        "Create, edit and publish programs in "
            + settingsManifest.getWhitelabelCivicEntityShortName(request).get();

    // TODO: Figure out how to simplify logic so getUpToDateQuestions() can be used in place of
    // getActiveAndDraftQuestions()
    ImmutableList<Long> universalQuestionIds =
        readOnlyQuestionService.getUpToDateQuestions().stream()
            .filter(QuestionDefinition::isUniversal)
            .map(QuestionDefinition::getId)
            .collect(ImmutableList.toImmutableList());

    // Include all programs in draft in publishAllDraft modal.
    ActiveAndDraftPrograms allPrograms =
        programService.getActiveAndDraftProgramsWithoutQuestionLoad();
    Optional<Modal> maybePublishModal =
        maybeRenderPublishAllModal(
            allPrograms,
            readOnlyQuestionService.getActiveAndDraftQuestions(),
            request,
            universalQuestionIds);
    Modal demographicsCsvModal = renderDemographicsCsvModal();
    ImmutableList<Modal> publishSingleProgramModals =
        buildPublishSingleProgramModals(programs.getDraftPrograms(), universalQuestionIds, request);

    DivTag headerContent =
        div()
            .withClasses("flex", "items-center", "space-x-4", "mt-12")
            .with(
                h1(pageTitle),
                div().withClass("flex-grow"),
                div()
                    .with(
                        div()
                            .with(
                                demographicsCsvModal
                                    .getButton()
                                    .withClasses(ButtonStyles.OUTLINED_WHITE_WITH_ICON, "my-2"),
                                renderNewProgramButton(),
                                maybePublishModal.isPresent()
                                    ? maybePublishModal.get().getButton()
                                    : null)
                            .withClasses("flex", "flex-row", "space-x-4"),
                        renderImportProgramLink())
                    .withClasses("flex", "flex-col", "items-end"));

    DivTag contentDiv =
        div()
            .withClasses("px-4")
            .with(
                headerContent,
                div()
                    .withClasses("flex", "items-center", "space-x-4", "mt-12")
                    .with(h2(pageExplanation)),
                div()
                    .withClasses("mt-10", "flex")
                    .with(
                        div().withClass("flex-grow"),
                        p("Sorting by most recently updated").withClass("text-sm")));

    if (programService.anyDisabledPrograms()) {
      contentDiv.with(
          renderFilterLink(
              ProgramTab.IN_USE, selectedTab, routes.AdminProgramController.index().url()),
          renderFilterLink(
              ProgramTab.DISABLED,
              selectedTab,
              routes.AdminProgramController.indexDisabled().url()));
    }

    contentDiv.with(
        div()
            .withClass("mt-6")
            .with(
                each(
                    programs.getProgramNames().stream()
                        .map(
                            name ->
                                this.buildProgramCardData(
                                    programs.getActiveProgramDefinition(name),
                                    programs.getDraftProgramDefinition(name),
                                    request,
                                    profile,
                                    publishSingleProgramModals,
                                    universalQuestionIds))
                        .sorted(ProgramCardFactory.programTypeThenLastModifiedThenNameComparator())
                        .map(cardData -> programCardFactory.renderCard(cardData, request)))));

    HtmlBundle htmlBundle =
        layout
            .getBundle(request)
            .setTitle(pageTitle)
            .addMainContent(contentDiv)
            .addModals(demographicsCsvModal);

    publishSingleProgramModals.stream()
        .forEach(
            (modal) -> {
              htmlBundle.addModals(modal);
            });

    maybePublishModal.ifPresent(htmlBundle::addModals);

    addSuccessAndErrorToasts(htmlBundle, request.flash());

    return layout.renderCentered(htmlBundle);
  }

  private ATag renderFilterLink(
      ProgramTab status, ProgramTab selectedTab, String redirectLocation) {
    String styles =
        selectedTab.equals(status) ? AdminStyles.LINK_SELECTED : AdminStyles.LINK_NOT_SELECTED;
    return new LinkElement()
        .setText(status.getTabName())
        .setHref(redirectLocation)
        .setStyles(styles)
        .asAnchorText();
  }

  private Modal renderDemographicsCsvModal() {
    String modalId = "download-demographics-csv-modal";
    String downloadActionText = "Download demographic data (CSV)";
    DivTag downloadDemographicCsvModalContent =
        div()
            .withClasses("px-8")
            .with(
                form()
                    .withMethod("GET")
                    .withAction(
                        routes.AdminApplicationController.downloadDemographics(
                                /* fromDate= */ Optional.empty(), /* untilDate= */ Optional.empty())
                            .url())
                    .with(
                        p("This will download demographic data for all applications for all"
                                + " programs. Use the filters below to select a date range for the"
                                + " exported data. If you select a large date range or leave it"
                                + " blank, the data could be slow to export.")
                            .withClass("text-sm"),
                        fieldset()
                            .withClasses("mt-4", "pt-1", "pb-2", "border")
                            .with(
                                legend("Applications submitted").withClass("ml-3"),
                                // The field names below should be kept in sync with
                                // AdminApplicationController.downloadDemographics.
                                FieldWithLabel.date()
                                    .setFieldName("fromDate")
                                    .setLabelText("From:")
                                    .getDateTag()
                                    .withClasses("ml-3", "inline-flex"),
                                FieldWithLabel.date()
                                    .setFieldName("untilDate")
                                    .setLabelText("Until:")
                                    .getDateTag()
                                    .withClasses("ml-3", "inline-flex")),
                        makeSvgTextButton(downloadActionText, Icons.DOWNLOAD)
                            .withClasses(ButtonStyles.SOLID_BLUE_WITH_ICON, "mt-6")
                            .withType("submit")));
    return Modal.builder()
        .setModalId(modalId)
        .setLocation(Modal.Location.ADMIN_FACING)
        .setContent(downloadDemographicCsvModalContent)
        .setModalTitle(downloadActionText)
        .setTriggerButtonContent(makeSvgTextButton(downloadActionText, Icons.DOWNLOAD))
        .build();
  }

  private ImmutableList<Modal> buildPublishSingleProgramModals(
      ImmutableList<ProgramDefinition> programs,
      ImmutableList<Long> universalQuestionIds,
      Http.Request request) {

    return programs.stream()
        .map(
            program -> {
              FormTag publishSingleProgramForm =
                  form(makeCsrfTokenInputTag(request))
                      .withMethod(HttpVerbs.POST)
                      .withAction(routes.AdminProgramController.publishProgram(program.id()).url());

              DivTag missingUniversalQuestionsWarning =
                  div()
                      .with(
                          AlertComponent.renderFullAlert(
                              AlertType.WARNING,
                              "Warning: This program does not use all recommended"
                                  + " universal questions.",
                              /* title= */ Optional.empty(),
                              /* hidden= */ false))
                      .with(
                          p("We recommend using all universal questions when possible"
                                  + " to create consistent reuse of data and question"
                                  + " formatting.")
                              .withClasses("py-4"));

              DivTag buttons =
                  div(
                          submitButton("Publish program").withClasses(ButtonStyles.SOLID_BLUE),
                          button("Cancel")
                              .withClasses(
                                  ButtonStyles.LINK_STYLE_WITH_TRANSPARENCY,
                                  ReferenceClasses.MODAL_CLOSE))
                      .withClasses(
                          "flex", "flex-col", StyleUtils.responsiveMedium("flex-row"), "py-4");

              return Modal.builder()
                  .setModalId(buildPublishModalId(program.slug()))
                  .setLocation(Modal.Location.ADMIN_FACING)
                  .setContent(
                      publishSingleProgramForm
                          .condWith(
                              getCountMissingUniversalQuestions(program, universalQuestionIds) > 0,
                              missingUniversalQuestionsWarning)
                          .with(buttons))
                  .setModalTitle(getPublishModalTitle(program))
                  .setTriggerButtonContent(
                      makeSvgTextButton("Publish", Icons.PUBLISH)
                          .withClasses(ButtonStyles.CLEAR_WITH_ICON))
                  .setWidth(Modal.Width.THIRD)
                  .build();
            })
        .collect(ImmutableList.toImmutableList());
  }

  private String buildPublishModalId(String programSlug) {
    return "publish-modal-" + programSlug;
  }

  private String getPublishModalTitle(ProgramDefinition program) {
    String programName = program.localizedName().getDefault();
    if (program.programType().equals(ProgramType.EXTERNAL)) {
      return "Are you sure you want to publish " + programName + "?";
    }
    return "Are you sure you want to publish " + programName + " and all of its draft questions?";
  }

  private int getCountMissingUniversalQuestions(
      ProgramDefinition program, ImmutableList<Long> universalQuestionIds) {
    if (program.programType().equals(ProgramType.EXTERNAL)) {
      // External programs don't have questions, thus universal question are not applied to them
      return 0;
    }

    return universalQuestionIds.stream()
        .filter(id -> !program.getQuestionIdsInProgram().contains(id))
        .collect(ImmutableList.toImmutableList())
        .size();
  }

  private Optional<Modal> maybeRenderPublishAllModal(
      ActiveAndDraftPrograms programs,
      ActiveAndDraftQuestions questions,
      Http.Request request,
      ImmutableList<Long> universalQuestionIds) {
    // We should only render the publish modal / button if there is at least one draft.
    if (!programs.anyDraft() && !questions.draftVersionHasAnyEdits()) {
      return Optional.empty();
    }

    String link = routes.AdminProgramController.publish().url();

    ImmutableList<QuestionDefinition> sortedDraftQuestions =
        questions.getDraftQuestions().stream()
            .sorted(Comparator.comparing(QuestionDefinition::getName))
            .collect(ImmutableList.toImmutableList());
    ImmutableList<ProgramDefinition> sortedDraftPrograms =
        programs.getDraftPrograms().stream()
            .sorted(Comparator.comparing(ProgramDefinition::adminName))
            .collect(ImmutableList.toImmutableList());

    String programString = sortedDraftPrograms.size() == 1 ? "program" : "programs";
    String questionString = sortedDraftQuestions.size() == 1 ? "question" : "questions";

    DivTag publishAllModalContent =
        div()
            .withClasses("flex-row", "space-y-6")
            .with(
                AlertComponent.renderFullAlert(
                    AlertType.WARNING,
                    "Due to the nature of shared questions and versioning, all questions and"
                        + " programs will need to be published together.",
                    /* title= */ Optional.of("All draft questions in programs will be published."),
                    /* hidden= */ false),
                div()
                    .withClasses(ReferenceClasses.ADMIN_PUBLISH_REFERENCES_PROGRAM)
                    .with(
                        p(String.format(
                                "%d draft %s will be published:",
                                sortedDraftPrograms.size(), programString))
                            .withClass("font-bold text-lg"))
                    .condWith(sortedDraftPrograms.isEmpty(), p("None").withClass("pl-5"))
                    .condWith(
                        !sortedDraftPrograms.isEmpty(),
                        ul().withClasses("list-disc", "list-inside")
                            .with(
                                each(
                                    sortedDraftPrograms,
                                    program ->
                                        renderPublishModalProgramItem(
                                            program, universalQuestionIds)))),
                div()
                    .withClasses(ReferenceClasses.ADMIN_PUBLISH_REFERENCES_QUESTION)
                    .with(
                        p(String.format(
                                "%d draft %s will be published:",
                                sortedDraftQuestions.size(), questionString))
                            .withClass("font-bold text-lg"))
                    .condWith(sortedDraftQuestions.isEmpty(), p("None").withClass("pl-5"))
                    .condWith(
                        !sortedDraftQuestions.isEmpty(),
                        ul().withClasses("list-disc", "list-inside")
                            .with(
                                each(sortedDraftQuestions, this::renderPublishModalQuestionItem))),
                div()
                    .withClasses("flex", "flex-row", "pt-5")
                    .with(
                        toLinkButtonForPost(
                            submitButton("Publish all draft programs and questions")
                                .withClasses(ButtonStyles.SOLID_BLUE),
                            link,
                            request),
                        button("Cancel")
                            .withClasses(
                                ReferenceClasses.MODAL_CLOSE,
                                ButtonStyles.LINK_STYLE_WITH_TRANSPARENCY)));
    ButtonTag publishAllButton =
        makeSvgTextButton("Publish all drafts", Icons.PUBLISH)
            .withClasses(ButtonStyles.SOLID_BLUE_WITH_ICON, "my-2");
    Modal publishAllModal =
        Modal.builder()
            .setModalId("publish-all-programs-modal")
            .setLocation(Modal.Location.ADMIN_FACING)
            .setContent(publishAllModalContent)
            .setModalTitle("Do you want to publish all draft programs?")
            .setTriggerButtonContent(publishAllButton)
            .build();
    return Optional.of(publishAllModal);
  }

  private LiTag renderPublishModalProgramItem(
      ProgramDefinition program, ImmutableList<Long> universalQuestionIds) {
    String visibilityText =
        switch (program.displayMode()) {
          case DISABLED -> " (Hidden from applicants and Trusted Intermediaries) ";
          case HIDDEN_IN_INDEX -> " (Hidden from applicants) ";
          case PUBLIC -> " (Publicly visible) ";
          case SELECT_TI, TI_ONLY -> " ";
        };

    Optional<String> maybeUniversalQuestionsText =
        generateUniversalQuestionText(program, universalQuestionIds);

    return li().with(
            span(program.localizedName().getDefault()).withClasses("font-medium"),
            span(visibilityText)
                .condWith(
                    maybeUniversalQuestionsText.isPresent(),
                    span(" - " + maybeUniversalQuestionsText.orElse(""))),
            new LinkElement()
                .setText("Edit")
                .setHref(
                    routes.AdminProgramController.edit(program.id(), ProgramEditStatus.EDIT.name())
                        .url())
                .asAnchorText())
        .withClass("pt-2");
  }

  private LiTag renderPublishModalQuestionItem(QuestionDefinition question) {
    return li().with(
            span(question.getQuestionText().getDefault()).withClasses("font-medium"),
            span(" - "),
            new LinkElement()
                .setText("Edit")
                .setHref(routes.AdminQuestionController.edit(question.getId()).url())
                .asAnchorText())
        .withClass("pt-2");
  }

  private ButtonTag renderNewProgramButton() {
    String link = routes.AdminProgramController.newOne().url();
    ButtonTag button =
        makeSvgTextButton("Create new program", Icons.ADD)
            .withId("new-program-button")
            .withClasses(ButtonStyles.OUTLINED_WHITE_WITH_ICON, "my-2");
    return asRedirectElement(button, link);
  }

  private ATag renderImportProgramLink() {
    return a("Import existing program")
        .withHref(routes.AdminImportController.index().url())
        .withClass("usa-link");
  }

  private ProgramCardFactory.ProgramCardData buildProgramCardData(
      Optional<ProgramDefinition> activeProgram,
      Optional<ProgramDefinition> draftProgram,
      Http.Request request,
      Optional<CiviFormProfile> profile,
      ImmutableList<Modal> publishSingleProgramModals,
      ImmutableList<Long> universalQuestionIds) {
    Optional<ProgramCardFactory.ProgramCardData.ProgramRow> draftRow = Optional.empty();
    Optional<ProgramCardFactory.ProgramCardData.ProgramRow> activeRow = Optional.empty();

    if (draftProgram.isPresent()) {
      ImmutableList.Builder<ButtonTag> draftRowActions = ImmutableList.builder();
      ImmutableList.Builder<ButtonTag> draftRowExtraActions = ImmutableList.builder();

      // Add the trigger button belonging to the modal that matches each draft program
      publishSingleProgramModals.stream()
          .forEach(
              (modal) -> {
                if (modal.modalId().equals(buildPublishModalId(draftProgram.get().slug()))) {
                  draftRowActions.add(modal.getButton());
                }
              });
      draftRowActions.add(renderEditLink(/* isActive= */ false, draftProgram.get(), request));

      maybeRenderManageProgramAdminsLink(draftProgram.get()).ifPresent(draftRowExtraActions::add);
      maybeRenderManageTranslationsLink(draftProgram.get()).ifPresent(draftRowExtraActions::add);
      maybeRenderManageApplications(draftProgram.get()).ifPresent(draftRowExtraActions::add);
      maybeRenderExportProgramLink(draftProgram.get()).ifPresent(draftRowExtraActions::add);

      draftRow =
          Optional.of(
              ProgramCardFactory.ProgramCardData.ProgramRow.builder()
                  .setProgram(draftProgram.get())
                  .setRowActions(draftRowActions.build())
                  .setExtraRowActions(draftRowExtraActions.build())
                  .setUniversalQuestionsText(
                      generateUniversalQuestionText(draftProgram.get(), universalQuestionIds))
                  .setTranslationCompletionTag(generateTranslationCompleteText(draftProgram.get()))
                  .build());
    }

    if (activeProgram.isPresent()) {
      ImmutableList.Builder<ButtonTag> activeRowActions = ImmutableList.builder();
      ImmutableList.Builder<ButtonTag> activeRowExtraActions = ImmutableList.builder();

      activeRowActions.add(renderViewLink(activeProgram.get(), request));
      maybeRenderShareLink(activeProgram.get()).ifPresent(activeRowActions::add);

      maybeRenderViewApplicationsLink(activeProgram.get(), profile, request)
          .ifPresent(activeRowExtraActions::add);
      if (draftProgram.isEmpty()) {
        activeRowExtraActions.add(
            renderEditLink(/* isActive= */ true, activeProgram.get(), request));

        if (settingsManifest.getTranslationManagementImprovementEnabled(request)) {
          maybeRenderManageTranslationsLink(activeProgram.get())
              .ifPresent(activeRowExtraActions::add);
        }
      }
      maybeRenderManageProgramAdminsLink(activeProgram.get()).ifPresent(activeRowExtraActions::add);
      maybeRenderExportProgramLink(activeProgram.get()).ifPresent(activeRowExtraActions::add);

      activeRow =
          Optional.of(
              ProgramCardFactory.ProgramCardData.ProgramRow.builder()
                  .setProgram(activeProgram.get())
                  .setRowActions(activeRowActions.build())
                  .setExtraRowActions(activeRowExtraActions.build())
                  .setUniversalQuestionsText(
                      generateUniversalQuestionText(activeProgram.get(), universalQuestionIds))
                  .setTranslationCompletionTag(generateTranslationCompleteText(activeProgram.get()))
                  .build());
    }

    return ProgramCardFactory.ProgramCardData.builder()
        .setActiveProgram(activeRow)
        .setDraftProgram(draftRow)
        .setIsCiviFormAdmin(profile.isPresent() && profile.get().isCiviFormAdmin())
        .build();
  }

  Optional<String> generateUniversalQuestionText(
      ProgramDefinition program, ImmutableList<Long> universalQuestionIds) {
    if (program.programType().equals(ProgramType.EXTERNAL)) {
      // External programs don't have questions, thus universal question are not applied to them
      return Optional.empty();
    }

    int countAllUniversalQuestions = universalQuestionIds.size();
    if (countAllUniversalQuestions == 0) {
      return Optional.empty();
    }

    int countMissingUniversalQuestionIds =
        getCountMissingUniversalQuestions(program, universalQuestionIds);
    String text =
        countMissingUniversalQuestionIds == 0
            ? "all"
            : countAllUniversalQuestions
                - countMissingUniversalQuestionIds
                + " of "
                + countAllUniversalQuestions;
    return Optional.of("Contains " + text + " universal questions ");
  }

  Optional<DomContent> generateTranslationCompleteText(ProgramDefinition programDefinition) {
    try {
      boolean isTranslationComplete = programService.isTranslationComplete(programDefinition);
      if (isTranslationComplete == true) {
        return Optional.of(
            div(text("Translation complete"), Icons.svg(Icons.CHECK).withClasses("h-4 w-4"))
                .withClasses("flex", "items-center", "gap-1"));
      } else {
        return Optional.of(
            div(text("Translation incomplete"), Icons.svg(Icons.CLOSE).withClasses("h-4 w-4"))
                .withClasses("flex", "items-center", "gap-1"));
      }
    } catch (ProgramNotFoundException e) {
      throw new RuntimeException(e);
    }
  }

  Optional<ButtonTag> maybeRenderShareLink(ProgramDefinition program) {
    if (program.programType().equals(ProgramType.EXTERNAL)) {
      return Optional.empty();
    }

    String programLink =
        baseUrl
            + controllers.applicant.routes.ApplicantProgramsController.show(program.slug()).url();
    return Optional.of(
        makeSvgTextButton("Share link", Icons.CONTENT_COPY)
            .withClass(ButtonStyles.CLEAR_WITH_ICON)
            .withData("copyable-program-link", programLink));
  }

  ButtonTag renderEditLink(boolean isActive, ProgramDefinition program, Http.Request request) {
    String editLink = routes.AdminProgramBlocksController.index(program.id()).url();
    String editLinkId = "program-edit-link-" + program.id();
    if (isActive) {
      editLink = routes.AdminProgramController.newVersionFrom(program.id()).url();
      editLinkId = "program-new-version-link-" + program.id();
    }

    ButtonTag button = makeSvgTextButton("Edit", Icons.EDIT).withId(editLinkId);
    return isActive
        ? toLinkButtonForPost(
            button.withClass(ButtonStyles.CLEAR_WITH_ICON_FOR_DROPDOWN), editLink, request)
        : asRedirectElement(button.withClass(ButtonStyles.CLEAR_WITH_ICON), editLink);
  }

  ButtonTag renderViewLink(ProgramDefinition program, Http.Request request) {
    String viewLink = routes.AdminProgramBlocksController.readOnlyIndex(program.id()).url();
    String viewLinkId = "program-view-link-" + program.id();

    ButtonTag button =
        makeSvgTextButton("View", Icons.VIEW)
            .withId(viewLinkId)
            .withClasses(ButtonStyles.CLEAR_WITH_ICON);
    return asRedirectElement(button, viewLink);
  }

  private Optional<ButtonTag> maybeRenderManageTranslationsLink(ProgramDefinition program) {
    return layout.createManageTranslationsButton(
        program.adminName(),
        /* buttonId= */ Optional.of("program-translations-link-" + program.id()),
        ButtonStyles.CLEAR_WITH_ICON_FOR_DROPDOWN);
  }

  private Optional<ButtonTag> maybeRenderManageApplications(ProgramDefinition program) {
    // External programs don't have applications hosted on CiviForm
    ProgramType programType = program.programType();
    if (programType.equals(ProgramType.EXTERNAL)) {
      return Optional.empty();
    }

    String linkDestination = routes.AdminProgramStatusesController.index(program.id()).url();
    ButtonTag button =
        makeSvgTextButton("Manage application statuses", Icons.FLAKY)
            .withClass(ButtonStyles.CLEAR_WITH_ICON_FOR_DROPDOWN);
    return Optional.of(asRedirectElement(button, linkDestination));
  }

  private Optional<ButtonTag> maybeRenderViewApplicationsLink(
      ProgramDefinition activeProgram,
      Optional<CiviFormProfile> maybeUserProfile,
      Http.Request request) {
    // External programs don't have applications hosted on CiviForm
    ProgramType programType = activeProgram.programType();
    if (programType.equals(ProgramType.EXTERNAL)) {
      return Optional.empty();
    }

    if (maybeUserProfile.isEmpty()) {
      return Optional.empty();
    }
    CiviFormProfile userProfile = maybeUserProfile.get();
    // TODO(#2582): Determine if this has N+1 query behavior and fix if
    // necessary.
    boolean userIsAuthorized;
    try {
      userProfile.checkProgramAuthorization(activeProgram.adminName(), request).join();
      userIsAuthorized = true;
    } catch (CompletionException e) {
      userIsAuthorized = false;
    }
    if (userIsAuthorized) {
      String editLink =
          routes.AdminApplicationController.index(
                  activeProgram.id(),
                  /* search= */ Optional.empty(),
                  /* page= */ Optional.empty(),
                  /* fromDate= */ Optional.empty(),
                  /* untilDate= */ Optional.empty(),
                  /* applicationStatus= */ Optional.empty(),
                  /* selectedApplicationUri= */ Optional.empty(),
                  /* showDownloadModal= */ Optional.empty(),
                  /* message= */ Optional.empty())
              .url();

      String buttonText =
          programType.equals(ProgramType.PRE_SCREENER_FORM) ? "Forms" : "Applications";
      ButtonTag button =
          makeSvgTextButton(buttonText, Icons.TEXT_SNIPPET).withClass(ButtonStyles.CLEAR_WITH_ICON);
      return Optional.of(asRedirectElement(button, editLink));
    }
    return Optional.empty();
  }

  private Optional<ButtonTag> maybeRenderManageProgramAdminsLink(ProgramDefinition program) {
    // External programs don't have program administrators, since they cannot edit external programs
    ProgramType programType = program.programType();
    if (programType.equals(ProgramType.EXTERNAL)) {
      return Optional.empty();
    }

    String adminLink = routes.ProgramAdminManagementController.edit(program.id()).url();
    ButtonTag button =
        makeSvgTextButton("Manage program admins", Icons.GROUP)
            .withId("manage-program-admin-link-" + program.id())
            .withClass(ButtonStyles.CLEAR_WITH_ICON_FOR_DROPDOWN);
    return Optional.of(asRedirectElement(button, adminLink));
  }

  private Optional<ButtonTag> maybeRenderExportProgramLink(ProgramDefinition program) {
    // External programs cannot be exported/imported
    ProgramType programType = program.programType();
    if (programType.equals(ProgramType.EXTERNAL)) {
      return Optional.empty();
    }

    String adminLink = routes.AdminExportController.index(program.id()).url();
    ButtonTag button =
        makeSvgTextButton("Export program", Icons.DOWNLOAD)
            .withClass(ButtonStyles.CLEAR_WITH_ICON_FOR_DROPDOWN);
    return Optional.of(asRedirectElement(button, adminLink));
  }
}
