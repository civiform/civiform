package views.applicant;

import static com.google.common.base.Preconditions.checkNotNull;
import static j2html.TagCreator.div;
import static j2html.TagCreator.h1;
import static j2html.TagCreator.h2;
import static services.applicant.ApplicantPersonalInfo.ApplicantType.GUEST;

import annotations.BindingAnnotations;
import auth.CiviFormProfile;
import com.google.common.collect.ImmutableList;
import controllers.routes;
import j2html.tags.specialized.DivTag;
import j2html.tags.specialized.H1Tag;
import j2html.tags.specialized.H2Tag;
import java.util.Locale;
import java.util.Optional;
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
  private final String authProviderName;
  private final ProgramCardViewRenderer programCardViewRenderer;

  @Inject
  public ProgramIndexView(
      ApplicantLayout layout,
      ProgramCardViewRenderer programCardViewRenderer,
      SettingsManifest settingsManifest,
      @BindingAnnotations.ApplicantAuthProviderName String authProviderName) {
    this.layout = checkNotNull(layout);
    this.programCardViewRenderer = checkNotNull(programCardViewRenderer);
    this.settingsManifest = checkNotNull(settingsManifest);
    this.authProviderName = checkNotNull(authProviderName);
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
      long applicantId,
      ApplicantPersonalInfo personalInfo,
      ApplicantService.ApplicationPrograms applicationPrograms,
      Optional<ToastMessage> bannerMessage,
      CiviFormProfile profile) {
    HtmlBundle bundle = layout.getBundle(request);
    bundle.setTitle(messages.at(MessageKey.CONTENT_FIND_PROGRAMS.getKeyName()));
    bannerMessage.ifPresent(bundle::addToastMessages);

    String sessionEndedMessage = messages.at(MessageKey.TOAST_SESSION_ENDED.getKeyName());
    bundle.addToastMessages(
        ToastMessage.success(sessionEndedMessage)
            .setCondOnStorageKey("session_just_ended")
            .setDuration(5000));

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

    return layout.renderWithNav(
        request, personalInfo, messages, bundle, /* includeAdminLogin= */ true, applicantId);
  }

  private DivTag topContent(
      Http.Request request, Messages messages, ApplicantPersonalInfo personalInfo) {

    String h1Text, infoDivText, widthClass;

    if (personalInfo.getType() == GUEST) {
      // "Save time finding and applying for programs and services"
      h1Text = messages.at(MessageKey.CONTENT_SAVE_TIME.getKeyName());
      infoDivText =
          messages.at(MessageKey.CONTENT_GUEST_DESCRIPTION.getKeyName(), authProviderName);
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
      long applicantId,
      Locale preferredLocale,
      HtmlBundle bundle,
      CiviFormProfile profile) {
    DivTag content =
        div()
            .withId("main-content")
            .withClasses("mx-auto", "my-4", StyleUtils.responsiveSmall("m-10"));

    // The different program card containers should have the same styling, by using the program
    // count of the larger set of programs
    String cardContainerStyles =
        programCardViewRenderer.programCardsContainerStyles(
            ProgramCardViewRenderer.ContainerWidth.FULL,
            Math.max(
                Math.max(relevantPrograms.unapplied().size(), relevantPrograms.submitted().size()),
                relevantPrograms.inProgress().size()));

    if (settingsManifest.getIntakeFormEnabled(request)
        && relevantPrograms.commonIntakeForm().isPresent()) {
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
              profile));
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
              profile));
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
              profile));
    }

    return div().withClasses("flex", "flex-col", "place-items-center").with(content);
  }

  private DivTag findServicesSection(
      Http.Request request,
      Messages messages,
      ApplicantPersonalInfo personalInfo,
      ApplicantService.ApplicationPrograms relevantPrograms,
      String cardContainerStyles,
      long applicantId,
      Locale preferredLocale,
      HtmlBundle bundle,
      CiviFormProfile profile) {
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
    return div()
        .withClass(ReferenceClasses.APPLICATION_PROGRAM_SECTION)
        .with(programSectionTitle(messages.at(MessageKey.TITLE_FIND_SERVICES_SECTION.getKeyName())))
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
                profile));
  }
}
