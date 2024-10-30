package views.applicant;

import static com.google.common.base.Preconditions.checkNotNull;
import static j2html.TagCreator.div;
import static j2html.TagCreator.each;
import static j2html.TagCreator.fieldset;
import static j2html.TagCreator.form;
import static j2html.TagCreator.h1;
import static j2html.TagCreator.h2;
import static j2html.TagCreator.input;
import static j2html.TagCreator.label;
import static j2html.TagCreator.legend;
import static services.applicant.ApplicantPersonalInfo.ApplicantType.GUEST;

import auth.CiviFormProfile;
import com.google.common.collect.ImmutableList;
import controllers.applicant.ApplicantRoutes;
import controllers.routes;
import j2html.tags.specialized.DivTag;
import j2html.tags.specialized.FormTag;
import j2html.tags.specialized.H1Tag;
import j2html.tags.specialized.H2Tag;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.stream.Stream;
import javax.inject.Inject;
import models.LifecycleStage;
import play.i18n.Messages;
import play.mvc.Http;
import play.twirl.api.Content;
import services.MessageKey;
import services.applicant.ApplicantPersonalInfo;
import services.applicant.ApplicantService;
import services.settings.SettingsManifest;
import views.BaseHtmlView;
import views.HtmlBundle;
import views.components.ButtonStyles;
import views.components.ToastMessage;
import views.style.ApplicantStyles;
import views.style.ReferenceClasses;
import views.style.StyleUtils;

/** Returns a list of programs that an applicant can browse, with buttons for applying. */
public final class ProgramIndexView extends BaseHtmlView {

  private final ApplicantLayout layout;
  private final SettingsManifest settingsManifest;
  private final ProgramCardViewRenderer programCardViewRenderer;
  private final ApplicantRoutes applicantRoutes;

  @Inject
  public ProgramIndexView(
      ApplicantLayout layout,
      ProgramCardViewRenderer programCardViewRenderer,
      SettingsManifest settingsManifest,
      ApplicantRoutes applicantRoutes) {
    this.layout = checkNotNull(layout);
    this.programCardViewRenderer = checkNotNull(programCardViewRenderer);
    this.settingsManifest = checkNotNull(settingsManifest);
    this.applicantRoutes = checkNotNull(applicantRoutes);
  }

  /**
   * For each program in the list, render the program information along with an "Apply" button that
   * redirects the user to that program's application.
   *
   * @param messages the localized {@link Messages} for the current applicant
   * @param applicantId the ID of the current applicant
   * @param applicationPrograms an {@link ImmutableList} of programs (with attached application)
   *     information that should be displayed in the list
   * @return HTML content for rendering the list of available programs
   */
  public Content render(
      Messages messages,
      Http.Request request,
      Optional<Long> applicantId,
      ApplicantPersonalInfo personalInfo,
      ApplicantService.ApplicationPrograms applicationPrograms,
      ImmutableList<String> selectedCategoriesFromParams,
      Optional<ToastMessage> bannerMessage,
      Optional<CiviFormProfile> profile) {
    HtmlBundle bundle = layout.getBundle(request);
    bundle.setTitle(messages.at(MessageKey.CONTENT_FIND_PROGRAMS.getKeyName()));
    bannerMessage.ifPresent(bundle::addToastMessages);

    String sessionEndedMessage = messages.at(MessageKey.TOAST_SESSION_ENDED.getKeyName());
    bundle.addToastMessages(
        ToastMessage.success(sessionEndedMessage)
            .setCondOnStorageKey("session_just_ended")
            .setDuration(5000));

    // TODO(#7610): When the program filtering flag is removed, we can remove this conditional
    // statement.
    if (settingsManifest.getProgramFilteringEnabled(request)) {
      bundle.addMainContent(
          topContent(request, messages, personalInfo),
          mainContentWithProgramFiltersEnabled(
              request,
              messages,
              personalInfo,
              applicationPrograms,
              selectedCategoriesFromParams,
              applicantId,
              messages.lang().toLocale(),
              bundle,
              profile));
    } else {
      bundle.addMainContent(
          topContent(request, messages, personalInfo),
          mainContent(
              request,
              messages,
              personalInfo,
              applicationPrograms,
              applicantId,
              messages.lang().toLocale(),
              bundle,
              profile));
    }

    return layout.renderWithNav(
        request, personalInfo, messages, bundle, /* includeAdminLogin= */ true, applicantId);
  }

