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
import featureflags.FeatureFlags;
import j2html.tags.specialized.ButtonTag;
import j2html.tags.specialized.DivTag;
import j2html.tags.specialized.LiTag;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletionException;
import play.mvc.Http;
import play.twirl.api.Content;
import services.TranslationLocales;
import services.program.ActiveAndDraftPrograms;
import services.program.ProgramDefinition;
import services.question.ActiveAndDraftQuestions;
import services.question.types.QuestionDefinition;
import views.BaseHtmlView;
import views.HtmlBundle;
import views.admin.AdminLayout;
import views.admin.AdminLayout.NavPage;
import views.admin.AdminLayoutFactory;
import views.components.FieldWithLabel;
import views.components.Icons;
import views.components.LinkElement;
import views.components.Modal;
import views.components.ProgramCardFactory;
import views.components.ToastMessage;
import views.style.AdminStyles;
import views.style.ReferenceClasses;

/** Renders a page so the admin can view all active programs and draft programs. */
public final class ProgramIndexView extends BaseHtmlView {
  private final AdminLayout layout;
  private final String baseUrl;
  private final TranslationLocales translationLocales;
  private final ProgramCardFactory programCardFactory;
  private final String civicEntityShortName;
  private final FeatureFlags featureFlags;

  @Inject
  public ProgramIndexView(
      AdminLayoutFactory layoutFactory,
      Config config,
      FeatureFlags featureFlags,
      TranslationLocales translationLocales,
      ProgramCardFactory programCardFactory) {
    this.layout = checkNotNull(layoutFactory).getLayout(NavPage.PROGRAMS);
    this.baseUrl = checkNotNull(config).getString("base_url");
    this.translationLocales = checkNotNull(translationLocales);
    this.programCardFactory = checkNotNull(programCardFactory);
    this.civicEntityShortName = config.getString("whitelabel.civic_entity_short_name");
    this.featureFlags = checkNotNull(featureFlags);
  }

