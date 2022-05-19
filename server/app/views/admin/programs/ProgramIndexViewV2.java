package views.admin.programs;

import static com.google.common.base.Preconditions.checkNotNull;
import static j2html.TagCreator.div;
import static j2html.TagCreator.each;
import static j2html.TagCreator.h1;
import static j2html.TagCreator.input;
import static j2html.TagCreator.label;
import static j2html.TagCreator.p;

import auth.CiviFormProfile;
import com.google.inject.Inject;
import com.typesafe.config.Config;
import controllers.admin.routes;
import j2html.tags.ContainerTag;
import j2html.tags.Tag;
import java.time.Instant;
import java.time.ZoneId;
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
import views.components.LinkElement;
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

  public Tag renderProgramListItem(
      Optional<ProgramDefinition> activeProgram,
      Optional<ProgramDefinition> draftProgram,
      Http.Request request,
      Optional<CiviFormProfile> profile) {
    String programStatusText = extractProgramStatusText(draftProgram, activeProgram);

    ProgramDefinition displayProgram = getDisplayProgram(draftProgram, activeProgram);

    String lastEditText =
        displayProgram.lastModifiedTime().isPresent()
            ? "Last updated: " + renderDateTime(displayProgram.lastModifiedTime().get(), zoneId)
            : "Could not find latest update time";
    String programTitleText = displayProgram.adminName();
    String programDescriptionText = displayProgram.adminDescription();
    String blockCountText = "Screens: " + displayProgram.getBlockCount();
    String questionCountText = "Questions: " + displayProgram.getQuestionCount();

    Tag topContent =
        div(
                div(
                    p(programStatusText).withClasses(Styles.TEXT_SM, Styles.TEXT_GRAY_700),
                    div(programTitleText)
                        .withClasses(
                            ReferenceClasses.ADMIN_PROGRAM_CARD_TITLE,
                            Styles.TEXT_BLACK,
                            Styles.FONT_BOLD,
                            Styles.TEXT_XL,
                            Styles.MB_2)),
                p().withClasses(Styles.FLEX_GROW),
                div(p(blockCountText), p(questionCountText))
                    .withClasses(
                        Styles.TEXT_RIGHT,
                        Styles.TEXT_XS,
                        Styles.TEXT_GRAY_700,
                        Styles.MR_2,
                        StyleUtils.applyUtilityClass(StyleUtils.RESPONSIVE_MD, Styles.MR_4)))
            .withClasses(Styles.FLEX);

    Tag midContent =
        div(programDescriptionText)
            .withClasses(Styles.TEXT_GRAY_700, Styles.TEXT_BASE, Styles.MB_8, Styles.LINE_CLAMP_3);

    Tag programDeepLink =
        label("Deep link, use this URL to link to this program from outside of CiviForm:")
            .withClasses(Styles.W_FULL)
            .with(
                input()
                    .withValue(
                        baseUrl
                            + controllers.applicant.routes.RedirectController.programByName(
                                    displayProgram.slug())
                                .url())
                    .attr("disabled", "readonly")
                    .withClasses(Styles.W_FULL, Styles.MB_2)
                    .withType("text"));

    Tag bottomContent =
        div(
                p(lastEditText).withClasses(Styles.TEXT_GRAY_700, Styles.ITALIC),
                p().withClasses(Styles.FLEX_GROW),
                maybeRenderManageTranslationsLink(draftProgram),
                maybeRenderEditLink(draftProgram, activeProgram, request),
                maybeRenderViewApplicationsLink(activeProgram, profile),
                renderManageProgramAdminsLink(draftProgram, activeProgram))
            .withClasses(Styles.FLEX, Styles.TEXT_SM, Styles.W_FULL);

    Tag innerDiv =
        div(topContent, midContent, programDeepLink, bottomContent)
            .withClasses(
                Styles.BORDER, Styles.BORDER_GRAY_300, Styles.BG_WHITE, Styles.ROUNDED, Styles.P_4);

    return div(innerDiv)
        .withClasses(
            ReferenceClasses.ADMIN_PROGRAM_CARD, Styles.W_FULL, Styles.SHADOW_LG, Styles.MB_4)
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

  private String extractProgramStatusText(
      Optional<ProgramDefinition> draftProgram, Optional<ProgramDefinition> activeProgram) {
    if (draftProgram.isPresent() && activeProgram.isPresent()) {
      return "Active, with draft";
    } else if (draftProgram.isPresent()) {
      return "Draft";
    } else if (activeProgram.isPresent()) {
      return "Active";
    }
    throw new IllegalArgumentException("Program neither active nor draft.");
  }

  Tag maybeRenderEditLink(
      Optional<ProgramDefinition> draftProgram,
      Optional<ProgramDefinition> activeProgram,
      Http.Request request) {
    String editLinkText = "Edit →";
    String newVersionText = "New Version";

    if (draftProgram.isPresent()) {
      String editLink =
          controllers.admin.routes.AdminProgramController.edit(draftProgram.get().id()).url();

      return new LinkElement()
          .setId("program-edit-link-" + draftProgram.get().id())
          .setHref(editLink)
          .setText(editLinkText)
          .setStyles(Styles.MR_2)
          .asAnchorText();
    } else if (activeProgram.isPresent()) {
      String newVersionLink =
          controllers.admin.routes.AdminProgramController.newVersionFrom(activeProgram.get().id())
              .url();

      return new LinkElement()
          .setId("program-new-version-link-" + activeProgram.get().id())
          .setHref(newVersionLink)
          .setText(newVersionText)
          .setStyles(Styles.MR_2)
          .asHiddenForm(request);
    } else {
      // obsolete or deleted, no edit link, empty div.
      return div();
    }
  }

  private Tag maybeRenderManageTranslationsLink(Optional<ProgramDefinition> draftProgram) {
    if (draftProgram.isPresent()) {
      String linkText = "Manage Translations →";
      String linkDestination =
          routes.AdminProgramTranslationsController.edit(
                  draftProgram.get().id(), LocalizedStrings.DEFAULT_LOCALE.toLanguageTag())
              .url();
      return new LinkElement()
          .setId("program-translations-link-" + draftProgram.get().id())
          .setHref(linkDestination)
          .setText(linkText)
          .setStyles(Styles.MR_2)
          .asAnchorText();
    } else {
      return div();
    }
  }

  private Tag maybeRenderViewApplicationsLink(
      Optional<ProgramDefinition> activeProgram, Optional<CiviFormProfile> userProfile) {
    if (activeProgram.isPresent() && userProfile.isPresent()) {
      boolean userIsAuthorized = true;
      try {
        userProfile.get().checkProgramAuthorization(activeProgram.get().adminName()).join();
      } catch (CompletionException e) {
        userIsAuthorized = false;
      }
      if (userIsAuthorized) {
        String editLink =
            routes.AdminApplicationController.index(
                    activeProgram.get().id(), Optional.empty(), Optional.empty())
                .url();

        return new LinkElement()
            .setId("program-view-apps-link-" + activeProgram.get().id())
            .setHref(editLink)
            .setText("Applications →")
            .setStyles(Styles.MR_2)
            .asAnchorText();
      }
    }
    return div();
  }

  private Tag renderManageProgramAdminsLink(
      Optional<ProgramDefinition> draftProgram, Optional<ProgramDefinition> activeProgram) {
    // We can use the ID of either, since we just add the program name and not ID to indicate
    // ownership.
    long programId =
        draftProgram.isPresent() ? draftProgram.get().id() : activeProgram.orElseThrow().id();
    String adminLink = routes.ProgramAdminManagementController.edit(programId).url();
    return new LinkElement()
        .setId("manage-program-admin-link-" + programId)
        .setHref(adminLink)
        .setText("Manage Admins →")
        .setStyles(Styles.MR_2)
        .asAnchorText();
  }
}