  public Content renderWithoutApplicant(
      Messages messages,
      Http.Request request,
      ApplicantService.ApplicationPrograms applicationPrograms,
      ImmutableList<String> selectedCategoriesFromParams) {
    return render(
        messages,
        request,
        /* applicantId= */ Optional.empty(),
        ApplicantPersonalInfo.ofGuestUser(),
        applicationPrograms,
        selectedCategoriesFromParams,
        /* bannerMessage= */ Optional.empty(),
        /* profile= */ Optional.empty());
  }

  private DivTag topContent(
      Http.Request request, Messages messages, ApplicantPersonalInfo personalInfo) {

    String h1Text, infoDivText, widthClass;

    if (personalInfo.getType() == GUEST) {
      // "Save time finding and applying for programs and services"
      h1Text = messages.at(MessageKey.CONTENT_SAVE_TIME.getKeyName());
      infoDivText =
          messages.at(
              MessageKey.CONTENT_GUEST_DESCRIPTION.getKeyName(),
              // The applicant portal name should always be set (there is a
              // default setting as well).
              settingsManifest.getApplicantPortalName(request).get());
      widthClass = "w-8/12";
    } else { // Logged in.
      // "Find programs"
      h1Text = messages.at(MessageKey.CONTENT_FIND_PROGRAMS.getKeyName());
      infoDivText =
          messages.at(
              MessageKey.CONTENT_CIVIFORM_DESCRIPTION.getKeyName(),
              settingsManifest.getWhitelabelCivicEntityShortName(request).get());
      widthClass = "w-5/12";
    }

    H1Tag programIndexH1 =
        h1().withText(h1Text)
            .withClasses(
                "text-4xl",
                StyleUtils.responsiveSmall("text-5xl"),
                "font-semibold",
                "mt-10",
                "px-6",
                StyleUtils.responsiveSmall("mb-6"));

    DivTag infoDiv =
        div()
            .withText(infoDivText)
            .withClasses(
                "text-sm", "px-6", widthClass, "pb-6", StyleUtils.responsiveSmall("text-base"));

    return div()
        .withId("top-content")
        .withClasses(
            ApplicantStyles.PROGRAM_INDEX_TOP_CONTENT,
            "relative",
            "flex",
            "flex-col",
            "items-center")
        .with(programIndexH1, infoDiv)
        .condWith(
            personalInfo.getType() == GUEST,
            // Log in and Create account buttons if user is a guest.
            div()
                .with(
                    redirectButton(
                            "login-button",
                            messages.at(MessageKey.BUTTON_LOGIN.getKeyName()),
                            routes.LoginController.applicantLogin(Optional.empty()).url())
                        .withClasses(ButtonStyles.SOLID_WHITE, "basis-60"))
                .with(
                    redirectButton(
                            "create-account",
                            messages.at(MessageKey.BUTTON_CREATE_ACCOUNT.getKeyName()),
                            routes.LoginController.register().url())
                        .withClasses(ButtonStyles.SOLID_WHITE, "basis-60"))
                .withClasses(
                    "flex",
                    "flex-row",
                    "gap-x-8",
                    "pb-6",
                    "px-4",
                    "w-screen",
                    "place-content-center"));
  }

  private H2Tag programSectionTitle(String title) {
    return h2().withText(title).withClasses("mb-4", "px-4", "text-xl", "font-semibold");
  }

