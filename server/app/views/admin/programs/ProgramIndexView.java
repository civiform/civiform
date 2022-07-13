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
import j2html.tags.Tag;
import j2html.tags.specialized.ATag;
import j2html.tags.specialized.DivTag;
import j2html.tags.specialized.FormTag;
import j2html.tags.specialized.LabelTag;
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
    DivTag publishAllModalContent =
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
    DivTag contentDiv =
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
                        demographicsCsvModal
                            .getButton()
                            .withClasses(AdminStyles.SECONDARY_BUTTON_STYLES, Styles.MY_2),
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
            .withClasses(Styles.PX_8)
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
                            .withClass(Styles.TEXT_SM),
                        fieldset()
                            .withClasses(Styles.MT_4, Styles.PT_1, Styles.PB_2, Styles.BORDER)
                            .with(
                                legend("Applications submitted").withClass(Styles.ML_3),
                                // The field names below should be kept in sync with
                                // AdminApplicationController.downloadDemographics.
                                FieldWithLabel.date()
                                    .setFieldName("fromDate")
                                    .setLabelText("From:")
                                    .getDateTag()
                                    .withClasses(Styles.ML_3, Styles.INLINE_FLEX),
                                FieldWithLabel.date()
                                    .setFieldName("untilDate")
                                    .setLabelText("To:")
                                    .getDateTag()
                                    .withClasses(Styles.ML_3, Styles.INLINE_FLEX)),
                        makeSvgTextButton(downloadActionText, Icons.DOWNLOAD)
                            .withClasses(AdminStyles.PRIMARY_BUTTON_STYLES, Styles.MT_6)
                            .withType("submit")));
    return Modal.builder(modalId, downloadDemographicCsvModalContent)
        .setModalTitle(downloadActionText)
        .setTriggerButtonContent(makeSvgTextButton(downloadActionText, Icons.DOWNLOAD))
        .build();
  }

  private Tag<?> maybeRenderPublishButton(ActiveAndDraftPrograms programs, Http.Request request) {
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

  private ATag renderNewProgramButton() {
    String link = controllers.admin.routes.AdminProgramController.newOne().url();
    ContainerTag button =
        makeSvgTextButton("Create new program", Icons.ADD)
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

  public DivTag renderProgramListItem(
      Optional<ProgramDefinition> activeProgram,
      Optional<ProgramDefinition> draftProgram,
      Http.Request request,
      Optional<CiviFormProfile> profile) {
    ProgramDefinition displayProgram = getDisplayProgram(draftProgram, activeProgram);

    String programTitleText = displayProgram.adminName();
    String programDescriptionText = displayProgram.adminDescription();

    DivTag topContent =
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

    DivTag midContent =
        div(programDescriptionText)
            .withClasses(Styles.TEXT_GRAY_700, Styles.TEXT_BASE, Styles.MB_8, Styles.LINE_CLAMP_3);

    LabelTag programDeepLink =
        label("Deep link, use this URL to link to this program from outside of CiviForm:")
            .withClasses(Styles.W_FULL)
            .with(
                input()
                    .withValue(
                        baseUrl
                            + controllers.applicant.routes.RedirectController.programByName(
                                    displayProgram.slug())
                                .url())
                    .isDisabled()
                    .isReadonly()
                    .withClasses(Styles.W_FULL, Styles.MB_2)
                    .withType("text"));

    DivTag bottomContent =
        div(
                p(lastEditText).withClasses(Styles.TEXT_GRAY_700, Styles.ITALIC),
                p().withClasses(Styles.FLEX_GROW),
                maybeRenderEditStatusesLink(draftProgram),
                maybeRenderManageTranslationsLink(draftProgram),
                maybeRenderEditLink(draftProgram, activeProgram, request),
                maybeRenderViewApplicationsLink(activeProgram, profile),
                renderManageProgramAdminsLink(draftProgram, activeProgram))
            .withClasses(Styles.FLEX, Styles.TEXT_SM, Styles.W_FULL);

    DivTag innerDiv =
        div(topContent, midContent, programDeepLink, bottomContent)
            .withClasses(
                Styles.BORDER, Styles.BORDER_GRAY_300, Styles.BG_WHITE, Styles.ROUNDED, Styles.P_4);

    return div(innerDiv)
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

  Tag renderShareLink(ProgramDefinition program) {
    String programLink =
        baseUrl
            + controllers.applicant.routes.RedirectController.programByName(program.slug()).url();
    return makeSvgTextButton("Share link", Icons.CONTENT_COPY)
        .withClass(AdminStyles.TERTIARY_BUTTON_STYLES)
        .withData("copyable-program-link", programLink);
  }

  Tag<?> maybeRenderEditLink(
      Optional<ProgramDefinition> draftProgram,
      Optional<ProgramDefinition> activeProgram,
      Http.Request request) {
    String editLinkText = "Edit →";
    String newVersionText = "New Version";
    FormTag linkElementAsForm;

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

      linkElementAsForm =
          new LinkElement()
              .setId("program-new-version-link-" + activeProgram.get().id())
              .setHref(newVersionLink)
              .setText(newVersionText)
              .setStyles(Styles.MR_2)
              .asHiddenForm(request);
      return linkElementAsForm;
    } else {
      // obsolete or deleted, no edit link, empty div.
      return div();
    }

    ContainerTag button =
        makeSvgTextButton("Edit", Icons.EDIT)
            .withId(editLinkId)
            .withClasses(AdminStyles.TERTIARY_BUTTON_STYLES);
    return isActive
        ? toLinkButtonForPost(button, editLink, request)
        : asRedirectButton(button, editLink);
  }

  private Tag<?> maybeRenderManageTranslationsLink(Optional<ProgramDefinition> draftProgram) {
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

  private Tag<?> maybeRenderEditStatusesLink(Optional<ProgramDefinition> draftProgram) {
    if (!statusTrackingEnabled || draftProgram.isEmpty()) {
      return div();
    }
    String linkText = "Manage statuses →";
    String linkDestination =
        routes.AdminProgramTranslationsController.edit(
                program.id(), LocalizedStrings.DEFAULT_LOCALE.toLanguageTag())
            .url();
    ContainerTag button =
        makeSvgTextButton("Manage translations", Icons.LANGUAGE)
            .withId("program-translations-link-" + program.id())
            .withClass(AdminStyles.TERTIARY_BUTTON_STYLES);
    return asRedirectButton(button, linkDestination);
  }

  private Tag<?> maybeRenderViewApplicationsLink(
      Optional<ProgramDefinition> activeProgram, Optional<CiviFormProfile> userProfile) {
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

      ContainerTag button =
          makeSvgTextButton("Applications", Icons.TEXT_SNIPPET)
              .withId("program-view-apps-link-" + activeProgram.id())
              .withClass(AdminStyles.TERTIARY_BUTTON_STYLES);
      return Optional.of(asRedirectButton(button, editLink));
    }
    return Optional.empty();
  }

  private ATag renderManageProgramAdminsLink(
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
