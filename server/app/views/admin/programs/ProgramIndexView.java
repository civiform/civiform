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

import annotations.FeatureFlagOverrides;
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
import javax.inject.Provider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
  private final Provider<Boolean> statusTrackingEnabled;

  @Inject
  public ProgramIndexView(
      AdminLayoutFactory layoutFactory,
      Config config,
      DateConverter dateConverter,
      @ApplicationStatusTrackingEnabled Provider<Boolean> statusTrackingEnabled) {
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
                                    .setLabelText("Until:")
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

  private ButtonTag makePublishButton() {
    return makeSvgTextButton("Publish all drafts", Icons.PUBLISH)
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

    DivTag publishAllModalContent =
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

  private ButtonTag renderNewProgramButton() {
    String link = controllers.admin.routes.AdminProgramController.newOne().url();
    ButtonTag button =
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
                Styles.H_12,
                extraActions.size() == 0 ? Styles.INVISIBLE : "");

    return div()
        .withClasses(
            Styles.PY_7,
            Styles.FLEX,
            Styles.FLEX_ROW,
            StyleUtils.hover(Styles.BG_GRAY_100),
            StyleUtils.joinStyles(extraStyles))
        .with(
            p().withClasses(
                    badgeBGColor,
                    badgeFillColor,
                    Styles.ML_2,
                    StyleUtils.responsiveXLarge(Styles.ML_8),
                    Styles.FONT_MEDIUM,
                    Styles.ROUNDED_FULL,
                    Styles.FLEX,
                    Styles.FLEX_ROW,
                    Styles.GAP_X_2,
                    Styles.PLACE_ITEMS_CENTER,
                    Styles.JUSTIFY_CENTER)
                .withStyle("min-width:90px")
                .with(
                    Icons.svg(Icons.NOISE_CONTROL_OFF, 20)
                        .withClasses(Styles.INLINE_BLOCK, Styles.ML_3_5),
                    span(badgeText).withClass(Styles.MR_4)),
            div()
                .withClasses(Styles.ML_4, StyleUtils.responsiveXLarge(Styles.ML_10))
                .with(
                    p().with(
                            span(updatedPrefix),
                            span(formattedUpdateTime)
                                .withClasses(
                                    Styles.FONT_SEMIBOLD,
                                    Styles.HIDDEN,
                                    StyleUtils.responsiveLarge(Styles.INLINE)),
                            span(formattedUpdateDate)
                                .withClasses(
                                    Styles.FONT_SEMIBOLD,
                                    StyleUtils.responsiveLarge(Styles.HIDDEN))),
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
                                    Styles.FLEX,
                                    Styles.FLEX_COL,
                                    Styles.BORDER,
                                    Styles.BG_WHITE,
                                    Styles.ABSOLUTE,
                                    Styles.RIGHT_0,
                                    Styles.W_56,
                                    Styles.Z_50)
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
      if (statusTrackingEnabled.get()) {
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
                  draftProgram.isPresent() ? Styles.BORDER_T : ""));
    }

    DivTag titleAndStatus =
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
                statusDiv.withClasses(
                    Styles.FLEX_GROW,
                    Styles.TEXT_SM,
                    StyleUtils.responsiveLarge(Styles.TEXT_BASE)));

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

  ButtonTag renderShareLink(ProgramDefinition program) {
    String programLink =
        baseUrl
            + controllers.applicant.routes.RedirectController.programBySlug(program.slug()).url();
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