  private DivTag mainContent(
      Http.Request request,
      Messages messages,
      ApplicantPersonalInfo personalInfo,
      ApplicantService.ApplicationPrograms relevantPrograms,
      Optional<Long> applicantId,
      Locale preferredLocale,
      HtmlBundle bundle,
      Optional<CiviFormProfile> profile) {
    DivTag content =
        div().withId("main-content").withClasses(ApplicantStyles.PROGRAM_CARDS_PARENT_CONTAINER);

    // The different program card containers should have the same styling, by using the program
    // count of the larger set of programs
    String cardContainerStyles =
        programCardViewRenderer.programCardsContainerStyles(
            Math.max(
                Math.max(relevantPrograms.unapplied().size(), relevantPrograms.submitted().size()),
                relevantPrograms.inProgress().size()));

    if (relevantPrograms.commonIntakeForm().isPresent()) {
      content.with(
          findServicesSection(
              request,
              messages,
              personalInfo,
              relevantPrograms,
              cardContainerStyles,
              applicantId,
              preferredLocale,
              bundle,
              profile),
          div().withClass("mb-12"),
          programSectionTitle(
              messages.at(
                  MessageKey.TITLE_ALL_PROGRAMS_SECTION.getKeyName(),
                  relevantPrograms.inProgress().size()
                      + relevantPrograms.submitted().size()
                      + relevantPrograms.unapplied().size())));
    } else {
      content.with(programSectionTitle(messages.at(MessageKey.TITLE_PROGRAMS.getKeyName())));
    }

    if (!relevantPrograms.inProgress().isEmpty()) {
      content.with(
          programCardViewRenderer.programCardsSection(
              request,
              messages,
              personalInfo,
              Optional.of(MessageKey.TITLE_PROGRAMS_IN_PROGRESS_UPDATED),
              cardContainerStyles,
              applicantId,
              preferredLocale,
              relevantPrograms.inProgress(),
              MessageKey.BUTTON_CONTINUE,
              MessageKey.BUTTON_CONTINUE_SR,
              bundle,
              profile,
              /* isMyApplicationsSection= */ false));
    }
    if (!relevantPrograms.submitted().isEmpty()) {
      content.with(
          programCardViewRenderer.programCardsSection(
              request,
              messages,
              personalInfo,
              Optional.of(MessageKey.TITLE_PROGRAMS_SUBMITTED),
              cardContainerStyles,
              applicantId,
              preferredLocale,
              relevantPrograms.submitted(),
              MessageKey.BUTTON_EDIT,
              MessageKey.BUTTON_EDIT_SR,
              bundle,
              profile,
              /* isMyApplicationsSection= */ false));
    }
    if (!relevantPrograms.unapplied().isEmpty()) {
      content.with(
          programCardViewRenderer.programCardsSection(
              request,
              messages,
              personalInfo,
              Optional.of(MessageKey.TITLE_PROGRAMS_ACTIVE_UPDATED),
              cardContainerStyles,
              applicantId,
              preferredLocale,
              relevantPrograms.unapplied(),
              MessageKey.BUTTON_APPLY,
              MessageKey.BUTTON_APPLY_SR,
              bundle,
              profile,
              /* isMyApplicationsSection= */ false));
    }

    return div().withClasses(ApplicantStyles.PROGRAM_CARDS_GRANDPARENT_CONTAINER).with(content);
  }

