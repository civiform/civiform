package views.admin.programs;

import static com.google.common.base.Preconditions.checkNotNull;
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
import static j2html.TagCreator.ul;

import auth.CiviFormProfile;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.inject.Inject;
import com.typesafe.config.Config;
import controllers.admin.routes;
import j2html.tags.specialized.ButtonTag;
import j2html.tags.specialized.DivTag;
import j2html.tags.specialized.FormTag;
import j2html.tags.specialized.LiTag;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletionException;
import play.mvc.Http;
import play.mvc.Http.HttpVerbs;
import play.twirl.api.Content;
import services.program.ActiveAndDraftPrograms;
import services.program.ProgramDefinition;
import services.question.ActiveAndDraftQuestions;
import services.question.ReadOnlyQuestionService;
import services.question.types.QuestionDefinition;
import services.settings.SettingsManifest;
import views.BaseHtmlView;
import views.HtmlBundle;
import views.ViewUtils;
import views.admin.AdminLayout;
import views.admin.AdminLayout.NavPage;
import views.admin.AdminLayoutFactory;
import views.components.ButtonStyles;
import views.components.FieldWithLabel;
import views.components.Icons;
import views.components.LinkElement;
import views.components.Modal;
import views.components.ProgramCardFactory;
import views.components.TextFormatter;
import views.components.ToastMessage;
import views.style.BaseStyles;
import views.style.ReferenceClasses;
import views.style.StyleUtils;

/** Renders a page so the admin can view all active programs and draft programs. */
public final class ProgramIndexView extends BaseHtmlView {
  private final AdminLayout layout;
  private final String baseUrl;
  private final ProgramCardFactory programCardFactory;
  private final SettingsManifest settingsManifest;

  @Inject
  public ProgramIndexView(
      AdminLayoutFactory layoutFactory,
      Config config,
      SettingsManifest settingsManifest,
      ProgramCardFactory programCardFactory) {
    this.layout = checkNotNull(layoutFactory).getLayout(NavPage.PROGRAMS);
    this.baseUrl = checkNotNull(config).getString("base_url");
    this.programCardFactory = checkNotNull(programCardFactory);
    this.settingsManifest = checkNotNull(settingsManifest);
  }

