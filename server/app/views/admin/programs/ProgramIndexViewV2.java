package views.admin.programs;

import static com.google.common.base.Preconditions.checkNotNull;
import static j2html.TagCreator.div;
import static j2html.TagCreator.each;
import static j2html.TagCreator.h1;
import static j2html.TagCreator.p;
import static j2html.TagCreator.span;

import auth.CiviFormProfile;
import com.google.common.collect.ImmutableSet;
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
import views.components.ActionButton;
import views.components.ActionButton.ActionType;
import views.components.Icons;
import views.components.Modal;
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
    if (maybePublishModal.isPresent()) {
      htmlBundle = htmlBundle.addModals(maybePublishModal.get());
    }
    return layout.renderCentered(htmlBundle);
  }

  private Tag renderDownloadExportCsvButton() {
    return ActionButton.builder()
        .setId("download-export-csv-button")
        .setActionType(ActionType.SECONDARY)
        .setExtraStyles(ImmutableSet.of(Styles.MY_2))
        .setSvgRef(Icons.DOWNLOAD_SVG_PATH)
        .setText("Download Exported Data (CSV)")
        .build()
        .renderAsLinkButton(routes.AdminApplicationController.downloadDemographics().url());
  }

  private Optional<Modal> maybeRenderPublishModal(
      ActiveAndDraftPrograms programs, Http.Request request) {
    // We should only render the publish modal / button if there is at least one draft.
    if (!programs.anyDraft()) {
      return Optional.empty();
    }
    String link = routes.AdminProgramController.publish().url();
    ActionButton publishButton =
        ActionButton.builder()
            .setId("publish-programs-button")
            .setActionType(ActionType.PRIMARY)
            .setExtraStyles(ImmutableSet.of(Styles.MY_2))
            .setSvgRef(Icons.PUBLISH_SVG_PATH)
            .setText("Publish all drafts")
            .build();

    ContainerTag publishAllModalContent =
        div()
            .withClasses(Styles.FLEX, Styles.FLEX_COL, Styles.GAP_4)
            .with(p("Are you sure you want to publish all programs?").withClasses(Styles.P_2))
            .with(publishButton.renderAsLinkButtonHiddenForm(link, request));
    Modal publishAllModal =
        Modal.builder("publish-all-programs-modal", publishAllModalContent)
            .setModalTitle("Confirmation")
            .setTriggerButtonContent(publishButton.renderAsCustomJSButton(""))
            .build();
    return Optional.of(publishAllModal);
  }

  private Tag renderNewProgramButton() {
    String link = controllers.admin.routes.AdminProgramController.newOne().url();
    return ActionButton.builder()
        .setId("new-program-button")
        .setActionType(ActionType.SECONDARY)
        .setExtraStyles(ImmutableSet.of(Styles.MY_2))
        .setSvgRef(Icons.ADD_SVG_PATH)
        .setText("Create new program")
        .build()
        .renderAsLinkButton(link);
  }

  public ProgramDefinition getDisplayProgram(
      Optional<ProgramDefinition> draftProgram, Optional<ProgramDefinition> activeProgram) {
    if (draftProgram.isPresent()) {
      return draftProgram.get();
    }
    return activeProgram.get();
  }

  private Tag renderProgramRow(
      boolean isActive, ProgramDefinition program, List<Tag> actions, List<Tag> extraActions) {
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
        updatedTime.isPresent() ? renderDateTime(updatedTime.get(), zoneId) : "unknown";

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
                    // TODO(#1238): min-width:90px
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
                .withClasses(Styles.FLEX, Styles.SPACE_X_2, Styles.FONT_MEDIUM)
                .with(actions)
                .condWith(
                    extraActions.size() > 0,
                    div()
                        .withClass(Styles.RELATIVE)
                        .with(
                            ActionButton.builder()
                                .setId("extra-actions-" + program.id())
                                .setActionType(ActionType.TERTIARY)
                                .setExtraStyles(ImmutableSet.of(ReferenceClasses.WITH_DROPDOWN))
                                .setSvgRef(Icons.MORE_VERT_PATH)
                                .build()
                                .renderAsCustomJSButton(""),
                            div()
                                .withId("extra-actions-" + program.id() + "-dropdown")
                                .withClasses(Styles.HIDDEN, Styles.ABSOLUTE, Styles.RIGHT_0)
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
      activeRow =
          renderProgramRow(true, activeProgram.get(), activeRowActions, activeRowExtraActions);
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
    return ActionButton.builder()
        .setActionType(ActionType.TERTIARY)
        .setSvgRef(Icons.VISIBILITY_SVG_PATH)
        .setText("View")
        .build()
        .renderAsLinkButton(viewLink);
  }

  Tag renderEditLink(boolean isActive, ProgramDefinition program, Http.Request request) {
    String editLink = controllers.admin.routes.AdminProgramController.edit(program.id()).url();
    String editLinkId = "program-edit-link-" + program.id();
    if (isActive) {
      editLink = controllers.admin.routes.AdminProgramController.newVersionFrom(program.id()).url();
      editLinkId = "program-new-version-link-" + program.id();
    }

    ActionButton button =
        ActionButton.builder()
            .setId(editLinkId)
            .setActionType(ActionType.TERTIARY)
            .setSvgRef(Icons.EDIT_SVG_PATH)
            .setText("Edit")
            .build();
    return isActive
        ? button.renderAsLinkButtonHiddenForm(editLink, request)
        : button.renderAsLinkButton(editLink);
  }

  private Tag renderManageTranslationsLink(ProgramDefinition program) {
    String linkDestination =
        routes.AdminProgramTranslationsController.edit(
                program.id(), LocalizedStrings.DEFAULT_LOCALE.toLanguageTag())
            .url();
    return ActionButton.builder()
        .setId("program-translations-link-" + program.id())
        .setActionType(ActionType.TERTIARY)
        .setSvgRef(Icons.LANGUAGE_SVG_PATH)
        .setText("Manage translations")
        .build()
        .renderAsLinkButton(linkDestination);
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

      return ActionButton.builder()
          .setId("program-view-apps-link-" + activeProgram.id())
          .setActionType(ActionType.TERTIARY)
          .setSvgRef(Icons.TEXT_SNIPPET_SVG_PATH)
          .setText("Applications")
          .build()
          .renderAsLinkButton(editLink);
    }
    return null;
  }

  private Tag renderManageProgramAdminsLink(ProgramDefinition program) {
    String adminLink = routes.ProgramAdminManagementController.edit(program.id()).url();
    return ActionButton.builder()
        .setId("manage-program-admin-link-" + program.id())
        .setActionType(ActionType.TERTIARY)
        .setSvgRef(Icons.GROUP_SVG_PATH)
        .setText("Manage admins")
        .build()
        .renderAsLinkButton(adminLink);
  }
}
