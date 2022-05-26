package views.admin.programs;

import static com.google.common.base.Preconditions.checkNotNull;
import static j2html.TagCreator.div;
import static j2html.TagCreator.each;
import static j2html.TagCreator.h1;
import static j2html.TagCreator.p;
import static j2html.TagCreator.span;

import auth.CiviFormProfile;
import com.google.common.collect.Lists;
import com.google.inject.Inject;
import com.typesafe.config.Config;
import controllers.admin.routes;
import j2html.tags.ContainerTag;
import j2html.tags.Tag;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletionException;
import play.mvc.Http;
import play.twirl.api.Content;
import services.LocalizedStrings;
import services.program.ActiveAndDraftPrograms;
import services.program.ProgramDefinition;
import views.BaseHtmlView;
import views.HtmlBundle;
import views.admin.AdminLayout;
import views.admin.AdminLayout.NavPage;
import views.admin.AdminLayoutFactory;
import views.components.Icons;
import views.components.Modal;
import views.style.AdminStyles;
import views.style.BaseStyles;
import views.style.ReferenceClasses;
import views.style.StyleUtils;
import views.style.Styles;

/** Renders a page so the admin can view all active programs and draft programs. */
public final class ProgramIndexViewV2 extends BaseHtmlView {
  private final AdminLayout layout;
  private final String baseUrl;
  private final ZoneId zoneId;

  @Inject
  public ProgramIndexViewV2(AdminLayoutFactory layoutFactory, Config config, ZoneId zoneId) {
    this.layout = checkNotNull(layoutFactory).getLayout(NavPage.PROGRAMS);
    this.baseUrl = checkNotNull(config).getString("base_url");
    this.zoneId = checkNotNull(zoneId);
  }

