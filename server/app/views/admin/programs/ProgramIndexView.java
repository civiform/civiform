package views.admin.programs;

import static annotations.FeatureFlags.ApplicationStatusTrackingEnabled;
import static com.google.common.base.Preconditions.checkNotNull;
import static j2html.TagCreator.div;
import static j2html.TagCreator.each;
import static j2html.TagCreator.fieldset;
import static j2html.TagCreator.form;
import static j2html.TagCreator.h1;
import static j2html.TagCreator.legend;
import static j2html.TagCreator.p;
import static j2html.TagCreator.span;

import auth.CiviFormProfile;
import com.google.common.collect.Lists;
import com.google.inject.Inject;
import com.typesafe.config.Config;
import controllers.admin.routes;
import j2html.tags.specialized.ButtonTag;
import j2html.tags.specialized.DivTag;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletionException;
import play.mvc.Http;
import play.twirl.api.Content;
import services.DateConverter;
import services.LocalizedStrings;
import services.program.ActiveAndDraftPrograms;
import services.program.ProgramDefinition;
import views.BaseHtmlView;
import views.HtmlBundle;
import views.admin.AdminLayout;
import views.admin.AdminLayout.NavPage;
import views.admin.AdminLayoutFactory;
import views.components.FieldWithLabel;
import views.components.Icons;
import views.components.Modal;
import views.style.AdminStyles;
import views.style.BaseStyles;
import views.style.ReferenceClasses;
import views.style.StyleUtils;
import views.style.Styles;

/** Renders a page so the admin can view all active programs and draft programs. */
public final class ProgramIndexView extends BaseHtmlView {
  private final AdminLayout layout;
  private final String baseUrl;
  private final DateConverter dateConverter;
  private final boolean statusTrackingEnabled;

  @Inject
  public ProgramIndexView(
      AdminLayoutFactory layoutFactory,
      Config config,
      DateConverter dateConverter,
      @ApplicationStatusTrackingEnabled boolean statusTrackingEnabled) {
    this.layout = checkNotNull(layoutFactory).getLayout(NavPage.PROGRAMS);
    this.baseUrl = checkNotNull(config).getString("base_url");
    this.dateConverter = checkNotNull(dateConverter);
    this.statusTrackingEnabled = statusTrackingEnabled;
  }

