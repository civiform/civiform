package views.admin.programs;

import static com.google.common.base.Preconditions.checkNotNull;
import static j2html.TagCreator.a;
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
import services.program.ActiveAndDraftPrograms;
import services.LocalizedStrings;
import services.program.ProgramDefinition;
import views.BaseHtmlView;
import views.HtmlBundle;
import views.admin.AdminLayout;
import views.components.ActionButton;
import views.components.ActionButton.ActionType;
import views.components.Icons;
import views.components.Modal;
import views.style.ReferenceClasses;
import views.style.StyleUtils;
import views.style.Styles;

/** Renders a page so the admin can view all active programs and draft programs. */
public final class ProgramIndexViewV2 extends BaseHtmlView {
  private final AdminLayout layout;
  private final String baseUrl;
  private final ZoneId zoneId;

  @Inject
  public ProgramIndexViewV2(AdminLayout layout, Config config, ZoneId zoneId) {
    this.layout = checkNotNull(layout);
    this.baseUrl = checkNotNull(config).getString("base_url");
    this.zoneId = checkNotNull(zoneId);
  }

  public Content render(
      ActiveAndDraftPrograms programs, Http.Request request, Optional<CiviFormProfile> profile) {
    if (profile.isPresent() && profile.get().isProgramAdmin() && !profile.get().isCiviFormAdmin()) {
      layout.setOnlyProgramAdminType();
    }

    String pageTitle = "All programs";
    ContainerTag publishAllModalContent =
        div()
            .withClasses(Styles.FLEX, Styles.FLEX_COL, Styles.GAP_4)
            .with(p("Are you sure you want to publish all programs?").withClasses(Styles.P_2))
            .with(maybeRenderPublishButton(programs));
    Modal publishAllModal =
        Modal.builder("publish-all-programs-modal", publishAllModalContent)
            .setModalTitle("Confirmation")
            .setTriggerButtonText("Publish all programs")
            .build();

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
                        maybeRenderPublishButton(programs)),
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
            .addModals(publishAllModal)
            .addFooterScripts(layout.viewUtils.makeLocalJsTag("admin_programs"));
    return layout.renderCentered(htmlBundle);
  }

  private Tag renderDownloadExportCsvButton() {
    return ActionButton.builder()
        .setId("download-export-csv-button")
        .setActionType(ActionType.SECONDARY)
        .setSvgRef(Icons.DOWNLOAD_SVG_PATH)
        .setText("Download Exported Data (CSV)")
        .setOnClickJS(
            ActionButton.navigateToJS(
                routes.AdminApplicationController.downloadDemographics().url()))
        .build()
        .render();
  }

  private Tag maybeRenderPublishButton(ActiveAndDraftPrograms programs) {
    // We should only render the publish button if there is at least one draft.
    if (!programs.anyDraft()) {
      return null;
    }
    // TODO(#1238): Previously the form submit was on this element rather than
    // on the modal. Move it to the modal, since the user has to accept within
    // the modal.
    String link = routes.AdminProgramController.publish().url();
    return ActionButton.builder()
        .setId("publish-programs-button")
        .setActionType(ActionType.PRIMARY)
        .setSvgRef(Icons.PUBLISH_SVG_PATH)
        .setText("Publish all drafts")
        .setOnClickJS(ActionButton.navigateToJS(link))
        .build()
        .render();
  }

  private Tag renderNewProgramButton() {
    String link = controllers.admin.routes.AdminProgramController.newOne().url();
    return ActionButton.builder()
        .setId("new-program-button")
        .setActionType(ActionType.SECONDARY)
        .setSvgRef(Icons.ADD_SVG_PATH)
        .setText("Create new program")
        .setOnClickJS(ActionButton.navigateToJS(link))
        .build()
        .render();
  }

  public ProgramDefinition getDisplayProgram(
      Optional<ProgramDefinition> draftProgram, Optional<ProgramDefinition> activeProgram) {
    if (draftProgram.isPresent()) {
      return draftProgram.get();
    }
    return activeProgram.get();
  }

  private Tag renderProgramRow(boolean isActive, ProgramDefinition program, List<Tag> actions, List<Tag> extraActions) {
    String badgeText = "Draft";
    String badgeBGColor = Styles.BG_PURPLE_300;
    String badgeFillColor = Styles.TEXT_PURPLE_700;
    String updatedPrefix = "Edited on ";
    Optional<Instant> updatedTime = program.lastModifiedTime();
    if (isActive) {
      badgeText = "Active";
      badgeBGColor = Styles.BG_GREEN_300;
      badgeFillColor = Styles.TEXT_GREEN_700;
      updatedPrefix = "Published on ";
    }

    String formattedUpdateTime =
        updatedTime.isPresent()
            ? renderDateTime(updatedTime.get(), zoneId)
            : "unknown";

    int blockCount = program.getBlockCount();
    int questionCount = program.getQuestionCount();

    return div()
        .withClasses(
            Styles.PY_7,
            Styles.SPACE_X_10,
            Styles.FLEX,
            Styles.FLEX_ROW,
            StyleUtils.hover(Styles.BG_GRAY_100))
        .with(
            p().withClasses(
                    // TODO(#1238): min-width:90px, custom background color
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
                        // TODO(#1238): Technically should be ML_3_5, but that
                        // isn't available yet. Check whether 2px off is ok.
                        .withClasses(Styles.INLINE_BLOCK, Styles.ML_3),
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
            div().withClasses(Styles.FLEX, Styles.SPACE_X_8, Styles.FONT_MEDIUM)
              .with(actions)
              .condWith(extraActions.size() > 0, a().with(Icons.svg(Icons.MORE_VERT_PATH, 18))));
  }

  public Tag renderProgramListItem(
      Optional<ProgramDefinition> activeProgram,
      Optional<ProgramDefinition> draftProgram,
      Http.Request request,
      Optional<CiviFormProfile> profile) {
    ProgramDefinition displayProgram = getDisplayProgram(draftProgram, activeProgram);

    String programTitleText = displayProgram.adminName();
    String programDescriptionText = displayProgram.adminDescription();

    Tag draftRow = null;
    if (draftProgram.isPresent()) {
      List<Tag> draftRowActions = Lists.newArrayList();
      List<Tag> draftRowExtraActions = Lists.newArrayList();
      draftRowActions.add(renderEditLink(false, draftProgram.get(), request));
      draftRowExtraActions.add(renderManageProgramAdminsLink(draftProgram.get()));
      draftRowExtraActions.add(renderManageTranslationsLink(draftProgram.get()));
      draftRow = renderProgramRow(false, draftProgram.get(), draftRowActions, draftRowExtraActions);
    }

    Tag activeRow = null;
    if (activeProgram.isPresent()) {
      List<Tag> activeRowActions = Lists.newArrayList();
      List<Tag> activeRowExtraActions = Lists.newArrayList();
      activeRowActions.add(maybeRenderViewApplicationsLink(activeProgram.get(), profile.get()));
      if (!draftProgram.isPresent()) {
        activeRowActions.add(renderEditLink(true, activeProgram.get(), request));
        activeRowExtraActions.add(renderManageProgramAdminsLink(activeProgram.get()));
      }
      activeRowActions.add(renderViewLink(activeProgram.get()));
      activeRow = renderProgramRow(true, activeProgram.get(), activeRowActions, activeRowExtraActions);
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
                div().withClass(Styles.FLEX_GROW).with(draftRow, activeRow));

    return div()
        .withClasses(
            ReferenceClasses.ADMIN_PROGRAM_CARD,
            Styles.W_FULL,
            Styles.MY_4,
            Styles.PX_6,
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
    String viewLink =
        baseUrl
            + controllers.applicant.routes.RedirectController.programByName(program.slug()).url();
    return a().withHref(viewLink)
        .with(
            Icons.svg(Icons.VISIBILITY_SVG_PATH, 20).withClasses(Styles.INLINE_BLOCK),
            span("View").withClass(Styles.PL_1));
  }

  Tag renderEditLink(boolean isActive, ProgramDefinition program, Http.Request request) {
    String editLink = controllers.admin.routes.AdminProgramController.edit(program.id()).url();
    String editLinkId = "program-edit-link-" + program.id();
    if (isActive) {
      editLink = controllers.admin.routes.AdminProgramController.newVersionFrom(program.id()).url();
      editLinkId = "program-new-version-link-" + program.id();
    }

    // TODO(#1238): Add this to ActionButton as a new style. Also add ability to render
    // as a hidden form when creating a new version.
    // Also consider adding an editOrNewVersion URL to remove the need to have distinct
    // URLs.
    return a().withId(editLinkId)
        .withHref(editLink)
        .with(
            Icons.svg(Icons.EDIT_SVG_PATH, 20).withClasses(Styles.INLINE_BLOCK),
            span("Edit").withClass(Styles.PL_1));
  }

  private Tag renderManageTranslationsLink(ProgramDefinition program) {
    String linkDestination =
        routes.AdminProgramTranslationsController.edit(
                program.id(), LocalizedStrings.DEFAULT_LOCALE.toLanguageTag())
            .url();
    return a()
        .withId("program-translations-link-" + program.id())
        .withHref(linkDestination)
        .with(
          Icons.svg(Icons.LANGUAGE_SVG_PATH, 20).withClasses(Styles.INLINE_BLOCK),
          span("Manage translations").withClasses(Styles.PL_1));
  }

  private Tag maybeRenderViewApplicationsLink(
      ProgramDefinition activeProgram, CiviFormProfile userProfile) {
    boolean userIsAuthorized = true;
    try {
      userProfile.checkProgramAuthorization(activeProgram.adminName()).join();
    } catch (CompletionException e) {
      userIsAuthorized = false;
    }
    if (userIsAuthorized) {
      String editLink =
          routes.AdminApplicationController.index(
                  activeProgram.id(), Optional.empty(), Optional.empty())
              .url();

      return a().withId("program-view-apps-link-" + activeProgram.id())
          .withHref(editLink)
          .with(
              Icons.svg(Icons.TEXT_SNIPPET_SVG_PATH, 20).withClasses(Styles.INLINE_BLOCK),
              span("Applications").withClass(Styles.PL_1));
    }
    return null;
  }

  private Tag renderManageProgramAdminsLink(ProgramDefinition program) {
    String adminLink = routes.ProgramAdminManagementController.edit(program.id()).url();
    return a()
        .withId("manage-program-admin-link-" + program.id())
        .withHref(adminLink)
        .with(
          Icons.svg(Icons.GROUP_SVG_PATH, 20).withClasses(Styles.INLINE_BLOCK),
          span("Manage admins").withClass(Styles.PL_1)
        );
  }
}