  public Content render(
      ActiveAndDraftPrograms programs,
      ActiveAndDraftQuestions questions,
      Http.Request request,
      Optional<CiviFormProfile> profile) {
    if (profile.isPresent() && profile.get().isProgramAdmin() && !profile.get().isCiviFormAdmin()) {
      layout.setOnlyProgramAdminType();
    }

    String pageTitle = "Program dashboard";

    // Revisit if we introduce internationalization because the word order could change in other
    // languages.
    String pageExplanation = "Create, edit and publish programs in " + civicEntityShortName;
    Optional<Modal> maybePublishModal = maybeRenderPublishModal(programs, questions, request);

    Modal demographicsCsvModal = renderDemographicsCsvModal();
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
                            .withClasses(AdminStyles.SECONDARY_BUTTON_STYLES, "my-2"),
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
                                            profile))
                                .sorted(ProgramCardFactory.lastModifiedTimeThenNameComparator())
                                .map(programCardFactory::renderCard))));

    HtmlBundle htmlBundle =
        layout
            .getBundle()
            .setTitle(pageTitle)
            .addMainContent(contentDiv)
            .addModals(demographicsCsvModal);
    maybePublishModal.ifPresent(htmlBundle::addModals);

    Http.Flash flash = request.flash();
    if (flash.get("error").isPresent()) {
      htmlBundle.addToastMessages(ToastMessage.error(flash.get("error").get()).setDuration(-1));
    } else if (flash.get("success").isPresent()) {
      htmlBundle.addToastMessages(ToastMessage.success(flash.get("success").get()).setDuration(-1));
    }

    return layout.renderCentered(htmlBundle);
  }

  private Modal renderDemographicsCsvModal() {
    String modalId = "download-demographics-csv-modal";
    String downloadActionText = "Download Exported Data (CSV)";
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
                        p("This will download all applications for all programs. Use the filters"
                                + " below to select a date range for the exported data. If you"
                                + " select a large date range or leave it blank, the data could"
                                + " be slow to export.")
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
                            .withClasses(AdminStyles.PRIMARY_BUTTON_STYLES, "mt-6")
                            .withType("submit")));
    return Modal.builder(modalId, downloadDemographicCsvModalContent)
        .setModalTitle(downloadActionText)
        .setTriggerButtonContent(makeSvgTextButton(downloadActionText, Icons.DOWNLOAD))
        .build();
  }

  private Optional<Modal> maybeRenderPublishModal(
      ActiveAndDraftPrograms programs, ActiveAndDraftQuestions questions, Http.Request request) {
    // We should only render the publish modal / button if there is at least one draft program.
    if (!programs.anyDraft()) {
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

    DivTag publishAllModalContent =
        div()
            .withClasses("p-6", "flex-row", "space-y-6")
            .with(
                p("Please be aware that due to the nature of shared questions and versioning,"
                        + " all questions and programs will need to be published together.")
                    .withClass("text-sm"),
                div()
                    .withClasses(ReferenceClasses.ADMIN_PUBLISH_REFERENCES_QUESTION)
                    .with(
                        p(String.format("Draft questions (%d):", sortedDraftQuestions.size()))
                            .withClass("font-semibold"))
                    .condWith(sortedDraftQuestions.isEmpty(), p("None").withClass("pl-5"))
                    .condWith(
                        !sortedDraftQuestions.isEmpty(),
                        ul().withClasses("list-disc", "list-inside")
                            .with(
                                each(sortedDraftQuestions, this::renderPublishModalQuestionItem))),
                div()
                    .withClasses(ReferenceClasses.ADMIN_PUBLISH_REFERENCES_PROGRAM)
                    .with(
                        p(String.format("Draft programs (%d):", sortedDraftPrograms.size()))
                            .withClass("font-semibold"))
                    .condWith(sortedDraftPrograms.isEmpty(), p("None").withClass("pl-5"))
                    .condWith(
                        !sortedDraftPrograms.isEmpty(),
                        ul().withClasses("list-disc", "list-inside")
                            .with(each(sortedDraftPrograms, this::renderPublishModalProgramItem))),
                p("Would you like to publish all draft questions and programs now?"),
                div()
                    .withClasses("flex", "flex-row")
                    .with(
                        div().withClass("flex-grow"),
                        button("Cancel")
                            .withClasses(
                                ReferenceClasses.MODAL_CLOSE, AdminStyles.TERTIARY_BUTTON_STYLES),
                        toLinkButtonForPost(
                            submitButton("Confirm").withClasses(AdminStyles.TERTIARY_BUTTON_STYLES),
                            link,
                            request)));
    ButtonTag publishAllButton =
        makeSvgTextButton("Publish all drafts", Icons.PUBLISH)
            .withClasses(AdminStyles.PRIMARY_BUTTON_STYLES, "my-2");
    Modal publishAllModal =
        Modal.builder("publish-all-programs-modal", publishAllModalContent)
            .setModalTitle("All draft programs will be published")
            .setTriggerButtonContent(publishAllButton)
            .build();
    return Optional.of(publishAllModal);
  }

  private LiTag renderPublishModalProgramItem(ProgramDefinition program) {
    String visibilityText = "";
    switch (program.displayMode()) {
      case HIDDEN_IN_INDEX:
        visibilityText = "Hidden from applicants";
        break;
      case PUBLIC:
        visibilityText = "Publicly visible";
        break;
      default:
        break;
    }
    return li().with(
            span(program.localizedName().getDefault()).withClasses("font-medium"),
            span(" - " + visibilityText + " "),
            new LinkElement()
                .setText("Edit")
                .setHref(controllers.admin.routes.AdminProgramController.edit(program.id()).url())
                .asAnchorText());
  }

  private LiTag renderPublishModalQuestionItem(QuestionDefinition question) {
    return li().with(
            span(question.getQuestionText().getDefault()).withClasses("font-medium"),
            span(" - "),
            new LinkElement()
                .setText("Edit")
                .setHref(
                    controllers.admin.routes.AdminQuestionController.edit(question.getId()).url())
                .asAnchorText());
  }

  private ButtonTag renderNewProgramButton() {
    String link = controllers.admin.routes.AdminProgramController.newOne().url();
    ButtonTag button =
        makeSvgTextButton("Create new program", Icons.ADD)
            .withId("new-program-button")
            .withClasses(AdminStyles.SECONDARY_BUTTON_STYLES, "my-2");
    return asRedirectElement(button, link);
  }

  private ProgramCardFactory.ProgramCardData buildProgramCardData(
      Optional<ProgramDefinition> activeProgram,
      Optional<ProgramDefinition> draftProgram,
      Http.Request request,
      Optional<CiviFormProfile> profile) {

    Optional<ProgramCardFactory.ProgramCardData.ProgramRow> draftRow = Optional.empty();
    Optional<ProgramCardFactory.ProgramCardData.ProgramRow> activeRow = Optional.empty();
    if (draftProgram.isPresent()) {
      List<ButtonTag> draftRowActions = Lists.newArrayList();
      List<ButtonTag> draftRowExtraActions = Lists.newArrayList();
      draftRowActions.add(renderEditLink(/* isActive = */ false, draftProgram.get(), request));
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
                  .build());
    }

    if (activeProgram.isPresent()) {
      List<ButtonTag> activeRowActions = Lists.newArrayList();
      List<ButtonTag> activeRowExtraActions = Lists.newArrayList();

      Optional<ButtonTag> applicationsLink =
          maybeRenderViewApplicationsLink(activeProgram.get(), profile, request);
      applicationsLink.ifPresent(activeRowExtraActions::add);
      if (draftProgram.isEmpty()) {
        if (featureFlags.isReadOnlyProgramViewEnabled(request)) {
          activeRowExtraActions.add(
              renderEditLink(/* isActive = */ true, activeProgram.get(), request));
        } else {
          activeRowActions.add(renderEditLink(/* isActive = */ true, activeProgram.get(), request));
        }
        activeRowExtraActions.add(renderManageProgramAdminsLink(activeProgram.get()));
      }
      if (featureFlags.isReadOnlyProgramViewEnabled(request)) {
        activeRowActions.add(renderViewLink(activeProgram.get(), request));
      }
      activeRowActions.add(renderShareLink(activeProgram.get()));
      activeRow =
          Optional.of(
              ProgramCardFactory.ProgramCardData.ProgramRow.builder()
                  .setProgram(activeProgram.get())
                  .setRowActions(ImmutableList.copyOf(activeRowActions))
                  .setExtraRowActions(ImmutableList.copyOf(activeRowExtraActions))
                  .build());
    }

    return ProgramCardFactory.ProgramCardData.builder()
        .setActiveProgram(activeRow)
        .setDraftProgram(draftRow)
        .build();
  }

  ButtonTag renderShareLink(ProgramDefinition program) {
    String programLink =
        baseUrl
            + controllers.applicant.routes.RedirectController.programBySlug(program.slug()).url();
    return makeSvgTextButton("Share link", Icons.CONTENT_COPY)
        .withClass(AdminStyles.TERTIARY_BUTTON_STYLES)
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

    ButtonTag button =
        makeSvgTextButton("Edit", Icons.EDIT)
            .withId(editLinkId)
            .withClasses(AdminStyles.TERTIARY_BUTTON_STYLES);
    return isActive
        ? toLinkButtonForPost(button, editLink, request)
        : asRedirectElement(button, editLink);
  }

  ButtonTag renderViewLink(ProgramDefinition program, Http.Request request) {
    String viewLink =
        controllers.admin.routes.AdminProgramBlocksController.readOnlyIndex(program.id()).url();
    String viewLinkId = "program-view-link-" + program.id();

    ButtonTag button =
        makeSvgTextButton("View", Icons.VIEW)
            .withId(viewLinkId)
            .withClasses(AdminStyles.TERTIARY_BUTTON_STYLES);
    return asRedirectElement(button, viewLink);
  }

  private Optional<ButtonTag> renderManageTranslationsLink(ProgramDefinition program) {
    if (translationLocales.translatableLocales().isEmpty()) {
      return Optional.empty();
    }
    String linkDestination =
        routes.AdminProgramTranslationsController.redirectToFirstLocale(program.id()).url();
    ButtonTag button =
        makeSvgTextButton("Manage translations", Icons.LANGUAGE)
            .withId("program-translations-link-" + program.id())
            .withClass(AdminStyles.TERTIARY_BUTTON_STYLES);
    return Optional.of(asRedirectElement(button, linkDestination));
  }

  private ButtonTag renderEditStatusesLink(ProgramDefinition program) {
    String linkDestination = routes.AdminProgramStatusesController.index(program.id()).url();
    ButtonTag button =
        makeSvgTextButton("Manage application statuses", Icons.FLAKY)
            .withClass(AdminStyles.TERTIARY_BUTTON_STYLES);
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

      ButtonTag button =
          makeSvgTextButton("Applications", Icons.TEXT_SNIPPET)
              .withClass(AdminStyles.TERTIARY_BUTTON_STYLES);
      return Optional.of(asRedirectElement(button, editLink));
    }
    return Optional.empty();
  }

  private ButtonTag renderManageProgramAdminsLink(ProgramDefinition program) {
    String adminLink = routes.ProgramAdminManagementController.edit(program.id()).url();
    ButtonTag button =
        makeSvgTextButton("Manage Program Admins", Icons.GROUP)
            .withId("manage-program-admin-link-" + program.id())
            .withClass(AdminStyles.TERTIARY_BUTTON_STYLES);
    return asRedirectElement(button, adminLink);
  }
}
