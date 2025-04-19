package views.applicant;

import static com.google.common.base.Preconditions.checkNotNull;
import static services.applicant.ApplicantPersonalInfo.ApplicantType.GUEST;

import auth.CiviFormProfile;
import com.google.common.collect.ImmutableList;
import com.google.inject.Inject;
import controllers.AssetsFinder;
import controllers.LanguageUtils;
import controllers.applicant.ApplicantRoutes;
import controllers.routes;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.stream.Stream;
import modules.ThymeleafModule;
import org.thymeleaf.TemplateEngine;
import play.i18n.Messages;
import play.mvc.Http.Request;
import services.DeploymentType;
import services.MessageKey;
import services.applicant.ApplicantPersonalInfo;
import services.applicant.ApplicantService;
import services.applicant.ApplicantService.ApplicantProgramData;
import services.settings.SettingsManifest;
import views.NorthStarBaseView;
import views.applicant.ProgramCardsSectionParamsFactory.ProgramSectionParams;

/** Renders a list of programs that an applicant can browse, with buttons for applying. */
public class NorthStarProgramIndexView extends NorthStarBaseView {
  private final ProgramCardsSectionParamsFactory programCardsSectionParamsFactory;

  @Inject
  NorthStarProgramIndexView(
      TemplateEngine templateEngine,
      ThymeleafModule.PlayThymeleafContextFactory playThymeleafContextFactory,
      AssetsFinder assetsFinder,
      ApplicantRoutes applicantRoutes,
      ProgramCardsSectionParamsFactory programCardsSectionParamsFactory,
      SettingsManifest settingsManifest,
      LanguageUtils languageUtils,
      DeploymentType deploymentType) {
    super(
        templateEngine,
        playThymeleafContextFactory,
        assetsFinder,
        applicantRoutes,
        settingsManifest,
        languageUtils,
        deploymentType);
    this.programCardsSectionParamsFactory = checkNotNull(programCardsSectionParamsFactory);
  }

  public String render(
      Messages messages,
      Request request,
      Optional<Long> applicantId,
      ApplicantPersonalInfo personalInfo,
      ApplicantService.ApplicationPrograms applicationPrograms,
      Optional<String> bannerMessage,
      Optional<CiviFormProfile> profile) {
    ThymeleafModule.PlayThymeleafContext context =
        createThymeleafContext(request, applicantId, profile, personalInfo, messages);

    context.setVariable("pageTitle", messages.at(MessageKey.CONTENT_FIND_PROGRAMS.getKeyName()));
    Optional<ProgramSectionParams> myApplicationsSection = Optional.empty();
    Optional<ProgramSectionParams> intakeSection = Optional.empty();
    Optional<ProgramSectionParams> unfilteredSection = Optional.empty();

    Locale preferredLocale = messages.lang().toLocale();

    ImmutableList<String> relevantCategories =
        applicationPrograms.unapplied().stream()
            .map(programData -> programData.program().categories())
            .flatMap(List::stream)
            .distinct()
            .map(category -> category.getLocalizedName().getOrDefault(preferredLocale))
            .sorted()
            .collect(ImmutableList.toImmutableList());

    if (isUnstartedCommonIntakeForm(applicationPrograms.commonIntakeForm())) {
      intakeSection =
          Optional.of(
              getCommonIntakeFormSection(
                  messages,
                  request,
                  applicationPrograms.commonIntakeForm().get(),
                  profile,
                  applicantId,
                  personalInfo));
    }

    if (!applicationPrograms.inProgressIncludingCommonIntake().isEmpty()
        || !applicationPrograms.submitted().isEmpty()) {
      myApplicationsSection =
          Optional.of(
              programCardsSectionParamsFactory.getSection(
                  request,
                  messages,
                  Optional.of(MessageKey.TITLE_MY_APPLICATIONS_SECTION_V2),
                  MessageKey.BUTTON_EDIT,
                  Stream.concat(
                          applicationPrograms.inProgressIncludingCommonIntake().stream(),
                          applicationPrograms.submitted().stream())
                      .collect(ImmutableList.toImmutableList()),
                  /* preferredLocale= */ messages.lang().toLocale(),
                  profile,
                  applicantId,
                  personalInfo,
                  ProgramCardsSectionParamsFactory.SectionType.MY_APPLICATIONS));
    }

    if (!applicationPrograms.unapplied().isEmpty()) {
      unfilteredSection =
          Optional.of(
              programCardsSectionParamsFactory.getSection(
                  request,
                  messages,
                  Optional.of(MessageKey.TITLE_PROGRAMS_SECTION_V2),
                  MessageKey.BUTTON_VIEW_AND_APPLY,
                  applicationPrograms.unapplied(),
                  /* preferredLocale= */ messages.lang().toLocale(),
                  profile,
                  applicantId,
                  personalInfo,
                  ProgramCardsSectionParamsFactory.SectionType.UNFILTERED_PROGRAMS));
    }

    context.setVariable("myApplicationsSection", myApplicationsSection);
    context.setVariable("commonIntakeSection", intakeSection);

    context.setVariable("unfilteredSection", unfilteredSection);
    context.setVariable(
        "authProviderName",
        // The applicant portal name should always be set (there is a
        // default setting as well).
        settingsManifest.getApplicantPortalName(request).get());
    context.setVariable("createAccountLink", routes.LoginController.register().url());
    context.setVariable("isGuest", personalInfo.getType() == GUEST);
    context.setVariable("hasProfile", profile.isPresent());
    context.setVariable("categoryOptions", relevantCategories);
    context.setVariable("applicantId", applicantId);

    // Toasts
    context.setVariable("bannerMessage", bannerMessage);

    return templateEngine.process("applicant/ProgramIndexTemplate", context);
  }

  private ProgramSectionParams getCommonIntakeFormSection(
      Messages messages,
      Request request,
      ApplicantProgramData commonIntakeForm,
      Optional<CiviFormProfile> profile,
      Optional<Long> applicantId,
      ApplicantPersonalInfo personalInfo) {

    return programCardsSectionParamsFactory.getSection(
        request,
        messages,
        Optional.of(MessageKey.TITLE_FIND_SERVICES_SECTION),
        MessageKey.BUTTON_START_SURVEY,
        ImmutableList.of(commonIntakeForm),
        /* preferredLocale= */ messages.lang().toLocale(),
        profile,
        applicantId,
        personalInfo,
        ProgramCardsSectionParamsFactory.SectionType.COMMON_INTAKE);
  }

  private boolean isUnstartedCommonIntakeForm(
      Optional<ApplicantProgramData> commonIntakeFormOptional) {
    if (commonIntakeFormOptional.isEmpty()) {
      return false;
    }
    return commonIntakeFormOptional
        .flatMap(ApplicantProgramData::latestApplicationLifecycleStage)
        .isEmpty();
  }
}