  public Content render(
      ActiveAndDraftPrograms programs,
      ReadOnlyQuestionService readOnlyQuestionService,
      Http.Request request,
      Optional<CiviFormProfile> profile) {
    if (profile.isPresent()) {
      layout.setAdminType(profile.get());
    }

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
    Optional<Modal> maybePublishModal =
        maybeRenderPublishAllModal(
            programs,
            readOnlyQuestionService.getActiveAndDraftQuestions(),
            request,
            universalQuestionIds);
    Modal demographicsCsvModal = renderDemographicsCsvModal();
    ImmutableList<Modal> publishSingleProgramModals =
        buildPublishSingleProgramModals(programs.getDraftPrograms(), universalQuestionIds, request);

    DivTag contentDiv =
        div()
            .withClasses("px-4")
            .with(
                div()
                    .withClasses("flex", "items-center", "space-x-4", "mt-12")
                    .with(
                        h1(pageTitle),
                        div().withClass("flex-grow"),
                        demographicsCsvModal
                            .getButton()
                            .withClasses(ButtonStyles.OUTLINED_WHITE_WITH_ICON, "my-2"),
                        renderNewProgramButton(),
                        maybePublishModal.isPresent() ? maybePublishModal.get().getButton() : null),
                div()
                    .withClasses("flex", "items-center", "space-x-4", "mt-12")
                    .with(h2(pageExplanation)),
                div()
                    .withClasses("mt-10", "flex")
                    .with(
                        div().withClass("flex-grow"),
                        p("Sorting by most recently updated").withClass("text-sm")),
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
                                .sorted(
                                    ProgramCardFactory
                                        .programTypeThenLastModifiedThenNameComparator())
                                .map(
                                    cardData ->
                                        programCardFactory.renderCard(request, cardData)))));

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

    Http.Flash flash = request.flash();
    if (flash.get("error").isPresent()) {
      htmlBundle.addToastMessages(ToastMessage.errorNonLocalized(flash.get("error").get()));
    } else if (flash.get("success").isPresent()) {
      htmlBundle.addToastMessages(ToastMessage.success(flash.get("success").get()));
    }

    return layout.renderCentered(htmlBundle);
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
                                Optional.empty(), Optional.empty())
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
                          ViewUtils.makeAlert(
                              "Warning: This program does not use all recommended"
                                  + " universal questions.",
                              /* hidden= */ false,
                              /* title= */ Optional.empty(),
                              BaseStyles.ALERT_WARNING))
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
                  .setModalTitle(
                      "Are you sure you want to publish "
                          + program.localizedName().getDefault()
                          + " and all of its draft questions?")
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

  private int getCountMissingUniversalQuestions(
      ProgramDefinition program, ImmutableList<Long> universalQuestionIds) {
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
                ViewUtils.makeAlert(
                    "Due to the nature of shared questions and versioning, all questions and"
                        + " programs will need to be published together.",
                    /* hidden= */ false,
                    /* title= */ Optional.of("All draft questions in programs will be published."),
                    BaseStyles.ALERT_WARNING),
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
                                            program, universalQuestionIds, request)))),
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
      ProgramDefinition program, ImmutableList<Long> universalQuestionIds, Http.Request request) {
    String visibilityText = " ";
    switch (program.displayMode()) {
      case DISABLED:
        if (settingsManifest.getDisabledVisibilityConditionEnabled(request)) {
          visibilityText = " (Hidden from applicants and Trusted Intermediaries) ";
        }
        break;
      case HIDDEN_IN_INDEX:
        visibilityText = " (Hidden from applicants) ";
        break;
      case PUBLIC:
        visibilityText = " (Publicly visible) ";
        break;
      default:
        break;
    }

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
                    controllers.admin.routes.AdminProgramController.edit(
                            program.id(), ProgramEditStatus.EDIT.name())
                        .url())
                .asAnchorText())
        .withClass("pt-2");
  }

  private LiTag renderPublishModalQuestionItem(QuestionDefinition question) {
    return li().with(
            span(TextFormatter.formatText(question.getQuestionText().getDefault()).toString()),
            span(" - "),
            new LinkElement()
                .setText("Edit")
                .setHref(
                    controllers.admin.routes.AdminQuestionController.edit(question.getId()).url())
                .asAnchorText())
        .withClass("pt-2");
  }

  private ButtonTag renderNewProgramButton() {
    String link = controllers.admin.routes.AdminProgramController.newOne().url();
    ButtonTag button =
        makeSvgTextButton("Create new program", Icons.ADD)
            .withId("new-program-button")
            .withClasses(ButtonStyles.OUTLINED_WHITE_WITH_ICON, "my-2");
    return asRedirectElement(button, link);
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
      List<ButtonTag> draftRowActions = Lists.newArrayList();
      List<ButtonTag> draftRowExtraActions = Lists.newArrayList();

      // Add the trigger button belonging to the modal that matches each draft program
      publishSingleProgramModals.stream()
          .forEach(
              (modal) -> {
                if (modal.modalId().equals(buildPublishModalId(draftProgram.get().slug()))) {
                  draftRowActions.add(modal.getButton());
                }
              });

      draftRowActions.add(renderEditLink(/* isActive= */ false, draftProgram.get(), request));
      draftRowExtraActions.add(renderManageProgramAdminsLink(draftProgram.get()));
      Optional<ButtonTag> maybeManageTranslationsLink =
          renderManageTranslationsLink(draftProgram.get());
      if (maybeManageTranslationsLink.isPresent()) {
        draftRowExtraActions.add(maybeManageTranslationsLink.get());
      }
      draftRowExtraActions.add(renderEditStatusesLink(draftProgram.get()));
      draftRow =
          Optional.of(
              ProgramCardFactory.ProgramCardData.ProgramRow.builder()
                  .setProgram(draftProgram.get())
                  .setRowActions(ImmutableList.copyOf(draftRowActions))
                  .setExtraRowActions(ImmutableList.copyOf(draftRowExtraActions))
                  .setUniversalQuestionsText(
                      generateUniversalQuestionText(draftProgram.get(), universalQuestionIds))
                  .build());
    }

    if (activeProgram.isPresent()) {
      List<ButtonTag> activeRowActions = Lists.newArrayList();
      List<ButtonTag> activeRowExtraActions = Lists.newArrayList();

      Optional<ButtonTag> applicationsLink =
          maybeRenderViewApplicationsLink(activeProgram.get(), profile, request);
      applicationsLink.ifPresent(activeRowExtraActions::add);
      if (draftProgram.isEmpty()) {
        activeRowExtraActions.add(
            renderEditLink(/* isActive= */ true, activeProgram.get(), request));
        activeRowExtraActions.add(renderManageProgramAdminsLink(activeProgram.get()));
      }
      activeRowActions.add(renderViewLink(activeProgram.get(), request));
      activeRowActions.add(renderShareLink(activeProgram.get()));
      activeRow =
          Optional.of(
              ProgramCardFactory.ProgramCardData.ProgramRow.builder()
                  .setProgram(activeProgram.get())
                  .setRowActions(ImmutableList.copyOf(activeRowActions))
                  .setExtraRowActions(ImmutableList.copyOf(activeRowExtraActions))
                  .setUniversalQuestionsText(
                      generateUniversalQuestionText(activeProgram.get(), universalQuestionIds))
                  .build());
    }

    return ProgramCardFactory.ProgramCardData.builder()
        .setActiveProgram(activeRow)
        .setDraftProgram(draftRow)
        .setProfile(profile)
        .build();
  }

  Optional<String> generateUniversalQuestionText(
      ProgramDefinition program, ImmutableList<Long> universalQuestionIds) {
    int countMissingUniversalQuestionIds =
        getCountMissingUniversalQuestions(program, universalQuestionIds);
    int countAllUniversalQuestions = universalQuestionIds.size();
    if (countAllUniversalQuestions == 0) {
      return Optional.empty();
    }
    String text =
        countMissingUniversalQuestionIds == 0
            ? "all"
            : countAllUniversalQuestions
                - countMissingUniversalQuestionIds
                + " of "
                + countAllUniversalQuestions;
    return Optional.of("Contains " + text + " universal questions ");
  }

  ButtonTag renderShareLink(ProgramDefinition program) {
    String programLink =
        baseUrl
            + controllers.applicant.routes.ApplicantProgramsController.show(program.slug()).url();
    return makeSvgTextButton("Share link", Icons.CONTENT_COPY)
        .withClass(ButtonStyles.CLEAR_WITH_ICON)
        .withData("copyable-program-link", programLink);
  }

  ButtonTag renderEditLink(boolean isActive, ProgramDefinition program, Http.Request request) {
    String editLink =
        controllers.admin.routes.AdminProgramBlocksController.index(program.id()).url();
    String editLinkId = "program-edit-link-" + program.id();
    if (isActive) {
      editLink = controllers.admin.routes.AdminProgramController.newVersionFrom(program.id()).url();
      editLinkId = "program-new-version-link-" + program.id();
    }

    ButtonTag button = makeSvgTextButton("Edit", Icons.EDIT).withId(editLinkId);
    return isActive
        ? toLinkButtonForPost(
            button.withClass(ButtonStyles.CLEAR_WITH_ICON_FOR_DROPDOWN), editLink, request)
        : asRedirectElement(button.withClass(ButtonStyles.CLEAR_WITH_ICON), editLink);
  }

  ButtonTag renderViewLink(ProgramDefinition program, Http.Request request) {
    String viewLink =
        controllers.admin.routes.AdminProgramBlocksController.readOnlyIndex(program.id()).url();
    String viewLinkId = "program-view-link-" + program.id();

    ButtonTag button =
        makeSvgTextButton("View", Icons.VIEW)
            .withId(viewLinkId)
            .withClasses(ButtonStyles.CLEAR_WITH_ICON);
    return asRedirectElement(button, viewLink);
  }

  private Optional<ButtonTag> renderManageTranslationsLink(ProgramDefinition program) {
    return layout.createManageTranslationsButton(
        program.adminName(),
        /* buttonId= */ Optional.of("program-translations-link-" + program.id()),
        ButtonStyles.CLEAR_WITH_ICON_FOR_DROPDOWN);
  }

  private ButtonTag renderEditStatusesLink(ProgramDefinition program) {
    String linkDestination = routes.AdminProgramStatusesController.index(program.id()).url();
    ButtonTag button =
        makeSvgTextButton("Manage application statuses", Icons.FLAKY)
            .withClass(ButtonStyles.CLEAR_WITH_ICON_FOR_DROPDOWN);
    return asRedirectElement(button, linkDestination);
  }

  private Optional<ButtonTag> maybeRenderViewApplicationsLink(
      ProgramDefinition activeProgram,
      Optional<CiviFormProfile> maybeUserProfile,
      Http.Request request) {
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
                  /* selectedApplicationUri= */ Optional.empty())
              .url();

      String buttonText =
          settingsManifest.getIntakeFormEnabled(request) && activeProgram.isCommonIntakeForm()
              ? "Forms"
              : "Applications";
      ButtonTag button =
          makeSvgTextButton(buttonText, Icons.TEXT_SNIPPET).withClass(ButtonStyles.CLEAR_WITH_ICON);
      return Optional.of(asRedirectElement(button, editLink));
    }
    return Optional.empty();
  }

  private ButtonTag renderManageProgramAdminsLink(ProgramDefinition program) {
    String adminLink = routes.ProgramAdminManagementController.edit(program.id()).url();
    ButtonTag button =
        makeSvgTextButton("Manage program admins", Icons.GROUP)
            .withId("manage-program-admin-link-" + program.id())
            .withClass(ButtonStyles.CLEAR_WITH_ICON_FOR_DROPDOWN);
    return asRedirectElement(button, adminLink);
  }
}