  private DivTag mainContentWithProgramFiltersEnabled(
      Http.Request request,
      Messages messages,
      ApplicantPersonalInfo personalInfo,
      ApplicantService.ApplicationPrograms relevantPrograms,
      ImmutableList<String> selectedCategoriesFromParams,
      Optional<Long> applicantId,
      Locale preferredLocale,
      HtmlBundle bundle,
      Optional<CiviFormProfile> profile) {
    DivTag content =
        div().withId("main-content").withClasses(ApplicantStyles.PROGRAM_CARDS_PARENT_CONTAINER);

    // Find all the categories that are on any of the relevant programs to which the resident hasn't
    // applied
    ImmutableList<String> relevantCategories =
        relevantPrograms.unapplied().stream()
            .map(programData -> programData.program().categories())
            .flatMap(List::stream)
            .distinct()
            .map(category -> category.getLocalizedName().getOrDefault(preferredLocale))
            .sorted()
            .collect(ImmutableList.toImmutableList());

    // Find all programs that have at least one of the selected categories
    ImmutableList<ApplicantService.ApplicantProgramData> filteredPrograms =
        relevantPrograms.unapplied().stream()
            .filter(
                program ->
                    program.program().categories().stream()
                        .anyMatch(
                            category ->
                                selectedCategoriesFromParams.contains(
                                    category.getLocalizedName().getOrDefault(preferredLocale))))
            .collect(ImmutableList.toImmutableList());

    // Find all programs that don't have any of the selected categories
    ImmutableList<ApplicantService.ApplicantProgramData> otherPrograms =
        relevantPrograms.unapplied().stream()
            .filter(programData -> !filteredPrograms.contains(programData))
            .collect(ImmutableList.toImmutableList());

    // The different program card containers should have the same styling, by using the program
    // count of the larger set of programs
    String cardContainerStyles =
        programCardViewRenderer.programCardsContainerStyles(
            Arrays.asList(
                    relevantPrograms.unapplied().size(),
                    relevantPrograms.inProgress().size() + relevantPrograms.submitted().size(),
                    filteredPrograms.size(),
                    otherPrograms.size())
                .stream()
                .max(Integer::compare)
                .get());

    // My applications section
    if (!relevantPrograms.inProgress().isEmpty() || !relevantPrograms.submitted().isEmpty()) {
      content.with(
          programCardViewRenderer.programCardsSection(
              request,
              messages,
              personalInfo,
              Optional.of(MessageKey.TITLE_MY_APPLICATIONS_SECTION),
              cardContainerStyles,
              applicantId,
              preferredLocale,
              Stream.concat(
                      relevantPrograms.inProgress().stream(), relevantPrograms.submitted().stream())
                  .collect(ImmutableList.toImmutableList()),
              MessageKey.BUTTON_EDIT,
              MessageKey.BUTTON_EDIT_SR,
              bundle,
              profile,
              /* isMyApplicationsSection= */ true));
      content.with(div().withClasses("mb-10"));
    }

    // The category buttons
    if (settingsManifest.getProgramFilteringEnabled(request) && !relevantCategories.isEmpty()) {
      content.with(
          renderCategoryFilterChips(
              profile, applicantId, relevantCategories, selectedCategoriesFromParams, messages));
    }

    if (selectedCategoriesFromParams.isEmpty()) {
      buildSectionsWithNoFiltersSelected(
          request,
          messages,
          personalInfo,
          relevantPrograms,
          applicantId,
          preferredLocale,
          bundle,
          profile,
          content,
          cardContainerStyles);

    } else {
      buildSectionsWithFiltersApplied(
          request,
          messages,
          personalInfo,
          applicantId,
          preferredLocale,
          bundle,
          profile,
          content,
          cardContainerStyles,
          filteredPrograms,
          otherPrograms);
    }

    return div().withClasses(ApplicantStyles.PROGRAM_CARDS_GRANDPARENT_CONTAINER).with(content);
  }

  private void buildSectionsWithFiltersApplied(
      Http.Request request,
      Messages messages,
      ApplicantPersonalInfo personalInfo,
      Optional<Long> applicantId,
      Locale preferredLocale,
      HtmlBundle bundle,
      Optional<CiviFormProfile> profile,
      DivTag content,
      String cardContainerStyles,
      ImmutableList<ApplicantService.ApplicantProgramData> filteredPrograms,
      ImmutableList<ApplicantService.ApplicantProgramData> otherPrograms) {
    // Recommended section
    content.with(
        programCardViewRenderer
            .programCardsSection(
                request,
                messages,
                personalInfo,
                Optional.of(MessageKey.TITLE_RECOMMENDED_PROGRAMS_SECTION_V2),
                cardContainerStyles,
                applicantId,
                preferredLocale,
                filteredPrograms,
                MessageKey.BUTTON_APPLY,
                MessageKey.BUTTON_APPLY_SR,
                bundle,
                profile,
                /* isMyApplicationsSection= */ false)
            .withId("recommended-programs"));

    if (!otherPrograms.isEmpty()) {
      content.with(div().withClasses("mt-10"));
      content.with(
          programCardViewRenderer.programCardsSection(
              request,
              messages,
              personalInfo,
              Optional.of(MessageKey.TITLE_OTHER_PROGRAMS_SECTION_V2),
              cardContainerStyles,
              applicantId,
              preferredLocale,
              otherPrograms,
              MessageKey.BUTTON_APPLY,
              MessageKey.BUTTON_APPLY_SR,
              bundle,
              profile,
              /* isMyApplicationsSection= */ false));
    }
  }