  public Content render(
      ActiveAndDraftPrograms programs, Http.Request request, Optional<CiviFormProfile> profile) {
    if (profile.isPresent() && profile.get().isProgramAdmin() && !profile.get().isCiviFormAdmin()) {
      layout.setOnlyProgramAdminType();
    }

    String pageTitle = "All programs";
    Optional<Modal> maybePublishModal = maybeRenderPublishModal(programs, request);

    Tag contentDiv =
        div()
            .withClasses(Styles.PX_4)
            .with(
                div()
                    .withClasses(
                        Styles.FLEX,
                        Styles.ITEMS_CENTER,
                        Styles.SPACE_X_4,
                        Styles.MT_12,
                        Styles.MB_10)
                    .with(
                        h1(pageTitle),
                        div().withClass(Styles.FLEX_GROW),
                        renderDownloadExportCsvButton(),
                        renderNewProgramButton(),
                        maybePublishModal.isPresent() ? maybePublishModal.get().getButton() : null),
                div()
                    .withClasses(ReferenceClasses.ADMIN_PROGRAM_CARD_LIST, Styles.INVISIBLE)
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
            .addFooterScripts(layout.viewUtils.makeLocalJsTag("admin_programs"));
    maybePublishModal.ifPresent(htmlBundle::addModals);
    return layout.renderCentered(htmlBundle);
  }

  private Tag renderDownloadExportCsvButton() {
    String link = routes.AdminApplicationController.downloadDemographics().url();
    ContainerTag button =
        makeSvgTextButton("Download Exported Data (CSV)", Icons.DOWNLOAD_SVG_PATH)
            .withId("download-export-csv-button")
            .withClasses(AdminStyles.SECONDARY_BUTTON_STYLES, Styles.MY_2);
    return asRedirectButton(button, link);
  }

  private ContainerTag makePublishButton() {
    return makeSvgTextButton("Publish all drafts", Icons.PUBLISH_SVG_PATH)
        .withId("publish-programs-button")
        .withClasses(AdminStyles.PRIMARY_BUTTON_STYLES, Styles.MY_2);
  }

  private Optional<Modal> maybeRenderPublishModal(
      ActiveAndDraftPrograms programs, Http.Request request) {
    // We should only render the publish modal / button if there is at least one draft.
    if (!programs.anyDraft()) {
      return Optional.empty();
    }

    String link = routes.AdminProgramController.publish().url();

    ContainerTag publishAllModalContent =
        div()
            .withClasses(Styles.FLEX, Styles.FLEX_COL, Styles.GAP_4, Styles.PX_2)
            .with(p("Are you sure you want to publish all programs?").withClasses(Styles.P_2))
            .with(div().with(toLinkButtonForPost(makePublishButton(), link, request)));
    Modal publishAllModal =
        Modal.builder("publish-all-programs-modal", publishAllModalContent)
            .setModalTitle("Confirmation")
            .setTriggerButtonContent(makePublishButton())
            .build();
    return Optional.of(publishAllModal);
  }

  private Tag renderNewProgramButton() {
    String link = controllers.admin.routes.AdminProgramController.newOne().url();
    ContainerTag button =
        makeSvgTextButton("Create new program", Icons.ADD_SVG_PATH)
            .withId("new-program-button")
            .withClasses(AdminStyles.SECONDARY_BUTTON_STYLES, Styles.MY_2);
    return asRedirectButton(button, link);
  }

  public ProgramDefinition getDisplayProgram(
      Optional<ProgramDefinition> draftProgram, Optional<ProgramDefinition> activeProgram) {
    if (draftProgram.isPresent()) {
      return draftProgram.get();
    }
    return activeProgram.get();
  }

  private Tag renderProgramRow(
      boolean isActive,
      ProgramDefinition program,
      List<Tag> actions,
      List<Tag> extraActions,
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

    String formattedUpdateTime = updatedTime.map(t -> renderDateTime(t, zoneId)).orElse("unknown");

    int blockCount = program.getBlockCount();
    int questionCount = program.getQuestionCount();

    String extraActionsButtonId = "extra-actions-" + program.id();
    ContainerTag extraActionsButton =
        makeSvgTextButton("", Icons.MORE_VERT_PATH)
            .withId(extraActionsButtonId)
            .withClasses(
                AdminStyles.TERTIARY_BUTTON_STYLES,
                ReferenceClasses.WITH_DROPDOWN,
                Styles.H_12,
                extraActions.size() == 0 ? Styles.INVISIBLE : "");

    return div()
        .withClasses(
            Styles.PY_7,
            Styles.SPACE_X_10,
            Styles.FLEX,
            Styles.FLEX_ROW,
            StyleUtils.hover(Styles.BG_GRAY_100),
            StyleUtils.joinStyles(extraStyles))
        .with(
            p().withClasses(
                    badgeBGColor,
                    badgeFillColor,
                    Styles.ML_8,
                    Styles.FONT_MEDIUM,
                    Styles.ROUNDED_FULL,
                    Styles.FLEX,
                    Styles.FLEX_ROW,
                    Styles.GAP_X_2,
                    Styles.PLACE_ITEMS_CENTER,
                    Styles.JUSTIFY_CENTER)
                .withStyle("min-width:90px")
                .with(
                    Icons.svg(Icons.NOISE_CONTROL_OFF_SVG_PATH, 20)
                        .withClasses(Styles.INLINE_BLOCK, Styles.ML_3_5),
                    span(badgeText).withClass(Styles.MR_4)),
            div()
                .with(
                    p().with(
                            span(updatedPrefix),
                            span(formattedUpdateTime).withClass(Styles.FONT_SEMIBOLD)),
                    p().with(
                            span(String.format("%d", blockCount)).withClass(Styles.FONT_SEMIBOLD),
                            span(blockCount == 1 ? " screen, " : " screens, "),
                            span(String.format("%d", questionCount))
                                .withClass(Styles.FONT_SEMIBOLD),
                            span(questionCount == 1 ? " question" : " questions"))),
            div().withClass(Styles.FLEX_GROW),
            div()
                .withClasses(Styles.FLEX, Styles.SPACE_X_2, Styles.PR_6, Styles.FONT_MEDIUM)
                .with(actions)
                .with(
                    div()
                        .withClass(Styles.RELATIVE)
                        .with(
                            extraActionsButton,
                            div()
                                .withId(extraActionsButtonId + "-dropdown")
                                .withClasses(
                                    Styles.HIDDEN,
                                    Styles.BORDER,
                                    Styles.BG_WHITE,
                                    Styles.ABSOLUTE,
                                    Styles.RIGHT_0)
                                .with(extraActions))));
  }

  public Tag renderProgramListItem(
      Optional<ProgramDefinition> activeProgram,
      Optional<ProgramDefinition> draftProgram,
      Http.Request request,
      Optional<CiviFormProfile> profile) {
    ProgramDefinition displayProgram = getDisplayProgram(draftProgram, activeProgram);

    String programTitleText = displayProgram.adminName();
    String programDescriptionText = displayProgram.adminDescription();

    ContainerTag statusDiv = div();
    if (draftProgram.isPresent()) {
      List<Tag> draftRowActions = Lists.newArrayList();
      List<Tag> draftRowExtraActions = Lists.newArrayList();
      draftRowActions.add(renderEditLink(/* isActive = */ false, draftProgram.get(), request));
      draftRowExtraActions.add(renderManageProgramAdminsLink(draftProgram.get()));
      draftRowExtraActions.add(renderManageTranslationsLink(draftProgram.get()));
      statusDiv =
          statusDiv.with(
              renderProgramRow(
                  /* isActive = */ false,
                  draftProgram.get(),
                  draftRowActions,
                  draftRowExtraActions));
    }

    if (activeProgram.isPresent()) {
      List<Tag> activeRowActions = Lists.newArrayList();
      List<Tag> activeRowExtraActions = Lists.newArrayList();
      Optional<Tag> applicationsLink =
          maybeRenderViewApplicationsLink(activeProgram.get(), profile.get());
      applicationsLink.ifPresent(activeRowExtraActions::add);
      if (!draftProgram.isPresent()) {
        activeRowActions.add(renderEditLink(/* isActive = */ true, activeProgram.get(), request));
        activeRowExtraActions.add(renderManageProgramAdminsLink(activeProgram.get()));
      }
      activeRowActions.add(renderViewLink(activeProgram.get()));
      statusDiv =
          statusDiv.with(
              renderProgramRow(
                  /* isActive = */ true,
                  activeProgram.get(),
                  activeRowActions,
                  activeRowExtraActions,
                  draftProgram.isPresent() ? Styles.BORDER_T : ""));
    }

    Tag titleAndStatus =
        div()
            .withClass(Styles.FLEX)
            .with(
                p(programTitleText)
                    .withClasses(
                        ReferenceClasses.ADMIN_PROGRAM_CARD_TITLE,
                        Styles.W_1_4,
                        Styles.PY_7,
                        Styles.TEXT_BLACK,
                        Styles.FONT_BOLD,
                        Styles.TEXT_XL),
                statusDiv.withClass(Styles.FLEX_GROW));

    return div()
        .withClasses(
            ReferenceClasses.ADMIN_PROGRAM_CARD,
            Styles.W_FULL,
            Styles.MY_4,
            Styles.PL_6,
            Styles.BORDER,
            Styles.BORDER_GRAY_300,
            Styles.ROUNDED_LG)
        .with(
            titleAndStatus,
            p(programDescriptionText)
                .withClasses(
                    Styles.W_3_4,
                    Styles.MB_8,
                    Styles.PT_4,
                    Styles.LINE_CLAMP_3,
                    Styles.TEXT_GRAY_700,
                    Styles.TEXT_BASE))
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

  Tag renderViewLink(ProgramDefinition program) {
    // TODO(#1238): Rather than navigating to the program link
    // consider renaming the action and copying the link to the clipboard
    // or opening it in a new tab.
    String viewLink =
        baseUrl
            + controllers.applicant.routes.RedirectController.programByName(program.slug()).url();
    ContainerTag button =
        makeSvgTextButton("View", Icons.VISIBILITY_SVG_PATH)
            .withClass(AdminStyles.TERTIARY_BUTTON_STYLES);
    return asRedirectButton(button, viewLink);
  }

  Tag renderEditLink(boolean isActive, ProgramDefinition program, Http.Request request) {
    String editLink = controllers.admin.routes.AdminProgramController.edit(program.id()).url();
    String editLinkId = "program-edit-link-" + program.id();
    if (isActive) {
      editLink = controllers.admin.routes.AdminProgramController.newVersionFrom(program.id()).url();
      editLinkId = "program-new-version-link-" + program.id();
    }

    ContainerTag button =
        makeSvgTextButton("Edit", Icons.EDIT_SVG_PATH)
            .withId(editLinkId)
            .withClasses(AdminStyles.TERTIARY_BUTTON_STYLES);
    return isActive
        ? toLinkButtonForPost(button, editLink, request)
        : asRedirectButton(button, editLink);
  }

  private Tag renderManageTranslationsLink(ProgramDefinition program) {
    String linkDestination =
        routes.AdminProgramTranslationsController.edit(
                program.id(), LocalizedStrings.DEFAULT_LOCALE.toLanguageTag())
            .url();
    ContainerTag button =
        makeSvgTextButton("Manage translations", Icons.LANGUAGE_SVG_PATH)
            .withId("program-translations-link-" + program.id())
            .withClass(AdminStyles.TERTIARY_BUTTON_STYLES);
    return asRedirectButton(button, linkDestination);
  }

  private Optional<Tag> maybeRenderViewApplicationsLink(
      ProgramDefinition activeProgram, CiviFormProfile userProfile) {
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
                  activeProgram.id(), Optional.empty(), Optional.empty())
              .url();

      ContainerTag button =
          makeSvgTextButton("Applications", Icons.TEXT_SNIPPET_SVG_PATH)
              .withId("program-view-apps-link-" + activeProgram.id())
              .withClass(AdminStyles.TERTIARY_BUTTON_STYLES);
      return Optional.of(asRedirectButton(button, editLink));
    }
    return Optional.empty();
  }

  private Tag renderManageProgramAdminsLink(ProgramDefinition program) {
    String adminLink = routes.ProgramAdminManagementController.edit(program.id()).url();
    ContainerTag button =
        makeSvgTextButton("Manage admins", Icons.GROUP_SVG_PATH)
            .withId("manage-program-admin-link-" + program.id())
            .withClass(AdminStyles.TERTIARY_BUTTON_STYLES);
    return asRedirectButton(button, adminLink);
  }
}
