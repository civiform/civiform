package views.applicant;

import static com.google.common.base.Preconditions.checkNotNull;

import auth.CiviFormProfile;
import com.google.common.collect.ImmutableList;
import com.google.inject.Inject;
import controllers.AssetsFinder;
import controllers.LanguageUtils;
import controllers.applicant.ApplicantRoutes;
import java.util.Locale;
import java.util.Optional;
import modules.ThymeleafModule;
import org.thymeleaf.TemplateEngine;
import play.i18n.Messages;
import play.mvc.Http;
import services.DeploymentType;
import services.MessageKey;
import services.applicant.ApplicantPersonalInfo;
import services.applicant.ApplicantService;
import services.settings.SettingsManifest;
import views.NorthStarBaseView;

/**
 * Renders the HTMX partial view for programs filtered by category on the landing page. The view has
 * two sections: one for programs that match the filters and another for programs that do not match
 * the filters.
 */
public class NorthStarFilteredProgramsViewPartial extends NorthStarBaseView {

  private final ProgramCardsSectionParamsFactory programCardsSectionParamsFactory;

  @Inject
  NorthStarFilteredProgramsViewPartial(
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
      Http.Request request,
      Optional<Long> applicantId,
      ApplicantPersonalInfo personalInfo,
      ApplicantService.ApplicationPrograms applicationPrograms,
      Optional<CiviFormProfile> profile,
      ImmutableList<String> selectedCategoriesFromParams) {
    ThymeleafModule.PlayThymeleafContext context =
        createThymeleafContext(request, applicantId, profile, personalInfo, messages);
    Locale preferredLocale = messages.lang().toLocale();

    // Find all programs that have at least one of the selected categories
    ImmutableList<ApplicantService.ApplicantProgramData> filteredPrograms =
        applicationPrograms.unapplied().stream()
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
        applicationPrograms.unapplied().stream()
            .filter(programData -> !filteredPrograms.contains(programData))
            .collect(ImmutableList.toImmutableList());

    ProgramCardsSectionParamsFactory.ProgramSectionParams recommendedSection =
        programCardsSectionParamsFactory.getSection(
            request,
            messages,
            Optional.of(MessageKey.TITLE_RECOMMENDED_PROGRAMS_SECTION_V2),
            MessageKey.BUTTON_VIEW_AND_APPLY,
            filteredPrograms,
            /* preferredLocale= */ messages.lang().toLocale(),
            profile,
            applicantId,
            personalInfo,
            ProgramCardsSectionParamsFactory.SectionType.RECOMMENDED);

    Optional<ProgramCardsSectionParamsFactory.ProgramSectionParams> otherProgramsSection =
        Optional.empty();

    if (!otherPrograms.isEmpty()) {
      otherProgramsSection =
          Optional.of(
              programCardsSectionParamsFactory.getSection(
                  request,
                  messages,
                  Optional.of(MessageKey.TITLE_OTHER_PROGRAMS_SECTION_V2),
                  MessageKey.BUTTON_VIEW_AND_APPLY,
                  otherPrograms,
                  /* preferredLocale= */ messages.lang().toLocale(),
                  profile,
                  applicantId,
                  personalInfo,
                  ProgramCardsSectionParamsFactory.SectionType.STANDARD));
    }

    context.setVariable("recommendedSection", recommendedSection);
    context.setVariable("otherProgramsSection", otherProgramsSection);
    return templateEngine.process("applicant/FilteredProgramsTemplate", context);
  }
}
