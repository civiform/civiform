package views.applicant;

import com.google.common.collect.ImmutableList;
import com.google.inject.Inject;
import controllers.AssetsFinder;
import controllers.LanguageUtils;
import controllers.applicant.ApplicantRoutes;
import java.util.Locale;
import java.util.Optional;
import modules.ThymeleafModule;
import org.thymeleaf.TemplateEngine;
import services.DeploymentType;
import services.settings.SettingsManifest;
import views.NorthStarBaseView;
import views.components.TextFormatter;

public class NorthStarApplicantCommonIntakeUpsellView extends NorthStarBaseView {

  @Inject
  NorthStarApplicantCommonIntakeUpsellView(
      TemplateEngine templateEngine,
      ThymeleafModule.PlayThymeleafContextFactory playThymeleafContextFactory,
      AssetsFinder assetsFinder,
      ApplicantRoutes applicantRoutes,
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
  }

  public String render(UpsellParams params) {
    ThymeleafModule.PlayThymeleafContext context =
        createThymeleafContext(
            params.request(),
            Optional.of(params.applicantId()),
            Optional.of(params.profile()),
            params.applicantPersonalInfo(),
            params.messages());

    context.setVariable("programTitle", params.programTitle().orElse(""));
    context.setVariable("programDescription", params.programDescription().orElse(""));

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
    linkHtml = TextFormatter.sanitizeHtml(linkHtml);
    context.setVariable("moreResourcesLinkHtml", linkHtml);

    String goBackHref =
        applicantRoutes
            .review(params.profile(), params.applicantId(), params.completedProgramId())
            .url();
    context.setVariable("goBackHref", goBackHref);

    // Create account or login alert
    context.setVariable("createAccountLink", controllers.routes.LoginController.register().url());

    if (params.eligiblePrograms().isPresent()) {
      Locale userLocale = params.messages().lang().toLocale();

      ImmutableList<DisplayProgram> displayPrograms =
          params.eligiblePrograms().orElse(ImmutableList.of()).stream()
              .map(
                  applicantProgramData ->
                      new DisplayProgram(
                          applicantProgramData.program().localizedName().getOrDefault(userLocale),
                          applicantProgramData
                              .program()
                              .localizedDescription()
                              .getOrDefault(userLocale)))
              .collect(ImmutableList.toImmutableList());

      context.setVariable("eligiblePrograms", displayPrograms);

      context.setVariable("bannerMessage", params.bannerMessage());
    }
    return templateEngine.process("applicant/ApplicantCommonIntakeUpsellTemplate", context);
  }

  /* Provides syntactic sugar for displaying user-facing program information in HTML. */
  public static final class DisplayProgram {
    private final String title;
    private final String description;

    public DisplayProgram(String title, String description) {
      this.title = title;
      this.description = description;
    }

    public String getTitle() {
      return title;
    }

    public String getDescription() {
      return description;
    }
  }
}
