package views.errors;

import com.google.inject.Inject;
import controllers.LanguageUtils;
import java.util.Optional;
import modules.ThymeleafModule;
import org.thymeleaf.TemplateEngine;
import play.i18n.Messages;
import play.mvc.Http;
import services.BundledAssetsFinder;
import services.DeploymentType;
import services.MessageKey;
import services.applicant.ApplicantPersonalInfo;
import services.settings.SettingsManifest;
import views.NorthStarBaseView;
import views.components.TextFormatter;

/**
 * Renders a page to handle internal server errors that will be shown to users instead of the
 * unthemed default Play page.
 */
public final class InternalServerError extends NorthStarBaseView {

  @Inject
  public InternalServerError(
      TemplateEngine templateEngine,
      ThymeleafModule.PlayThymeleafContextFactory playThymeleafContextFactory,
      BundledAssetsFinder bundledAssetsFinder,
      controllers.applicant.ApplicantRoutes applicantRoutes,
      SettingsManifest settingsManifest,
      LanguageUtils languageUtils,
      DeploymentType deploymentType) {
    super(
        templateEngine,
        playThymeleafContextFactory,
        bundledAssetsFinder,
        applicantRoutes,
        settingsManifest,
        languageUtils,
        deploymentType);
  }

  public String render(Http.Request request, Messages messages, String exceptionId) {
    ThymeleafModule.PlayThymeleafContext context =
        createThymeleafContext(
            request,
            Optional.empty(),
            Optional.empty(),
            ApplicantPersonalInfo.ofGuestUser(),
            messages);

    String additionalInfo = buildAdditionalInfo(request, messages, exceptionId);

    context.setVariable("additionalInfo", additionalInfo);
    context.setVariable("homeUrl", controllers.routes.HomeController.index().url());
    context.setVariable("statusCode", "500");

    return templateEngine.process("errors/InternalServerErrorTemplate.html", context);
  }

  private String buildAdditionalInfo(Http.Request request, Messages messages, String exceptionId) {
    // Support email address is required and should never be blank
    String emailAddress = settingsManifest.getSupportEmailAddress(request).orElse("");
    String emailLinkHref =
        String.format("mailto:%s?body=[CiviForm Error ID: %s]", emailAddress, exceptionId);
    String emailLinkHtml =
        String.format("<a href=\"%s\" class=\"usa-link\">%s</a>", emailLinkHref, emailAddress);
    String descriptionText =
        messages.at(MessageKey.ERROR_INTERNAL_SERVER_DESCRIPTION.getKeyName(), exceptionId);
    // Since the exceptionId comes through in a query param, we want to sanitize it before allowing
    // the raw html to be rendered to remove any script tags or other potentially harmful injections
    String sanitizedDescription =
        TextFormatter.sanitizeHtml(String.format(descriptionText, emailLinkHtml));
    return sanitizedDescription;
  }
}
