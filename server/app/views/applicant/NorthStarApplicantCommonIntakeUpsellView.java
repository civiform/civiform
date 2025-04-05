package views.applicant;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.inject.Inject;
import controllers.AssetsFinder;
import controllers.LanguageUtils;
import controllers.applicant.ApplicantRoutes;
import java.util.Optional;
import modules.ThymeleafModule;
import org.thymeleaf.TemplateEngine;
import services.DeploymentType;
import services.MessageKey;
import services.settings.SettingsManifest;
import views.NorthStarBaseView;
import views.applicant.ProgramCardsSectionParamsFactory.ProgramSectionParams;

public class NorthStarApplicantCommonIntakeUpsellView extends NorthStarBaseView {
  private final ProgramCardsSectionParamsFactory programCardsSectionParamsFactory;

  @Inject
  NorthStarApplicantCommonIntakeUpsellView(
      TemplateEngine templateEngine,
      ThymeleafModule.PlayThymeleafContextFactory playThymeleafContextFactory,
      AssetsFinder assetsFinder,
      ApplicantRoutes applicantRoutes,
      SettingsManifest settingsManifest,
      LanguageUtils languageUtils,
      DeploymentType deploymentType,
      ProgramCardsSectionParamsFactory programCardsSectionParamsFactory) {
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

  public String render(UpsellParams params) {
    ThymeleafModule.PlayThymeleafContext context =
        createThymeleafContext(
            params.request(),
            Optional.of(params.applicantId()),
            Optional.of(params.profile()),
            params.applicantPersonalInfo(),
            params.messages());

    context.setVariable(
        "pageTitle", params.messages().at(MessageKey.TITLE_APPLICATION_CONFIRMATION.getKeyName()));

    context.setVariable("programTitle", params.programTitle().orElse(""));
    context.setVariable("programShortDescription", params.programShortDescription().orElse(""));

    // Info for login modal
    String applyToProgramsUrl = applicantRoutes.index(params.profile(), params.applicantId()).url();
    context.setVariable("upsellBypassUrl", applyToProgramsUrl);
    context.setVariable(
        "upsellLoginUrl",
        controllers.routes.LoginController.applicantLogin(Optional.of(applyToProgramsUrl)).url());

    // In Thymeleaf, there's no easy way to construct a hyperlink inside a localized string
    String linkHref = settingsManifest.getCommonIntakeMoreResourcesLinkHref(params.request()).get();
    String linkText = settingsManifest.getCommonIntakeMoreResourcesLinkText(params.request()).get();
    String linkHtml =
        "<a href=\"" + linkHref + "\" target=\"_blank\" class=\"usa-link\">" + linkText + "</a>";
    context.setVariable("moreResourcesLinkHtml", linkHtml);

    String goBackHref =
        applicantRoutes
            .review(params.profile(), params.applicantId(), params.completedProgramId())
            .url();
    context.setVariable("goBackHref", goBackHref);

    // Create account or login alert
    context.setVariable("createAccountLink", controllers.routes.LoginController.register().url());

    // Program cards section
    boolean showProgramsCardsSection =
        params.eligiblePrograms().isPresent() && params.eligiblePrograms().get().size() > 0;
    Optional<ProgramSectionParams> cardsSection = Optional.empty();
    if (showProgramsCardsSection) {
      cardsSection =
          Optional.of(
              programCardsSectionParamsFactory.getSection(
                  params.request(),
                  params.messages(),
                  Optional.empty(),
                  MessageKey.BUTTON_VIEW_AND_APPLY,
                  params.eligiblePrograms().get(),
                  /* preferredLocale= */ params.messages().lang().toLocale(),
                  Optional.of(params.profile()),
                  Optional.of(params.applicantId()),
                  params.applicantPersonalInfo(),
                  ProgramCardsSectionParamsFactory.SectionType.STANDARD));
    }
    context.setVariable("showProgramsCardsSection", showProgramsCardsSection);
    context.setVariable("cardsSection", cardsSection);

    // Toasts
    if (params.eligiblePrograms().isPresent()) {
      context.setVariable("bannerMessage", params.bannerMessage());
    }

    return templateEngine.process("applicant/ApplicantCommonIntakeUpsellTemplate", context);
  }

  /* Provides syntactic sugar for displaying user-facing program information in HTML. */
  public static final class DisplayProgram {
    private final String title;
    private final String shortDescription;

    public DisplayProgram(String title, String shortDescription) {
      this.title = title;
      this.shortDescription = shortDescription;
    }

    public String getTitle() {
      return title;
    }

    public String getShortDescription() {
      return shortDescription;
    }
  }
}
