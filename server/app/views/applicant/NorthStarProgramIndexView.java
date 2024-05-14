package views.applicant;

import static com.google.common.base.Preconditions.checkNotNull;
import static services.applicant.ApplicantPersonalInfo.ApplicantType.GUEST;

import annotations.BindingAnnotations;
import auth.CiviFormProfile;
import com.google.common.collect.ImmutableList;
import com.google.inject.Inject;
import controllers.AssetsFinder;
import controllers.LanguageUtils;
import controllers.applicant.ApplicantRoutes;
import controllers.routes;
import java.util.Optional;
import models.LifecycleStage;
import modules.ThymeleafModule;
import org.thymeleaf.TemplateEngine;
import play.i18n.Messages;
import play.mvc.Http.Request;
import services.MessageKey;
import services.applicant.ApplicantPersonalInfo;
import services.applicant.ApplicantService;
import services.applicant.ApplicantService.ApplicantProgramData;
import services.settings.SettingsManifest;
import views.applicant.ProgramCardsSectionParamsFactory.ProgramSectionParams;

/** Renders a list of programs that an applicant can browse, with buttons for applying. */
public class NorthStarProgramIndexView extends NorthStarApplicantBaseView {
  private final ProgramCardsSectionParamsFactory programCardsSectionParamsFactory;
  private final String authProviderName;

  @Inject
  NorthStarProgramIndexView(
      TemplateEngine templateEngine,
      ThymeleafModule.PlayThymeleafContextFactory playThymeleafContextFactory,
      AssetsFinder assetsFinder,
      ApplicantRoutes applicantRoutes,
      ProgramCardsSectionParamsFactory programCardsSectionParamsFactory,
      SettingsManifest settingsManifest,
      @BindingAnnotations.ApplicantAuthProviderName String authProviderName,
      LanguageUtils languageUtils) {
    super(
        templateEngine,
        playThymeleafContextFactory,
        assetsFinder,
        applicantRoutes,
        settingsManifest,
        languageUtils);
    this.programCardsSectionParamsFactory = checkNotNull(programCardsSectionParamsFactory);
    this.authProviderName = checkNotNull(authProviderName);
  }

  public String render(
      Messages messages,
      Request request,
      long applicantId,
      ApplicantPersonalInfo personalInfo,
      ApplicantService.ApplicationPrograms applicationPrograms,
      CiviFormProfile profile) {
    ThymeleafModule.PlayThymeleafContext context = createThymeleafContext(request);

    ImmutableList.Builder<ProgramSectionParams> sectionParamsBuilder = ImmutableList.builder();

    Optional<ProgramSectionParams> intakeSection = Optional.empty();

    if (settingsManifest.getIntakeFormEnabled(request)
        && applicationPrograms.commonIntakeForm().isPresent()) {
      intakeSection =
          Optional.of(
              getCommonIntakeFormSection(
                  messages,
                  request,
                  applicationPrograms.commonIntakeForm().get(),
                  profile,
                  applicantId));
    }

    if (!applicationPrograms.inProgress().isEmpty()) {
      sectionParamsBuilder.add(
          programCardsSectionParamsFactory.getSection(
              request,
              messages,
              Optional.of(MessageKey.TITLE_PROGRAMS_IN_PROGRESS_UPDATED),
              MessageKey.BUTTON_CONTINUE,
              applicationPrograms.inProgress(),
              /* preferredLocale= */ messages.lang().toLocale(),
              profile,
              applicantId));
    }

    if (!applicationPrograms.submitted().isEmpty()) {
      sectionParamsBuilder.add(
          programCardsSectionParamsFactory.getSection(
              request,
              messages,
              Optional.of(MessageKey.TITLE_PROGRAMS_SUBMITTED),
              MessageKey.BUTTON_EDIT,
              applicationPrograms.submitted(),
              /* preferredLocale= */ messages.lang().toLocale(),
              profile,
              applicantId));
    }

    if (!applicationPrograms.unapplied().isEmpty()) {
      sectionParamsBuilder.add(
          programCardsSectionParamsFactory.getSection(
              request,
              messages,
              Optional.of(MessageKey.TITLE_PROGRAMS_ACTIVE_UPDATED),
              MessageKey.BUTTON_APPLY,
              applicationPrograms.unapplied(),
              /* preferredLocale= */ messages.lang().toLocale(),
              profile,
              applicantId));
    }

    context.setVariable("commonIntakeSection", intakeSection);
    context.setVariable(
        "numPrograms",
        applicationPrograms.inProgress().size()
            + applicationPrograms.submitted().size()
            + applicationPrograms.unapplied().size());
    context.setVariable(
        "civicEntityShortName", settingsManifest.getWhitelabelCivicEntityShortName(request).get());
    context.setVariable("sections", sectionParamsBuilder.build());
    context.setVariable("authProviderName", authProviderName);
    context.setVariable("createAccountLink", routes.LoginController.register().url());
    context.setVariable("isGuest", personalInfo.getType() == GUEST);

    return templateEngine.process("applicant/ProgramIndexTemplate", context);
  }

  private ProgramSectionParams getCommonIntakeFormSection(
      Messages messages,
      Request request,
      ApplicantProgramData commonIntakeForm,
      CiviFormProfile profile,
      long applicantId) {
    Optional<LifecycleStage> commonIntakeFormApplicationStatus =
        commonIntakeForm.latestApplicationLifecycleStage();

    MessageKey buttonText = MessageKey.BUTTON_START_HERE;
    if (commonIntakeFormApplicationStatus.isPresent()) {
      switch (commonIntakeFormApplicationStatus.get()) {
        case ACTIVE:
          buttonText = MessageKey.BUTTON_EDIT;
          break;
        case DRAFT:
          buttonText = MessageKey.BUTTON_CONTINUE;
          break;
        default:
          // Leave button text as is.
      }
    }

    return programCardsSectionParamsFactory.getSection(
        request,
        messages,
        Optional.empty(),
        buttonText,
        ImmutableList.of(commonIntakeForm),
        /* preferredLocale= */ messages.lang().toLocale(),
        profile,
        applicantId);
  }
}