  private void buildSectionsWithNoFiltersSelected(
      Http.Request request,
      Messages messages,
      ApplicantPersonalInfo personalInfo,
      ApplicantService.ApplicationPrograms relevantPrograms,
      Optional<Long> applicantId,
      Locale preferredLocale,
      HtmlBundle bundle,
      Optional<CiviFormProfile> profile,
      DivTag content,
      String cardContainerStyles) {
    // Intake form
    if (relevantPrograms.commonIntakeForm().isPresent()) {
      content.with(
          findServicesSection(
              request,
              messages,
              personalInfo,
              relevantPrograms,
              cardContainerStyles,
              applicantId,
              preferredLocale,
              bundle,
              profile),
          div().withClass("mb-12"));
    }

    if (!relevantPrograms.unapplied().isEmpty()) {
      content.with(
          programCardViewRenderer.programCardsSection(
              request,
              messages,
              personalInfo,
              Optional.of(MessageKey.TITLE_PROGRAMS_SECTION_V2),
              cardContainerStyles,
              applicantId,
              preferredLocale,
              relevantPrograms.unapplied(),
              MessageKey.BUTTON_APPLY,
              MessageKey.BUTTON_APPLY_SR,
              bundle,
              profile,
              /* isMyApplicationsSection= */ false));
    }
  }

  private DivTag findServicesSection(
      Http.Request request,
      Messages messages,
      ApplicantPersonalInfo personalInfo,
      ApplicantService.ApplicationPrograms relevantPrograms,
      String cardContainerStyles,
      Optional<Long> applicantId,
      Locale preferredLocale,
      HtmlBundle bundle,
      Optional<CiviFormProfile> profile) {
    Optional<LifecycleStage> commonIntakeFormApplicationStatus =
        relevantPrograms.commonIntakeForm().get().latestApplicationLifecycleStage();
    MessageKey buttonText = MessageKey.BUTTON_START_HERE;
    MessageKey buttonScreenReaderText = MessageKey.BUTTON_START_HERE_COMMON_INTAKE_SR;
    if (commonIntakeFormApplicationStatus.isPresent()) {
      switch (commonIntakeFormApplicationStatus.get()) {
        case ACTIVE:
          buttonText = MessageKey.BUTTON_EDIT;
          buttonScreenReaderText = MessageKey.BUTTON_EDIT_COMMON_INTAKE_SR;
          break;
        case DRAFT:
          buttonText = MessageKey.BUTTON_CONTINUE;
          buttonScreenReaderText = MessageKey.BUTTON_CONTINUE_COMMON_INTAKE_SR;
          break;
        default:
          // Leave button text as is.
      }
    }

    String titleMessage =
        settingsManifest.getProgramFilteringEnabled(request)
            ? messages.at(MessageKey.TITLE_BENEFITS_FINDER_SECTION_V2.getKeyName())
            : messages.at(MessageKey.TITLE_FIND_SERVICES_SECTION.getKeyName());

    return div()
        .withClass(ReferenceClasses.APPLICATION_PROGRAM_SECTION)
        .with(programSectionTitle(titleMessage))
        .with(
            programCardViewRenderer.programCardsSection(
                request,
                messages,
                personalInfo,
                Optional.empty(),
                cardContainerStyles,
                applicantId,
                preferredLocale,
                ImmutableList.of(relevantPrograms.commonIntakeForm().get()),
                buttonText,
                buttonScreenReaderText,
                bundle,
                profile,
                /* isMyApplicationsSection= */ false));
  }

  private FormTag renderCategoryFilterChips(
      Optional<CiviFormProfile> profile,
      Optional<Long> applicantId,
      ImmutableList<String> relevantCategories,
      ImmutableList<String> selectedCategoriesFromParams,
      Messages messages) {
    return form()
        .withId("category-filter-form")
        .withAction(
            applicantId.isPresent() && profile.isPresent()
                ? applicantRoutes.index(profile.get(), applicantId.get()).url()
                : controllers.applicant.routes.ApplicantProgramsController.indexWithoutApplicantId(
                        ImmutableList.of())
                    .url())
        .withMethod("GET")
        .with(
            fieldset(
                    legend(messages.at(MessageKey.LABEL_PROGRAM_FILTERS.getKeyName()))
                        .withClasses("mb-2"),
                    each(
                        relevantCategories,
                        category ->
                            div()
                                .withClass("filter-chip")
                                .with(
                                    input()
                                        .withId("check-category-" + category.replace(' ', '-'))
                                        .withType("checkbox")
                                        .withName("categories")
                                        .withValue(category)
                                        .withCondChecked(
                                            selectedCategoriesFromParams.contains(category))
                                        .withClasses("sr-only"),
                                    label(category)
                                        .withClasses("px-4", "py-2")
                                        .withFor("check-category-" + category.replace(' ', '-')))))
                .withClasses("flex", "mb-10", "flex-wrap", "ml-4"));
  }
}