  public Content render(
      ActiveAndDraftPrograms programs, Http.Request request, Optional<CiviFormProfile> profile) {
    if (profile.isPresent() && profile.get().isProgramAdmin() && !profile.get().isCiviFormAdmin()) {
      layout.setOnlyProgramAdminType();
    }

    String pageTitle = "All programs";
    Optional<Modal> maybePublishModal = maybeRenderPublishModal(programs, request);

    Modal demographicsCsvModal = renderDemographicsCsvModal();
    DivTag contentDiv =
        div()
            .withClasses("px-4")
            .with(
                div()
                    .withClasses(
                        "flex",
                        "items-center",
                        "space-x-4",
                        "mt-12",
                        "mb-10")
                    .with(
                        h1(pageTitle),
                        div().withClass("flex-grow"),
                        demographicsCsvModal
                            .getButton()
                            .withClasses(AdminStyles.SECONDARY_BUTTON_STYLES, "my-2"),
                        renderNewProgramButton(),
                        maybePublishModal.isPresent() ? maybePublishModal.get().getButton() : null),
                div()
                    .withClasses(ReferenceClasses.ADMIN_PROGRAM_CARD_LIST, "invisible")
                    .with(
                        p("Loading")
                            .withClasses(ReferenceClasses.ADMIN_PROGRAM_CARD_LIST_PLACEHOLDER),
                        each(
                            programs.getProgramNames(),
                            name ->
                                this.renderProgramListItem(
                                    programs.getActiveProgramDefinition(name),
                                    programs.getDraftProgramDefinition(name),
                                    request,
                                    profile))));

    HtmlBundle htmlBundle =
        layout
            .getBundle()
            .setTitle(pageTitle)
            .addMainContent(contentDiv)
            .addModals(demographicsCsvModal)
            .addFooterScripts(layout.viewUtils.makeLocalJsTag("admin_programs"));
    maybePublishModal.ifPresent(htmlBundle::addModals);
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

  private ButtonTag makePublishButton() {
    return makeSvgTextButton("Publish all drafts", Icons.PUBLISH)
        .withId("publish-programs-button")
        .withClasses(AdminStyles.PRIMARY_BUTTON_STYLES, "my-2");
  }

  private Optional<Modal> maybeRenderPublishModal(
      ActiveAndDraftPrograms programs, Http.Request request) {
    // We should only render the publish modal / button if there is at least one draft.
    if (!programs.anyDraft()) {
      return Optional.empty();
    }

    String link = routes.AdminProgramController.publish().url();

    DivTag publishAllModalContent =
        div()
            .withClasses("flex", "flex-col", "gap-4", "px-2")
            .with(p("Are you sure you want to publish all programs?").withClasses("p-2"))
            .with(div().with(toLinkButtonForPost(makePublishButton(), link, request)));
    Modal publishAllModal =
        Modal.builder("publish-all-programs-modal", publishAllModalContent)
            .setModalTitle("Confirmation")
            .setTriggerButtonContent(makePublishButton())
            .build();
    return Optional.of(publishAllModal);
  }

  private ButtonTag renderNewProgramButton() {
    String link = controllers.admin.routes.AdminProgramController.newOne().url();
    ButtonTag button =
        makeSvgTextButton("Create new program", Icons.ADD)
            .withId("new-program-button")
            .withClasses(AdminStyles.SECONDARY_BUTTON_STYLES, "my-2");
    return asRedirectButton(button, link);
  }

  public ProgramDefinition getDisplayProgram(
      Optional<ProgramDefinition> draftProgram, Optional<ProgramDefinition> activeProgram) {
    if (draftProgram.isPresent()) {
      return draftProgram.get();
    }
    return activeProgram.get();
  }

  private DivTag renderProgramRow(
      boolean isActive,
      ProgramDefinition program,
      List<ButtonTag> actions,
      List<ButtonTag> extraActions,
      String... extraStyles) {
    String badgeText = "Draft";
    String badgeBGColor = BaseStyles.BG_CIVIFORM_PURPLE_LIGHT;
    String badgeFillColor = BaseStyles.TEXT_CIVIFORM_PURPLE;
    String updatedPrefix = "Edited on ";
    Optional<Instant> updatedTime = program.lastModifiedTime();
    if (isActive) {
      badgeText = "Active";
      badgeBGColor = BaseStyles.BG_CIVIFORM_GREEN_LIGHT;
      badgeFillColor = BaseStyles.TEXT_CIVIFORM_GREEN;
      updatedPrefix = "Published on ";
    }

    String formattedUpdateTime =
        updatedTime.map(t -> dateConverter.renderDateTime(t)).orElse("unknown");
    String formattedUpdateDate =
        updatedTime.map(t -> dateConverter.renderDate(t)).orElse("unknown");

    int blockCount = program.getBlockCount();
    int questionCount = program.getQuestionCount();

    String extraActionsButtonId = "extra-actions-" + program.id();
    ButtonTag extraActionsButton =
        makeSvgTextButton("", Icons.MORE_VERT)
            .withId(extraActionsButtonId)
            .withClasses(
                AdminStyles.TERTIARY_BUTTON_STYLES,
                ReferenceClasses.WITH_DROPDOWN,
                "h-12",
                extraActions.size() == 0 ? "invisible" : "");

    return div()
        .withClasses(
            "py-7",
            "flex",
            "flex-row",
            StyleUtils.hover("bg-gray-100"),
            StyleUtils.joinStyles(extraStyles))
        .with(
            p().withClasses(
                    badgeBGColor,
                    badgeFillColor,
                    "ml-2",
                    StyleUtils.responsiveXLarge("ml-8"),
                    "font-medium",
                    "rounded-full",
                    "flex",
                    "flex-row",
                    "gap-x-2",
                    "place-items-center",
                    "justify-center")
                .withStyle("min-width:90px")
                .with(
                    Icons.svg(Icons.NOISE_CONTROL_OFF, 20)
                        .withClasses("inline-block", "ml-3.5"),
                    span(badgeText).withClass("mr-4")),
            div()
                .withClasses("ml-4", StyleUtils.responsiveXLarge("ml-10"))
                .with(
                    p().with(
                            span(updatedPrefix),
                            span(formattedUpdateTime)
                                .withClasses(
                                    "font-semibold",
                                    "hidden",
                                    StyleUtils.responsiveLarge("inline")),
                            span(formattedUpdateDate)
                                .withClasses(
                                    "font-semibold",
                                    StyleUtils.responsiveLarge("hidden"))),
                    p().with(
                            span(String.format("%d", blockCount)).withClass("font-semibold"),
                            span(blockCount == 1 ? " screen, " : " screens, "),
                            span(String.format("%d", questionCount))
                                .withClass("font-semibold"),
                            span(questionCount == 1 ? " question" : " questions"))),
            div().withClass("flex-grow"),
            div()
                .withClasses("flex", "space-x-2", "pr-6", "font-medium")
                .with(actions)
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
                                .with(extraActions))));
  }

  public DivTag renderProgramListItem(
      Optional<ProgramDefinition> activeProgram,
      Optional<ProgramDefinition> draftProgram,
      Http.Request request,
      Optional<CiviFormProfile> profile) {
    ProgramDefinition displayProgram = getDisplayProgram(draftProgram, activeProgram);

    String programTitleText = displayProgram.adminName();
    String programDescriptionText = displayProgram.adminDescription();

    DivTag statusDiv = div();
    if (draftProgram.isPresent()) {
      List<ButtonTag> draftRowActions = Lists.newArrayList();
      List<ButtonTag> draftRowExtraActions = Lists.newArrayList();
      draftRowActions.add(renderEditLink(/* isActive = */ false, draftProgram.get(), request));
      draftRowExtraActions.add(renderManageProgramAdminsLink(draftProgram.get()));
      draftRowExtraActions.add(renderManageTranslationsLink(draftProgram.get()));
      if (statusTrackingEnabled) {
        draftRowExtraActions.add(renderEditStatusesLink(draftProgram.get()));
      }
      statusDiv =
          statusDiv.with(
              renderProgramRow(
                  /* isActive = */ false,
                  draftProgram.get(),
                  draftRowActions,
                  draftRowExtraActions));
    }

    if (activeProgram.isPresent()) {
      List<ButtonTag> activeRowActions = Lists.newArrayList();
      List<ButtonTag> activeRowExtraActions = Lists.newArrayList();
      Optional<ButtonTag> applicationsLink =
          maybeRenderViewApplicationsLink(activeProgram.get(), profile);
      applicationsLink.ifPresent(activeRowExtraActions::add);
      if (!draftProgram.isPresent()) {
        activeRowActions.add(renderEditLink(/* isActive = */ true, activeProgram.get(), request));
        activeRowExtraActions.add(renderManageProgramAdminsLink(activeProgram.get()));
      }
      activeRowActions.add(renderShareLink(activeProgram.get()));
      statusDiv =
          statusDiv.with(
              renderProgramRow(
                  /* isActive = */ true,
                  activeProgram.get(),
                  activeRowActions,
                  activeRowExtraActions,
                  draftProgram.isPresent() ? "border-t" : ""));
    }

    DivTag titleAndStatus =
        div()
            .withClass("flex")
            .with(
                p(programTitleText)
                    .withClasses(
                        ReferenceClasses.ADMIN_PROGRAM_CARD_TITLE,
                        "w-1/4",
                        "py-7",
                        "text-black",
                        "font-bold",
                        "text-xl"),
                statusDiv.withClasses(
                    "flex-grow",
                    "text-sm",
                    StyleUtils.responsiveLarge("text-base")));

    return div()
        .withClasses(
            ReferenceClasses.ADMIN_PROGRAM_CARD,
            "w-full",
            "my-4",
            "pl-6",
            "border",
            "border-gray-300",
            "rounded-lg")
        .with(
            titleAndStatus,
            p(programDescriptionText)
                .withClasses(
                    "w-3/4",
                    "mb-8",
                    "pt-4",
                    "line-clamp-3",
                    "text-gray-700",
                    "text-base"))
        // Add data attributes used for client-side sorting.
        .withData(
            "last-updated-millis",
            Long.toString(extractLastUpdated(draftProgram, activeProgram).toEpochMilli()))
        .withData("name", programTitleText);
  }

  private static Instant extractLastUpdated(
      Optional<ProgramDefinition> draftProgram, Optional<ProgramDefinition> activeProgram) {
    // Prefer when the draft was last updated, since active versions should be immutable after
    // being published.
    if (draftProgram.isEmpty() && activeProgram.isEmpty()) {
      throw new IllegalArgumentException("Program neither active nor draft.");
    }

    ProgramDefinition program = draftProgram.isPresent() ? draftProgram.get() : activeProgram.get();
    return program.lastModifiedTime().orElse(Instant.EPOCH);
  }

  ButtonTag renderShareLink(ProgramDefinition program) {
    String programLink =
        baseUrl
            + controllers.applicant.routes.RedirectController.programByName(program.slug()).url();
    return makeSvgTextButton("Share link", Icons.CONTENT_COPY)
        .withClass(AdminStyles.TERTIARY_BUTTON_STYLES)
        .withData("copyable-program-link", programLink);
  }

  ButtonTag renderEditLink(boolean isActive, ProgramDefinition program, Http.Request request) {
    String editLink = controllers.admin.routes.AdminProgramController.edit(program.id()).url();
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
        : asRedirectButton(button, editLink);
  }

  private ButtonTag renderManageTranslationsLink(ProgramDefinition program) {
    String linkDestination =
        routes.AdminProgramTranslationsController.edit(
                program.id(), LocalizedStrings.DEFAULT_LOCALE.toLanguageTag())
            .url();
    ButtonTag button =
        makeSvgTextButton("Manage translations", Icons.LANGUAGE)
            .withId("program-translations-link-" + program.id())
            .withClass(AdminStyles.TERTIARY_BUTTON_STYLES);
    return asRedirectButton(button, linkDestination);
  }

  private ButtonTag renderEditStatusesLink(ProgramDefinition program) {
    String linkDestination = routes.AdminProgramStatusesController.index(program.id()).url();
    ButtonTag button =
        makeSvgTextButton("Manage application statuses", Icons.FLAKY)
            .withClass(AdminStyles.TERTIARY_BUTTON_STYLES);
    return asRedirectButton(button, linkDestination);
  }

  private Optional<ButtonTag> maybeRenderViewApplicationsLink(
      ProgramDefinition activeProgram, Optional<CiviFormProfile> maybeUserProfile) {
    if (maybeUserProfile.isEmpty()) {
      return Optional.empty();
    }
    CiviFormProfile userProfile = maybeUserProfile.get();
    // TODO(#2582): Determine if this has N+1 query behavior and fix if
    // necessary.
    boolean userIsAuthorized;
    try {
      userProfile.checkProgramAuthorization(activeProgram.adminName()).join();
      userIsAuthorized = true;
    } catch (CompletionException e) {
      userIsAuthorized = false;
    }
    if (userIsAuthorized) {
      String editLink =
          routes.AdminApplicationController.index(
                  activeProgram.id(),
                  Optional.empty(),
                  Optional.empty(),
                  Optional.empty(),
                  Optional.empty())
              .url();

      ButtonTag button =
          makeSvgTextButton("Applications", Icons.TEXT_SNIPPET)
              .withId("program-view-apps-link-" + activeProgram.id())
              .withClass(AdminStyles.TERTIARY_BUTTON_STYLES);
      return Optional.of(asRedirectButton(button, editLink));
    }
    return Optional.empty();
  }

  private ButtonTag renderManageProgramAdminsLink(ProgramDefinition program) {
    String adminLink = routes.ProgramAdminManagementController.edit(program.id()).url();
    ButtonTag button =
        makeSvgTextButton("Manage admins", Icons.GROUP)
            .withId("manage-program-admin-link-" + program.id())
            .withClass(AdminStyles.TERTIARY_BUTTON_STYLES);
    return asRedirectButton(button, adminLink);
  }
}
