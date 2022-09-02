package views.admin.programs;

import static com.google.common.base.Preconditions.checkNotNull;
import static j2html.TagCreator.div;
import static j2html.TagCreator.each;
import static j2html.TagCreator.fieldset;
import static j2html.TagCreator.form;
import static j2html.TagCreator.h1;
import static j2html.TagCreator.legend;
import static j2html.TagCreator.li;
import static j2html.TagCreator.p;
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
import views.components.Modal;
import views.components.ProgramCardFactory;
import views.components.ToastMessage;
import views.style.AdminStyles;
import views.style.ReferenceClasses;
import views.style.Styles;

/** Renders a page so the admin can view all active programs and draft programs. */
public final class ProgramIndexView extends BaseHtmlView {
  private final AdminLayout layout;
  private final String baseUrl;
  private final TranslationLocales translationLocales;
  private final ProgramCardFactory programCardFactory;
  private final FeatureFlags featureFlags;

  @Inject
  public ProgramIndexView(
      AdminLayoutFactory layoutFactory,
      Config config,
      TranslationLocales translationLocales,
      ProgramCardFactory programCardFactory,
      FeatureFlags featureFlags) {
    this.layout = checkNotNull(layoutFactory).getLayout(NavPage.PROGRAMS);
    this.baseUrl = checkNotNull(config).getString("base_url");
    this.translationLocales = checkNotNull(translationLocales);
    this.programCardFactory = checkNotNull(programCardFactory);
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

    String pageTitle = "All programs";
    Optional<Modal> maybePublishModal = maybeRenderPublishModal(programs, questions, request);

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
            .withClasses(Styles.P_6, Styles.FLEX_ROW, Styles.SPACE_Y_6)
            .with(
                p("Please be aware that due to the nature of shared questions and versioning,"
                        + " all questions and programs will need to be published together.")
                    .withClass(Styles.TEXT_SM),
                div()
                    .withClasses(ReferenceClasses.ADMIN_PUBLISH_REFERENCES_QUESTION)
                    .with(
                        p(String.format("Edited questions (%d):", sortedDraftQuestions.size()))
                            .withClass(Styles.FONT_SEMIBOLD))
                    .condWith(sortedDraftQuestions.isEmpty(), p("None").withClass(Styles.PL_5))
                    .condWith(
                        !sortedDraftQuestions.isEmpty(),
                        ul().withClasses(Styles.LIST_DISC, Styles.LIST_INSIDE)
                            .with(
                                each(
                                    sortedDraftQuestions,
                                    draftQuestion -> li(draftQuestion.getName())))),
                div()
                    .withClasses(ReferenceClasses.ADMIN_PUBLISH_REFERENCES_PROGRAM)
                    .with(
                        p(String.format("Edited programs (%d):", sortedDraftPrograms.size()))
                            .withClass(Styles.FONT_SEMIBOLD))
                    .condWith(sortedDraftPrograms.isEmpty(), p("None").withClass(Styles.PL_5))
                    .condWith(
                        !sortedDraftPrograms.isEmpty(),
                        ul().withClasses(Styles.LIST_DISC, Styles.LIST_INSIDE)
                            .with(
                                each(
                                    sortedDraftPrograms,
                                    draftProgram -> li(draftProgram.adminName())))),
                p("Would you like to publish all edited questions and programs now?"),
                div()
                    .withClasses(Styles.FLEX, Styles.FLEX_ROW)
                    .with(
                        div().withClass(Styles.FLEX_GROW),
                        button("Cancel")
                            .withClasses(
                                ReferenceClasses.MODAL_CLOSE, AdminStyles.TERTIARY_BUTTON_STYLES),
                        toLinkButtonForPost(
                            submitButton("Confirm").withClasses(AdminStyles.TERTIARY_BUTTON_STYLES),
                            link,
                            request)));
    ButtonTag publishAllButton =
        makeSvgTextButton("Publish all drafts", Icons.PUBLISH)
            .withClasses(AdminStyles.PRIMARY_BUTTON_STYLES, Styles.MY_2);
    Modal publishAllModal =
        Modal.builder("publish-all-programs-modal", publishAllModalContent)
            .setModalTitle("All program and question drafts will be published")
            .setTriggerButtonContent(publishAllButton)
            .build();
    return Optional.of(publishAllModal);
  }

  private ButtonTag renderNewProgramButton() {
    String link = controllers.admin.routes.AdminProgramController.newOne().url();
    ButtonTag button =
        makeSvgTextButton("Create new program", Icons.ADD)
            .withId("new-program-button")
            .withClasses(AdminStyles.SECONDARY_BUTTON_STYLES, Styles.MY_2);
    return asRedirectElement(button, link);
  }

  private DivTag renderProgramListItem(
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
      if (featureFlags.isStatusTrackingEnabled(request)) {
        draftRowExtraActions.add(renderEditStatusesLink(draftProgram.get()));
      }
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
          maybeRenderViewApplicationsLink(activeProgram.get(), profile);
      applicationsLink.ifPresent(activeRowExtraActions::add);
      if (!draftProgram.isPresent()) {
        activeRowActions.add(renderEditLink(/* isActive = */ true, activeProgram.get(), request));
        activeRowExtraActions.add(renderManageProgramAdminsLink(activeProgram.get()));
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

    return programCardFactory.renderCard(
        ProgramCardFactory.ProgramCardData.builder()
            .setActiveProgram(activeRow)
            .setDraftProgram(draftRow)
            .build());
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
        : asRedirectElement(button, editLink);
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
