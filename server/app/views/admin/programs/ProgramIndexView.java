package views.admin.programs;

import static com.google.common.base.Preconditions.checkNotNull;
import static j2html.TagCreator.div;
import static j2html.TagCreator.each;
import static j2html.TagCreator.fieldset;
import static j2html.TagCreator.form;
import static j2html.TagCreator.h1;
import static j2html.TagCreator.input;
import static j2html.TagCreator.label;
import static j2html.TagCreator.legend;
import static j2html.TagCreator.p;

import auth.CiviFormProfile;
import com.google.inject.Inject;
import com.typesafe.config.Config;
import controllers.admin.Filters;
import controllers.admin.routes;
import j2html.tags.ContainerTag;
import j2html.tags.Tag;
import java.time.Instant;
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
import views.components.LinkElement;
import views.components.Modal;
import views.style.BaseStyles;
import views.style.ReferenceClasses;
import views.style.StyleUtils;
import views.style.Styles;

/** Renders a page so the admin can view all active programs and draft programs. */
public final class ProgramIndexView extends BaseHtmlView {
  private final AdminLayout layout;
  private final String baseUrl;
  private final DateConverter dateConverter;

  @Inject
  public ProgramIndexView(
      AdminLayoutFactory layoutFactory, Config config, DateConverter dateConverter) {
    this.layout = checkNotNull(layoutFactory).getLayout(NavPage.PROGRAMS);
    this.baseUrl = checkNotNull(config).getString("base_url");
    this.dateConverter = checkNotNull(dateConverter);
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
            .with(maybeRenderPublishButton(programs, request));
    Modal publishAllModal =
        Modal.builder("publish-all-programs-modal", publishAllModalContent)
            .setModalTitle("Confirmation")
            .setTriggerButtonText("Publish all programs")
            .build();

    Modal demographicsCsvModal = renderDemographicsCsvModal();
    Tag contentDiv =
        div()
            .withClasses(Styles.PX_20)
            .with(
                h1(pageTitle).withClasses(Styles.MY_4),
                div()
                    .withClasses(Styles.FLEX, Styles.ITEMS_CENTER)
                    .with(renderNewProgramButton())
                    .with(div().withClass(Styles.FLEX_GROW))
                    .condWith(programs.anyDraft(), publishAllModal.getButton()),
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
                                    profile))))
            .with(demographicsCsvModal.getButton());

    HtmlBundle htmlBundle =
        layout
            .getBundle()
            .setTitle(pageTitle)
            .addMainContent(contentDiv)
            .addModals(publishAllModal, demographicsCsvModal)
            .addFooterScripts(layout.viewUtils.makeLocalJsTag("admin_programs"));
    return layout.renderCentered(htmlBundle);
  }

  private Modal renderDemographicsCsvModal() {
    String modalId = "download-demographics-csv-modal";
    String downloadActionText = "Download Exported Data (CSV)";

    ContainerTag downloadDemographicCsvModalContent =
        div()
            .withClasses(Styles.PX_8)
            .with(
                form()
                    .withMethod("GET")
                    .withAction(routes.AdminApplicationController.downloadDemographics().url())
                    .with(
                        p("This will download all applications for all programs and can take"
                                + " potentially be quite slow without filtering down the set of"
                                + " returned applications. Consider using the filters below to"
                                + " decrease the time to generate the report.")
                            .withClass(Styles.TEXT_SM),
                        fieldset()
                            .withClasses(Styles.MT_4, Styles.PT_1, Styles.PB_2, Styles.BORDER)
                            .with(
                                legend("Applications submitted").withClass(Styles.ML_3),
                                FieldWithLabel.date()
                                    .setFieldName(Filters.FROM_DATE_QUERY_PARAM)
                                    .setLabelText("From:")
                                    .getContainer()
                                    .withClasses(Styles.ML_3, Styles.INLINE_FLEX),
                                FieldWithLabel.date()
                                    .setFieldName(Filters.TO_DATE_QUERY_PARAM)
                                    .setLabelText("To:")
                                    .getContainer()
                                    .withClasses(Styles.ML_3, Styles.INLINE_FLEX)),
                        button(downloadActionText)
                            .withClasses(BaseStyles.MODAL_BUTTON, Styles.MT_6)
                            .withType("submit")));
    return Modal.builder(modalId, downloadDemographicCsvModalContent)
        .setModalTitle(downloadActionText)
        .build();
  }

  private Tag maybeRenderPublishButton(ActiveAndDraftPrograms programs, Http.Request request) {
    // We should only render the publish button if there is at least one draft.
    if (programs.anyDraft()) {
      String link = routes.AdminProgramController.publish().url();
      return new LinkElement()
          .setId("publish-programs-button")
          .setHref(link)
          .setText("Publish all drafts")
          .asHiddenForm(request);
    } else {
      return div();
    }
  }

  private Tag renderNewProgramButton() {
    String link = controllers.admin.routes.AdminProgramController.newOne().url();
    return new LinkElement()
        .setId("new-program-button")
        .setHref(link)
        .setText("Create new program")
        .asButton();
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
            ? "Last updated: "
                + dateConverter.renderDateTime(displayProgram.lastModifiedTime().get())
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
    // TODO(#2582): Determine if this has N+1 query behavior and fix if
    // necessary.
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
